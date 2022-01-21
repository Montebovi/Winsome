package server.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import server.domain.*;
import shared.utils.AppLogger;
import shared.utils.ServiceResultCodes;
import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// Classe per la gestione dei Post con i commenti ed i rate.
// Lo stato che consiste di una lista e due mappe viene persisito su differenti file json.
public class PostsManager {
    private static final int MAX_LEN_POST = 500;
    private static final int MAX_LEN_TITLE = 20;

    private final FollowersManager followerMan;

    private static final String PostListFileName = "PostList.json";
    private static final String CommentListFileName = "CommentList.json";
    private static final String PostRateListFileName = "PostRateList.json";

    private List<Post> postList;
    private HashMap<Integer, ArrayList<Comment>> commentList;
    private HashMap<Integer, ArrayList<PostRate>> postRateList;

    // costruttore in cui viene passato il FollowersManager
    public PostsManager(FollowersManager followerMan) {
        this.followerMan = followerMan;
    }

    // L' inizializzazione consiste nella creazione o ripristino da json delle strutture interne.
    public void initialize() throws FileNotFoundException {

        File f = new File(PostListFileName);
        postList = new ArrayList<>();
        if (f.exists() && !f.isDirectory()) {
            FileInputStream inputStream = new FileInputStream(PostListFileName);
            JsonReader reader = new JsonReader(new InputStreamReader(inputStream));
            Type collectionType = new TypeToken<ArrayList<Post>>() {
            }.getType();
            postList = new Gson().fromJson(reader, collectionType);
        }

        File c = new File(CommentListFileName);
        commentList = new HashMap<>();
        if (c.exists() && !c.isDirectory()) {
            FileInputStream inputStream = new FileInputStream(CommentListFileName);
            JsonReader reader = new JsonReader(new InputStreamReader(inputStream));
            Type collectionType = new TypeToken<HashMap<Integer, ArrayList<Comment>>>() {
            }.getType();
            commentList = new Gson().fromJson(reader, collectionType);
        }

        File r = new File(PostRateListFileName);
        postRateList = new HashMap<>();
        if (r.exists() && !r.isDirectory()) {
            FileInputStream inputStream = new FileInputStream(PostRateListFileName);
            JsonReader reader = new JsonReader(new InputStreamReader(inputStream));
            Type collectionType = new TypeToken<HashMap<Integer, ArrayList<PostRate>>>() {
            }.getType();
            postRateList = new Gson().fromJson(reader, collectionType);
        }
    }

    // creazine di un Post.
    //      u: utente che ha creato il post
    //      titolo: titolo del post
    //      contenuto: contenuto del post
    public synchronized Result<Integer> createPost(User u, String titolo, String contenuto) {
        if (contenuto.length() > MAX_LEN_POST)
            return new Result<>(0,ServiceResultCodes.ERR_CONTENT_TOO_LONG);
        if (titolo.length() > MAX_LEN_TITLE)
            return new Result<>(0,ServiceResultCodes.ERR_TITLE_TOO_LONG);
        var nuovoId = getNewIdPost();
        Post post = new Post(nuovoId, titolo, contenuto, u.getId(), u.getId());
        post.setIdOriginalPost(post.getIdPost());
        postList.add(post);
        savePostList();
        return new Result<>(nuovoId, ServiceResultCodes.SUCCESS);
    }

    // view del blog
    // Ritorna elenco dei post di cui l'utente è publisher (autore o autore di rewin).
    public synchronized ArrayList<Post> viewBlog(User user) {
        ArrayList<Post> blogOfUser = new ArrayList<Post>();
        for (Post p : postList) {
            if (p.getIdPublisher() == user.getId())
                blogOfUser.add(p);
        }
        return blogOfUser;
    }

    // view feed
    // ritorno elenco dei post visibili a user
    public synchronized ArrayList<Post> showFeed(User u) {
        ArrayList<Post> feedOfUser = new ArrayList<Post>();
        var following = followerMan.getListFollowingId(u);
        for (Post p : postList) {
            if (following.contains(p.getIdPublisher()))
                feedOfUser.add(0, p);
        }
        return feedOfUser;
    }

