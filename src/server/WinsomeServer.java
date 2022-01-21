package server;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;

import shared.*;
import shared.interfaces.*;
import shared.utils.*;

import server.domain.*;
import server.managers.*;
import server.services.*;

// rappresenta il server
// esso istanzia gli oggetti menager per gestire la business logic, avvia il thread per il calcolo/notifica wallet,
// crea la coda dei comandi ed il thredapool per eseguire i comandi.
public class WinsomeServer {
    private final WinsomeServerCfg config;  // configurazione del server

    // managers ---------------------------------
    private final UserManager userMan;
    private final FollowersManager follerwManager;
    private final PostsManager postsManager;
    private final WalletManager walletManager;
    //-------------------------------------------

    private final ThreadPoolExecutor poolOfThreads;  // thread pool impiegato per eseguire i task dei comandi
    private final LinkedBlockingQueue<Runnable> commandsQueue;  // coda per i task dei comandi

    private final WalletService walletService;    // servizio per aggiornare e notifica wallet
    private IFollowersNotifyService followersNotifyService;  // servizio per la notifica dei follower.

    public WinsomeServer() throws IOException {
        config = new WinsomeServerCfg();
        config.LoadConfig("server.cfg");

        // creazione dei managers ----------------------------
        userMan = new UserManager();
        userMan.initialize();

        follerwManager = new FollowersManager(userMan);
        follerwManager.initialize();

        postsManager = new PostsManager(/*userMan,*/ follerwManager);
        postsManager.initialize();

        walletManager = new WalletManager(userMan,postsManager, config.percEarnForAuthor);
        walletManager.initialize();
        //---------------------------------------------------

        walletService = new WalletService(config.multicastAddress,config.multicatsPort,
                walletManager,config.WalletServicePeriodInSec);

        // creazione thread pool
        poolOfThreads = new ThreadPoolExecutor( config.corePoolSize, config.maximumPoolSize,
                config.keepAliveTime, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(config.sizeTaskQueue),
                new ThreadPoolExecutor.CallerRunsPolicy());

        // coda dei comandi
        commandsQueue = new LinkedBlockingQueue<Runnable>();
    }

    public void execute() throws Exception {

        // start servizio basato su RMI per la register user
        startRegisterService();

        // servizio RMI callback per notificare aggiunta/rimozione followers
        followersNotifyService = FollowersNotifyServiceImpl.StartService(config.followerSvcName,
                config.followerSvcPort, follerwManager);

        walletService.start();  // avvio thread per il calcolo/notifica wallet periodico
        startCommandConsumer(); // avvio del consumatore
        startCommandReceiver(); // si pone in ricezione comandi

    }

    // avvio consumer
    private void startCommandConsumer() {
        Thread consumer = new Thread(new CommandsConsumer(poolOfThreads, commandsQueue));
        consumer.start();
    }

    // ciclo per ricezione comandi (uso del selettore)
    private void startCommandReceiver() throws Exception {
        final int bufSize = 1024*8;

        // apertura socket
        Selector selector = Selector.open();
        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.bind(new InetSocketAddress(config.serviceHost, config.servicePort));
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);

        AppLogger.log("Server pronto sulla porta: " + config.servicePort);

