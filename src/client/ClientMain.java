package client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import shared.Command;
import shared.Response;
import shared.dto.PostDetailDto;
import shared.dto.PostDto;
import shared.dto.UserDto;
import shared.dto.UserWalletDto;
import shared.interfaces.IRegisterService;
import shared.utils.AppLogger;
import shared.utils.ServiceResultCodes;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.*;

public class ClientMain {
    public static ClientConfig config;
    private static Map<String,Integer> stringToCodeMap = new HashMap<>() ;

    private static FollowersCache followersCache;

    //************************************************************************
    // Metodo main del client
    public static void main(String[] args) throws InterruptedException, IOException {
        initializeCommandsMap();

        System.out.print("Client starting....");

        // caricamento config
        config = new ClientConfig();
        config.LoadConfig("client.cfg");

        // creazione oggetto per memorizzare i follwer
        followersCache = new FollowersCache(config.followerSvcHost, config.followerSvcName,config.followerSvcPort);

        try {
            followersCache.initialize();  // inizializzazione
        }
        catch (RemoteException e)        {
            System.out.println("Server non attivo. Il client viene terminato.");
            System.exit(0);
        } catch (NotBoundException e) {
            System.out.println("NotBoundException. Il client viene terminato.");
            System.exit(0);
        }

        // creazione thread per ricevere le notifiche di aggiornamento wallet
        var walletClientThread = new WalletClientThread(config.multicastAddress,config.multicatsPort);
        walletClientThread.start();

        // apertura del canale socket TCP per inviare i comandi al server
        SocketChannel socketChannel = null;
        try {
            socketChannel = SocketChannel.open(new InetSocketAddress(config.host, config.port));
        }
        catch (java.net.ConnectException e)        {
            System.out.println("Server non attivo. Il client viene terminato.");
            System.exit(0);
        }

        System.out.println("DONE");
        System.out.println("In attesa di comandi");

        var scanner = new Scanner(System.in);

        // ciclo prompt per input comandi ed invio
        while (true) {
            System.out.print("> ");
            String inputStr = scanner.nextLine();

            // gestione di EXIT
            if (inputStr.equalsIgnoreCase("exit")){
                followersCache.unregisterForCallback();
                socketChannel.close();
                break;
            }

            // parse del comando
            var cmd = parseCommand(inputStr);
            if (cmd != null){

                // gestione REGISTER
                if (cmd.code == Command.CMD_REGISTER)
                {
                    if (cmd.parameters.size() < 3){
                        printOut("Numero parametri non sufficienti nella register almeno 3 parametri)");
                    }
                    else {
                        var userName = cmd.parameters.get(0);
                        var password = cmd.parameters.get(1);
                        var tags = cmd.parameters.stream().skip(2).toList();
                        userRegister(userName, password, tags);  // registrazione con RMI
                    }
                }
                // gestione LIST FOLLOWERS (non chiama il server)
                else if (cmd.code == Command.CMD_LIST_FOLLOWERS)
                {
                    var users = followersCache.getFollowersForCurrentUser();
                    printOut("Followers list:",users);
                }
                else {   // gestione dei restanti  comandi che vanno inviati al server
                    var response = sendCommand(socketChannel, cmd);

                    // gestione del response
                    if (response.resultCode != ServiceResultCodes.SUCCESS)
                        printOut("Errore " + response.resultCode + "\t " + response.getResponseMessage());
                    else
                        manageCmdResponse(cmd, response);
                }
            }
        }

        System.out.print("Terminazione in corso...");

        // interruzione del thread per i wallet
        walletClientThread.interrupt();
        walletClientThread.join(5*1000);

        System.out.println("DONE");
        System.exit(0);
    }

    // inizializzazione mappa per supportare il parsing
    private static void initializeCommandsMap() {
        stringToCodeMap.put("register",Command.CMD_REGISTER);
        stringToCodeMap.put("login",Command.CMD_LOGIN);
        stringToCodeMap.put("logout",Command.CMD_LOGOUT);
        stringToCodeMap.put("list-users",Command.CMD_LIST_USERS);
        //-----
        stringToCodeMap.put("list-followers",Command.CMD_LIST_FOLLOWERS);
        stringToCodeMap.put("list-following",Command.CMD_LIST_FOLLOWING);
        stringToCodeMap.put("follow-user",Command.CMD_FOLLOW_USER);
        stringToCodeMap.put("unfollow-user",Command.CMD_UNFOLLOW_USER);
        //-----
        stringToCodeMap.put("create-post",Command.CMD_CREATE_POST);
        stringToCodeMap.put("view-blog",Command.CMD_VIEW_BLOG);
        stringToCodeMap.put("show-feed",Command.CMD_SHOW_FEED);
        stringToCodeMap.put("show-post",Command.CMD_SHOW_POST);
        stringToCodeMap.put("delete-post",Command.CMD_DELETE_POST);
        stringToCodeMap.put("add-comment",Command.CMD_ADD_COMMENT);
        stringToCodeMap.put("rate-post",Command.CMD_RATE_POST);
        stringToCodeMap.put("rewin-post",Command.CMD_REWIN_POST);
        stringToCodeMap.put("get-wallet",Command.CMD_GET_WALLET);
        stringToCodeMap.put("get-walletb",Command.CMD_GET_WALLETB);
    }