    // show post
    // ritorna il dettaglio del post identificato da idPost.
    // note: si verifica che l'utente sia autorizzato alla visione del post (o è publisher o il post è sul proprio feed).
    public synchronized Result<PostData> showPost(User user, int idPost) {
        Post p = getPostById(idPost);
        if (p == null)
            return new Result(null, ServiceResultCodes.ERR_POST_NOT_FOUND);

        if (user.getId() != p.getIdPublisher()) {
            if (isInUserFeed(user, idPost) != ServiceResultCodes.SUCCESS)
                return new Result(null, ServiceResultCodes.ERR_POST_NOT_ACCESSIBLE);
        }
        return new Result(
                new PostData(p, calcNumPos(idPost), calcNumNeg(idPost), commentList.get(idPost)),
                ServiceResultCodes.SUCCESS);
    }

    // Elimina un post
    // note: l'operazione è concessa solo se user è il publisher.
    public synchronized int deletePost(User u, int idPost) {
        Post p = getPostById(idPost);
        if (p == null)
            return ServiceResultCodes.ERR_POST_NOT_FOUND;

        // solo il publisher può eliminare
        if (u.getId() != p.getIdPublisher())
            return ServiceResultCodes.ERR_ACTION_DENIED;

        //note: elimina tutti i post che hanno come origine idPost (eventuali post rewin)
        for (Post aPost : postList) {
            if (aPost.getIdOriginalPost() == idPost) {
                deleteComment(aPost.getIdPost());
                deleteRate(aPost.getIdPost());
            }
        }
        postList.removeIf(x -> x.getIdOriginalPost() == idPost);
        saveCommentList();
        savePostRateList();
        savePostList();
        return ServiceResultCodes.SUCCESS;
    }

    // aggiunge un commento sul post
    // il commento può essere aggiunto solo se il post fa parte del proprio feed.
    public synchronized int addComment(User commenter, int idPost, String commento) {
        var result = isInUserFeed(commenter, idPost);
        if (result != ServiceResultCodes.SUCCESS)
            return result;

        var commentiDelPost = commentList.get(idPost);
        if (commentiDelPost == null) {
            var comments = new ArrayList<Comment>();
            var comment = new Comment(idPost + "_" + (comments.size() + 1), idPost, commenter.getId(), commento);
            comments.add(comment);
            commentList.put(idPost, comments);
        } else {
            var idCommento = idPost + "_" + (commentiDelPost.size() + 1);
            var comment = new Comment(idCommento, idPost, commenter.getId(), commento);
            commentiDelPost.add(comment);
        }
        saveCommentList();
        return ServiceResultCodes.SUCCESS;
    }

    // aggiunge un voto positivo o negativo al post
    // il voto può essere assegnato solo se il post fa parte del proprio feed.
    //note: nel caso di rewin il voto viene dato a chi ha eseguito il rewin e non all'autore originale
    public synchronized int addVote(User u, int idPost, int vote) {
        if (vote != -1 && vote != 1)
            return ServiceResultCodes.ERR_VOTE_NOT_CORRECT;

        var result = isInUserFeed(u, idPost);
        if (result != ServiceResultCodes.SUCCESS)
            return result;
        var ratesOfPost = postRateList.get(idPost);

        if (ratesOfPost == null) {
            ratesOfPost = new ArrayList<PostRate>();
            postRateList.put(idPost, ratesOfPost);
        }
        if (ratesOfPost.stream().anyMatch(r -> r.getIdVotant() == u.getId()))
            return ServiceResultCodes.ERR_ACTION_DENIED;

        //note: nel caso di rewin il voto viene dato a chi ha eseguito il rewin e non all'autore originale
        PostRate rate = new PostRate(idPost, u.getId(), vote);

        ratesOfPost.add(rate);
        savePostRateList();
        return ServiceResultCodes.SUCCESS;
    }

    // rewin di un post
    // l'azione è consentita solo se idpost fa parte del feed dell'utente.
    public synchronized int rewinPost(User u, int idPost) {
        var result = isInUserFeed(u, idPost);
        if (result != ServiceResultCodes.SUCCESS)
            return result;
        Post p = getPostById(idPost);
        Post rewinP = new Post(getNewIdPost(), p.getTitolo(), p.getContenuto(), p.getIdAuthor(), u.getId());
        rewinP.setIdOriginalPost(p.getIdOriginalPost());
        postList.add(rewinP);
        savePostList();
        return ServiceResultCodes.SUCCESS;
    }

