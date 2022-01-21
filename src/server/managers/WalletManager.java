package server.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import server.domain.*;
import shared.utils.AppLogger;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

// classe che gestisce i wallet degli utenti
public class WalletManager {
    private final UserManager userMan;
    private final PostsManager postsManager;
    private static final String USER_WALLET_FILENAME = "userWallets.json";
    private ArrayList<UserWallet> userWallets;
    private int percEarnForAuthor;

    // costruttore in cui viene passata anche la percentuale su come calcolare i guadagni.
    public WalletManager(UserManager userMan, PostsManager postsManager,int percEarnForAuthor) {
        this.userMan = userMan;
        this.postsManager = postsManager;
        this.percEarnForAuthor = percEarnForAuthor;
    }

    // inizializzazione in cui si crea la lista dei wallet, o viene ripristinata da json.
    public void initialize() throws IOException {
        File f = new File(USER_WALLET_FILENAME);
        userWallets = new ArrayList<>();
        if(f.exists() && !f.isDirectory()) {
            var inputStream = new FileInputStream(USER_WALLET_FILENAME);
            var reader = new JsonReader(new InputStreamReader(inputStream));
            Type collectionType = new TypeToken<ArrayList<UserWallet>>(){}.getType();
            userWallets = new Gson().fromJson(reader,collectionType) ;
        }
    }

    // calcolo dei wallets.
    public synchronized void calculateWallets() {
        HashMap<Integer, Double> guadagnoPerUtente = new HashMap<Integer,Double>(); // tiene il guadagno totale per ogni utente

        for(Post p: postsManager.getAllPosts()){
            HashSet<Integer> curatori = new HashSet<Integer>();

            // calcolo guadagno proveniente dai rates
            var earnFromLikes = calculatEarnPostForLikes(p, curatori);

            // calcolo guadagno proveniente dai commenti
            var earnFromComments = calculatEarnPostForComments(p, curatori);

            var etaOfPost = p.incEta();  // eta del post
            var totalEarnOfPost = (earnFromLikes+earnFromComments)/etaOfPost;  //totale guadagno del post
            var earnForAuthor =totalEarnOfPost*percEarnForAuthor/100;   // ripartizione su autore
            var earnForCurators =totalEarnOfPost*(100-percEarnForAuthor)/100;  // ripartizione su curatori

            updateEarn(p.getIdPublisher(),earnForAuthor, guadagnoPerUtente);
            for(var curator: curatori){
                updateEarn(curator,earnForCurators/curatori.size(),guadagnoPerUtente);
            }
        }
        updateUsersWallets(guadagnoPerUtente); // aggiornamento dei wallets

        SaveUsersWallet();  //persistenza wallet
        postsManager.savePostList();   //persistenza post che sono variati per eta
        postsManager.savePostRateList();  //persistenza dei voti
        postsManager.saveCommentList();  //persistenza dei commenti
    }

    // ritorna il wallet di un utente.
    public synchronized UserWallet getWallet(User u){
        var userWallet = userWallets.stream().filter(x->x.getIdUser()==u.getId()).findAny().orElse(null);
        if(userWallet==null){
            userWallet = new UserWallet(u.getId());
            userWallets.add(userWallet);
        }
        return userWallet;
    }

    // calcolo guadagno derivato dai voti per il post p
    // note: aggiorna il set dei curatori
    private double calculatEarnPostForLikes(Post p, HashSet<Integer> curatori){
        var  newRates =  postsManager.getNewLikes(p);
        int sumOfVotes = newRates.stream().mapToInt(x->x.getVote()).sum();
        if (sumOfVotes<0)
            sumOfVotes=0;
        double earnFromLikes = Math.log(sumOfVotes+1);

        postsManager.markCountedRates(newRates);
        curatori.addAll(newRates.stream().mapToInt(x->x.getIdVotant()).boxed().toList());
        return earnFromLikes;
    }

    // calclo guadagno dai commenti per il post p
    // note: aggiorna il set dei curatori
    private double calculatEarnPostForComments(Post p, HashSet<Integer> curatori) {
        var newComments = postsManager.getNewComments(p);
        HashMap<Integer, ArrayList<Comment>> commentsForCurator = new HashMap<>();
        for(var c:newComments){
            var comAuthor = commentsForCurator.get(c.getIdAuthor());
            if(comAuthor==null) {
                comAuthor = new ArrayList<Comment>();
                commentsForCurator.put(c.getIdAuthor(), comAuthor);
            }
            comAuthor.add(c);
        }

        double earnFromComments = 1;
        for(var el:commentsForCurator.keySet()){
            int cp = commentsForCurator.get(el).size();
            var q = 2/(1+Math.exp(-cp+1));
            earnFromComments+=q;
        }
        earnFromComments = Math.log(earnFromComments);

        postsManager.markCountedComments(newComments);
        curatori.addAll(newComments.stream().mapToInt(x->x.getIdAuthor()).boxed().toList());
        return earnFromComments;
    }

    // aggiorna il guadagno per un utente.
    private void updateEarn(int idUser, double earn, HashMap<Integer, Double> guadagnoPerUtente) {
        var oldValue = guadagnoPerUtente.get(idUser);
        if (oldValue == null)
            oldValue = 0.0;
        else
            guadagnoPerUtente.remove(idUser);
        guadagnoPerUtente.put(idUser,oldValue+earn);
    }

    // aggiorna tutti i wallet utilizzando una mappa che per ogni utente contiene il
    // guadagno totale calcolato in base a voti e commenti dei post.
    private void updateUsersWallets(HashMap<Integer, Double> guadagnoPerUtente) {
        var timestamp = new Date();
        for(var idUserEarn:guadagnoPerUtente.keySet()){
            double value = guadagnoPerUtente.get(idUserEarn);
            if(value!=0){
                updateUserWallet(idUserEarn,value,timestamp);
            }
        }
    }

    // aggiornamento di un wallet, aggiungendo una transazione.
    private void updateUserWallet(int idUserEarn, double value, Date timestamp) {
        var userWallet = userWallets.stream().filter(x->x.getIdUser()==idUserEarn).findAny().orElse(null);
        var transaction= new Transaction(value,timestamp);
        if(userWallet!=null){
            userWallet.addTransaction(transaction);
        }
        else {
            userWallet = new UserWallet(idUserEarn);
            userWallet.addTransaction(transaction);
            userWallets.add(userWallet);
        }
    }

    // salvataggio di tutti i wallet su file json
    private void SaveUsersWallet() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            FileOutputStream fos = new FileOutputStream(USER_WALLET_FILENAME);
            OutputStreamWriter ow = new OutputStreamWriter(fos);
            String usersJson = gson.toJson(userWallets);
            ow.write(usersJson);
            ow.flush();
        } catch (IOException e) {
            AppLogger.log("Errore durante il salvataggio: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