    // invio del comando al server e ricezione del response
    private static Response sendCommand(SocketChannel socketChannel, Command cmd) throws IOException {
        final int bufSize = 1024 * 8;

        var json = new Gson().toJson(cmd); // comando convertito in json
        byte[] message = json.getBytes();
        ByteBuffer buffer = ByteBuffer.allocate(bufSize);
        buffer.clear();
        buffer.putInt(message.length);
        buffer.put(message);
        buffer.flip();
        socketChannel.write(buffer);   // invio del comando (in formato json)

        buffer.clear();
        socketChannel.read(buffer);  // lettura risposta
        buffer.flip();
        int replyLength = buffer.getInt();
        byte[] replyBytes = new byte[replyLength];
        buffer.get(replyBytes);

        // deserializzazione del response (json -> oggetto response)
        var response = new Gson().fromJson(new String(replyBytes), Response.class);
        return response;
    }

    // invia i comandi al server e interpreta i risultati in funzione del tipo di comando
    private static void manageCmdResponse(Command cmd, Response response) {
        switch (cmd.code){

            case Command.CMD_LOGIN: {
                printOut("\t" + response.getResponseMessage());
                followersCache.registerForCallback(cmd.parameters.get(0));
                break;
            }

            case Command.CMD_LOGOUT: {
                followersCache.unregisterForCallback();
                printOut("\t" + response.getResponseMessage());
                break;
            }

            case Command.CMD_LIST_USERS: {
                Type collectionType = new TypeToken<ArrayList<UserDto>>() {}.getType();
                ArrayList<UserDto> usersList = new Gson().fromJson(response.payload, collectionType);
                printOut("Users list:",usersList);
                break;
            }
            case Command.CMD_LIST_FOLLOWING: {
                Type collectionType = new TypeToken<ArrayList<UserDto>>() {}.getType();
                ArrayList<UserDto> usersList = new Gson().fromJson(response.payload, collectionType);
                printOut("Users followed list:",usersList);
                break;
            }

            case Command.CMD_CREATE_POST:{
                printOut(String.format("Nuovo post creato (id=%s)",response.payload));
                break;
            }

            case Command.CMD_UNFOLLOW_USER:
            case Command.CMD_FOLLOW_USER:
            case Command.CMD_DELETE_POST:
            case Command.CMD_ADD_COMMENT:
            case Command.CMD_RATE_POST:
            case Command.CMD_REWIN_POST:
            {
                printOut("\t" + response.getResponseMessage());
                break;
            }

            case Command.CMD_VIEW_BLOG:
            {
                Type collectionType = new TypeToken<ArrayList<PostDto>>() {}.getType();
                ArrayList<PostDto> postsList = new Gson().fromJson(response.payload, collectionType);
                printOut("Proprio Blog (elenco post dell'utente corrente):");
                for (var aPost : postsList) {
                    printOut( String.format("\tid: [%d] - Autore: [%s]   Titolo:[%s]", aPost.idPost, aPost.autore, aPost.titolo));
                }
                break;
            }

            case Command.CMD_SHOW_FEED:
            {
                var collectionType = new TypeToken<ArrayList<PostDto>>() {}.getType();
                ArrayList<PostDto> postsList = new Gson().fromJson(response.payload, collectionType);
                printOut("Elenco post del feed dell'utente corrente:");
                for (var aPost : postsList) {
                    printOut(String.format("\tid: [%d] - Autore: [%s]   Titolo:[%s]", aPost.idPost, aPost.autore, aPost.titolo));
                }
                break;
            }

            case Command.CMD_SHOW_POST:
            {
                var collectionType = new TypeToken<PostDetailDto>() {}.getType();
                PostDetailDto post = new Gson().fromJson(response.payload, collectionType);
                printOut("Autore:"+post.autore);
                printOut("Titolo:"+post.titolo);
                printOut("Contenuto:"+post.getContenuto());
                printOut(String.format("Voti: %d positivi, %d negativi",post.numPos,post.numNeg));
                var comments = post.getComments();
                if (comments.isEmpty())
                    printOut("Nessun commento");
                else{
                    printOut("Commenti ("+comments.size()+"):");
                    for(var aComment:comments){
                        printOut(String.format("\t%s:\t\"%s\"",aComment.author,aComment.comment));
                    }
                }
                break;
            }

            case Command.CMD_GET_WALLET:{
                var type = new TypeToken<UserWalletDto>() {}.getType();
                UserWalletDto userWallet = new Gson().fromJson(response.payload, type);
                printOut(String.format("Total wincoins: %.4f",(float)userWallet.getTotalCoins()));
                printOut("Transazioni:");
                SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                for(var t:userWallet.getTransactions())
                {
                    var data = date.format(t.getTimestamp());
                    printOut(String.format("\t timestamp:%s\t | Wincoins: %.4f",data,t.getEarn()));
                }
                break;
            }

            case Command.CMD_GET_WALLETB:{
                var type = new TypeToken<UserWalletDto>() {}.getType();
                UserWalletDto userWallet = new Gson().fromJson(response.payload, type);
                printOut(String.format("Total bitcoins: %.4f",(float)userWallet.getTotalCoins()));
                printOut(String.format("Applied rate (wincoins -> bitcoins): %.8f",(float)userWallet.getAppliedRate()));
                break;
            }

            default:
                printOut("Response non gestito per comando ["+ cmd.code+"] - "+ response.payload);
        }
    }