        while (true) {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();

            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                // Controllo se sul canale associato alla chiave
                // c'è la possibilità di accettare una nuova connessione.
                if (key.isAcceptable()) {
                    // Accetto la connessione e registro il canale ottenuto
                    // sul selettore.
                    SocketChannel client = serverSocket.accept();
                    AppLogger.log("Nuova connessione ricevuta");
                    client.configureBlocking(false);
                    var key2 = client.register(selector, SelectionKey.OP_READ);
                    key2.attach(new UserSession());  // associo una sessione con utente anonimo
                }
                // Se il canale associato alla chiave è leggibile,
                // allora procedo con l'invio del messaggio di risposta.
                if (key.isReadable())
                    manageCommand(key);  // gestione del comando ricevuto
                iter.remove();
            }
        }
    }

    private void manageCommand(SelectionKey key) throws Exception {
        final int bufSize = 1024*8;
        ByteBuffer buffer = ByteBuffer.allocate(bufSize);
        SocketChannel client = (SocketChannel) key.channel();
        buffer.clear();
        try {
            client.read(buffer);  // lettura del comando
        }
        catch(SocketException e){  // accade quando il client  termina in modo anomalo
            client.close();
            return;
        }
        buffer.flip();
        if(!buffer.hasRemaining()) {
            client.close();
            return;
        }
        int receivedLength = buffer.getInt();
        byte[] receivedBytes = new byte[receivedLength];
        buffer.get(receivedBytes);
        String receivedStr = new String(receivedBytes);

        Command cmd = new Gson().fromJson(receivedStr,Command.class);  // il comando viene creato dal json ricevuto

        Response response = null;
        var userSession = (UserSession)key.attachment();   // acquisizione della sessione

        // gestione della LOGIN
        if (cmd.code == Command.CMD_LOGIN){
            if (userSession.username != null)
                response = new Response(ServiceResultCodes.ERR_ACTION_DENIED,"login già effettuato");
            else if (cmd.parameters.size() != 2)
                response = new Response(ServiceResultCodes.ERR_WRONG_NUM_PARAMS,"numero parametri non corretto.");
            else{
                var res = userMan.login(cmd.parameters.get(0), cmd.parameters.get(1));
                if (res == 0){
                    userSession.username = cmd.parameters.get(0);   // note: la usersession non è più anonima, ma riferita al primo parametro (username)
                }
                response = new Response(res,"login result.");
            }
        }
        // gestione del comando logout
        else if (cmd.code == Command.CMD_LOGOUT){
            if (userSession.username == null)
                response = new Response(ServiceResultCodes.ERR_ACTION_DENIED,"login mai effettuato");
            else if (cmd.parameters.size() != 1)
                response = new Response(ServiceResultCodes.ERR_WRONG_NUM_PARAMS,"numero parametri non corretto.");
            else if (!userSession.username.equals(cmd.parameters.get(0)))
                response = new Response(ServiceResultCodes.ERR_INVALID_USERNAME,"nome utente non corretto.");
            else{
                userSession.clear();  // riporta la sessione a utente anonimo
                var res = userMan.logout(cmd.parameters.get(0));
                response = new Response(res,"logout result.");
            }
        }
        // note: altri comandi non sono eseguibile se non so è effettuata la login
        else if (userSession.username == null) {
            response = new Response( ServiceResultCodes.ERR_ACTION_DENIED,"login mai effettuato");
        }
        else{
            // creazione del task per gestire il comando
            var task = new CommandToExec(userSession,client,cmd,followersNotifyService,
                    userMan,follerwManager,postsManager,walletManager);
            // il task viene messo in coda.
            commandsQueue.add(task);
        }

        // si invia il response al client
        if (response != null) {
            //String replyStr = new String("successo");
            var replyStr =  new Gson().toJson(response);  // response inviato in json
            byte[] replyBytes = replyStr.getBytes();
            buffer.clear();
            buffer.putInt(replyBytes.length);
            buffer.put(replyBytes);
            buffer.flip();
            client.write(buffer);
        }
    }


    // registrazione del RMI per il comando "REGISTER"
    private void startRegisterService() throws RemoteException {
        IRegisterService rService = new RegisterService(userMan);
        var stub = (IRegisterService) UnicastRemoteObject.exportObject(rService,config.registerPort);
        LocateRegistry.createRegistry(config.registerPort);
        Registry r = LocateRegistry.getRegistry(config.registerPort);
        r.rebind(config.regServiceName,stub);
        AppLogger.log("Register Server ready (port="+config.registerPort+")");
    }
}