    // ritorna tutti post
    synchronized List<Post> getAllPosts() {
        return this.postList;
    }

    // salvataggio su file dei commenti
    synchronized void saveCommentList() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            var fos = new FileOutputStream(CommentListFileName);
            OutputStreamWriter ow = new OutputStreamWriter(fos);
            String usersJson = gson.toJson(commentList);
            ow.write(usersJson);
            ow.flush();
        } catch (IOException e) {
            AppLogger.log("Errore durante il salvataggio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // salvataggio su file dei rate
    synchronized void savePostRateList() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            FileOutputStream fos = new FileOutputStream(PostRateListFileName);
            OutputStreamWriter ow = new OutputStreamWriter(fos);
            String usersJson = gson.toJson(postRateList);
            ow.write(usersJson);
            ow.flush();
        } catch (IOException e) {
            AppLogger.log("Errore durante il salvataggio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // salvataggio su file dei post
    synchronized void savePostList() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            FileOutputStream fos = new FileOutputStream(PostListFileName);
            OutputStreamWriter ow = new OutputStreamWriter(fos);
            String usersJson = gson.toJson(postList);
            ow.write(usersJson);
            ow.flush();
        } catch (IOException e) {
            AppLogger.log("Errore durante il salvataggio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // marca i rate conteggiati per il calcolo wallet
    synchronized void markCountedRates(List<PostRate> newRates) {
        for (var r : newRates)
            r.isCounted = true;
    }

    // marca i commenti conteggiati per il calcolo wallet
    synchronized void markCountedComments(List<Comment> newComments) {
        for(var r:newComments)
            r.isCounted=true;
    }

    // ritorna i nuovi rate (quelli non marcati)
    synchronized List<PostRate> getNewLikes(Post p) {
        var rates = postRateList.get(p.getIdPost());
        if (rates != null)
            return rates.stream().filter(x -> !x.isCounted).toList();
        return new ArrayList<PostRate>();
    }

    // ritorna i nuovi commenti (quelli non marcati)
    synchronized List<Comment> getNewComments(Post p) {
        var comments = commentList.get(p.getIdPost());
        if (comments != null)
            return comments.stream().filter(x -> !x.isCounted).toList();
        return new ArrayList<Comment>();
    }

    // ritorna il nuovo id da assegnare ad un post creato
    synchronized private int getNewIdPost() {
        var maxId = postList.stream().mapToInt(x -> x.getIdPost()).max().orElse(0);
        return maxId + 1;
    }

    // ritorna il post con id=idPost
    private Post getPostById(int idPost) {
        return postList.stream().filter(post -> post.getIdPost() == idPost).findAny().orElse(null);
    }

    // ritorna il numero dei voti positivi del post
    private int calcNumPos(int idPost) {
        var rateList = postRateList.get(idPost);
        if (rateList == null)
            return 0;
        return (int) rateList.stream().filter(x -> x.getVote() > 0).count();
    }

    // ritorna il numero dei voti negativi del post
    private int calcNumNeg(int idPost) {
        var rateList = postRateList.get(idPost);
        if (rateList == null)
            return 0;
        return (int) rateList.stream().filter(x -> x.getVote() < 0).count();
    }

    // funzione interna usata per determinare se il post (idPost) appartiene al feed dell'utente u.
    private int isInUserFeed(User u, int idPost) {
        Post p = getPostById(idPost);
        if (p == null)
            return ServiceResultCodes.ERR_POST_NOT_FOUND;
        if (p.getIdPublisher() == u.getId())
            return ServiceResultCodes.ERR_ACTION_DENIED;

        var listOfFollowing = followerMan.getListFollowingId(u);

        if (!listOfFollowing.contains(p.getIdPublisher()))
            return ServiceResultCodes.ERR_ACTION_DENIED;

        return ServiceResultCodes.SUCCESS;
    }

    // elimina i voti di un post
    private void deleteRate(int idPost) {
        var rates = postRateList.get(idPost);
        if (rates != null)
            rates.removeIf(x -> x.getIdPost() == idPost);
    }

    // elimina i commenti di un post
    private void deleteComment(int idPost) {
        var comments = commentList.get(idPost);
        if (comments != null)
            comments.removeIf(x -> x.getIdPost() == idPost);
    }
}