    // Esegui il parse del comando inserito a console
    // Tramite regex isola le varie parole. Converte la prima parola in codice comando attraverso la mappa
    // costruita all'inizio e mette in lista tutti i parametri.
    // Non effettua controlli di sintassi perchè non ha responsabilità sulla logica.
    // NOTA: La regex è fatta in modo che interpreta come unico parametro le parole delimitate da apici (utile quando
    // si crea un post con titlo e messaggio, o si aggiungono commenti.
    private static Command parseCommand(String inputStr) {
        List<String> matchList = new ArrayList<String>();
        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        Matcher regexMatcher = regex.matcher(inputStr);
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                // Add double-quoted string without the quotes
                matchList.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null) {
                // Add single-quoted string without the quotes
                matchList.add(regexMatcher.group(2));
            } else {
                // Add unquoted word
                matchList.add(regexMatcher.group());
            }
        }

        if (matchList.size() == 0)
            return null;

        var firstWord = matchList.get(0);

        Command cmd = null;

        if (firstWord.toLowerCase().equals("help"))
        {
            showHelp();
            return cmd;
        }
        var code = stringToCode(matchList.get(0));  // modo veloce per verificare se il comando esiste.
        if (code == null) {
            printOut("Comando sconosciuto. Scrivi help per la lista comandi.");
            return cmd;
        }

        cmd =  new Command(code.intValue(), matchList.stream().skip(1).toList());
        return cmd;
    }

    // mostra help dei comandi a video
    private static void showHelp() {
        var descr = new CommandDescriptors();
        System.out.println("*** Help comandi ***");
        for(var element: stringToCodeMap.keySet()){
            var code = stringToCode(element);
            System.out.println("\t"+descr.GetCommandSyntax(code));
            System.out.println("\t"+descr.GetCommandDescription(code));
            System.out.println();
        }

        System.out.println("Digitare EXIT per chiudere il client");
        System.out.println("********************************************");
    }

    // converte la parola in codice comando (nota: è volutamente case insensitive).
    private static Integer stringToCode(String s) {
        s = s.trim();
        s = s.toLowerCase();
        Integer code = stringToCodeMap.get(s);
        return code;
    }

    // registrazione dell'utente (comando REGISTER).
    public static void userRegister(String username, String pwd, List<String> tags){
        IRegisterService serverObject;
        Remote RemoteObject;
        try {
            Registry r = LocateRegistry.getRegistry(config.registerHost, config.registerPort);
            RemoteObject = r.lookup(config.regServiceName);
            serverObject = (IRegisterService) RemoteObject;

            var result = serverObject.register(username,pwd, tags);
            if (result == ServiceResultCodes.ERR_INVALID_USERNAME)
                printOut("Username inserito invalido");
            else if (result == ServiceResultCodes.ERR_PASSWORD_TOOSHORT)
                printOut("Password inserita troppo corta");
            else if(result == ServiceResultCodes.ERR_USERNAME_USED)
                printOut("Username già utilizzato");
            else if(result == ServiceResultCodes.ERR_TOO_MUCH_TAGS)
                printOut("Limite massimo di tag è 5");
            else if(result == ServiceResultCodes.SUCCESS)
                printOut("Registrazione avvenuta con successo");
            else
                printOut("Errore sconosciuto");
        }
        catch (Exception e){
            AppLogger.log("Error in invoking object method " +
                    e.toString() + e.getMessage());
            e.printStackTrace();
        }
    }

    // metodo per scrivere il risultato
    private static void printOut(String msg){
        System.out.println("< "+msg);
    }

    // metodo per scrivere elenco di utenti
    private static void printOut(String title, Collection<UserDto> usersList){
        System.out.println("< "+title);
        for (var u : usersList) {
            var tags = String.join("; ", u.getTags());
            printOut("\tUsername: " + u.username + " \t| " + tags);
        }
    }

}
