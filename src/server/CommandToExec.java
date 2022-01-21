package server;

import com.google.gson.Gson;
import server.domain.*;
import server.managers.FollowersManager;
import server.managers.PostsManager;
import server.managers.UserManager;
import server.managers.WalletManager;
import shared.Command;
import shared.Response;
import shared.dto.*;
import shared.interfaces.IFollowersNotifyService;
import shared.utils.AppLogger;
import shared.utils.CurrencyConverter;
import shared.utils.ServiceResultCodes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;

// task che esegue il comando passato in fase di costruzione.
public class CommandToExec implements Runnable {

    private final UserSession userSession;
    private final SocketChannel client;
    private final Command cmd;
    private final UserManager userman;
    private final FollowersManager followersMngr;
    private final IFollowersNotifyService followersNotifyService;
    private final WalletManager walletManager;
    private PostsManager postsManager;

    /**
     * costruttore
     *   userSession    sessione utente corrente
     *   client         socket channel
     *   cmd            comanda da eseguire
     *   followersNotifyService     servizio per notifica follower
     *   userman                    manager utenti
     *   followersMngr              manager followers
     *   postsManager               manager post
     *   walletManager              manager wallet
     */
    public CommandToExec(UserSession userSession, SocketChannel client, Command cmd,
                         IFollowersNotifyService followersNotifyService,
                         UserManager userman, FollowersManager followersMngr,
                         PostsManager postsManager, WalletManager walletManager) {
        this.userSession = userSession;
        this.client = client;
        this.cmd = cmd;
        this.followersNotifyService = followersNotifyService;
        this.userman = userman;
        this.followersMngr = followersMngr;
        this.postsManager = postsManager;
        this.walletManager = walletManager;
    }

    // metodo di esecuzione
    @Override
    public void run() {
        AppLogger.log("gestione del comando " + cmd.code);
        for (var p : cmd.parameters) {
            AppLogger.log("\t" + p);
        }

        // response di default
        Response response = new Response(ServiceResultCodes.ERR_UNKNOWN, "non implementato");

        // esecuzione del comando in funzione del codice
        if (cmd.code == Command.CMD_LIST_USERS) {
            var users = userman.listUsers(userSession.username);
            var resData = users.stream().map(u -> new UserDto(u.getUsername(), u.getTags())).toList();
            response.resultCode = ServiceResultCodes.SUCCESS;
            response.payload = new Gson().toJson(resData);
        } else if (cmd.code == Command.CMD_FOLLOW_USER) execCmdFollowUser(cmd, response);
        else if (cmd.code == Command.CMD_UNFOLLOW_USER) execCmdUnfollowUser(cmd, response);
        else if (cmd.code == Command.CMD_LIST_FOLLOWING) execCmdListFollowing(cmd, response);
        else if (cmd.code == Command.CMD_VIEW_BLOG) execCmdViewBlog(cmd, response);
        else if (cmd.code == Command.CMD_SHOW_FEED) execCmdShowFeed(cmd, response);
        else if (cmd.code == Command.CMD_CREATE_POST) execCmdCreatePost(cmd, response);
        else if (cmd.code == Command.CMD_SHOW_POST) execCmdShowPost(cmd, response);
        else if (cmd.code == Command.CMD_DELETE_POST) execCmdDeletePost(cmd, response);
        else if (cmd.code == Command.CMD_ADD_COMMENT) execCmdAddComment(cmd, response);
        else if (cmd.code == Command.CMD_RATE_POST) execCmdRatePost(cmd, response);
        else if (cmd.code == Command.CMD_REWIN_POST) execCmdRewinPost(cmd, response);
        else if (cmd.code == Command.CMD_GET_WALLET) execCmdGetWallet(cmd, response, false);
        else if (cmd.code == Command.CMD_GET_WALLETB) execCmdGetWallet(cmd, response, true);

        // invio del response
        final int bufSize = 2048;
        ByteBuffer buffer = ByteBuffer.allocate(bufSize);
        var replyStr = new Gson().toJson(response);
        byte[] replyBytes = replyStr.getBytes();
        buffer.clear();
        buffer.putInt(replyBytes.length);
        buffer.put(replyBytes);
        buffer.flip();
        try {
            client.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // esecuzione del comando per list following
    private void execCmdListFollowing(Command cmd, Response response) {
        response.payload = null;
        if (cmd.parameters.size() != 0)
            response.resultCode = ServiceResultCodes.ERR_WRONG_NUM_PARAMS;
        else {
            var currentUser = userman.getUser(userSession.username);
            var followedUsers = followersMngr.getListFollowing(currentUser);
            var resData = followedUsers.stream().map(u -> new UserDto(u.getUsername(), u.getTags())).toList();
            response.resultCode = ServiceResultCodes.SUCCESS;
            response.payload = new Gson().toJson(resData);
        }
    }

    // esecuzione del comando per add follower
    private void execCmdFollowUser(Command cmd, Response response) {
        response.payload = null;
        if (cmd.parameters.size() != 1)
            response.resultCode = ServiceResultCodes.ERR_WRONG_NUM_PARAMS;
        else {
            var usernameToFollow = cmd.parameters.get(0);
            // l'utente corrente è il follower
            var followerUser = userman.getUser(userSession.username);
            if (followerUser == null)
            {
                response.resultCode = ServiceResultCodes.ERR_USERNAME_NOT_EXISTING;
                return;
            }
            response.resultCode = followersMngr.followUser(followerUser, usernameToFollow);
            if (response.resultCode == ServiceResultCodes.SUCCESS) {
                try {
                    var followerDto = new UserDto(followerUser.getUsername(), followerUser.getTags());
                    followersNotifyService.notifyFollowerChanged(usernameToFollow,followerDto, true);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //comando per rimozione follower
    private void execCmdUnfollowUser(Command cmd, Response response) {
        response.payload = null;
        if (cmd.parameters.size() != 1)
            response.resultCode = ServiceResultCodes.ERR_WRONG_NUM_PARAMS;
        else {
            var userToUnfollow = cmd.parameters.get(0);
            var currentUser = userman.getUser(userSession.username);
            response.resultCode = followersMngr.unfollowUser(currentUser, userToUnfollow);

            try {
                var follower =  new UserDto(currentUser.getUsername(),currentUser.getTags());
                followersNotifyService.notifyFollowerChanged(userToUnfollow, follower,false);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    //comando per vedere il proprio blog
    private void execCmdViewBlog(Command cmd, Response response) {
        response.payload = null;
        if (cmd.parameters.size() != 0)
            response.resultCode = ServiceResultCodes.ERR_WRONG_NUM_PARAMS;
        else {
            var currentUser = userman.getUser(userSession.username);
            var blog = postsManager.viewBlog(currentUser);
            var resData = blog.stream()
                    .map(p -> new PostDto(p.getIdPost(), p.getTitolo(),
                            userman.getUserById(p.getIdAuthor()).getUsername())).toList();
            response.payload = new Gson().toJson(resData);
            response.resultCode = ServiceResultCodes.SUCCESS;
        }
    }

    //comando per vedere il proprio feed
    private void execCmdShowFeed(Command cmd, Response response) {
        response.payload = null;
        if (cmd.parameters.size() != 0)
            response.resultCode = ServiceResultCodes.ERR_WRONG_NUM_PARAMS;
        else {
            var currentUser = userman.getUser(userSession.username);
            var blog = postsManager.showFeed(currentUser);
            var resData = blog.stream()
                    .map(p -> new PostDto(p.getIdPost(), p.getTitolo(),
                            userman.getUserById(p.getIdAuthor()).getUsername())).toList();
            response.payload = new Gson().toJson(resData);
            response.resultCode = ServiceResultCodes.SUCCESS;
        }
    }

    //comando per creare un post
    private void execCmdCreatePost(Command cmd, Response response) {
        response.payload = null;
        if (cmd.parameters.size() != 2)
            response.resultCode = ServiceResultCodes.ERR_WRONG_NUM_PARAMS;
        else {
            var currentUser = userman.getUser(userSession.username);
            var result = postsManager.createPost(currentUser, cmd.parameters.get(0), cmd.parameters.get(1));
            response.resultCode = result.getResultCode();
            response.payload = result.getPayload().toString();
        }
    }

    //comando per vedere un post
    private void execCmdShowPost(Command cmd, Response response) {
        response.payload = null;
        if (cmd.parameters.size() != 1) {
            response.resultCode = ServiceResultCodes.ERR_WRONG_NUM_PARAMS;
            return;
        }

        var currentUser = userman.getUser(userSession.username);
        int idPost;
        try {
            idPost = Integer.parseInt(cmd.parameters.get(0));
        } catch (NumberFormatException ex) {
            response.resultCode = ServiceResultCodes.ERR_IDPOST_NOT_NUMERIC;
            return;
        }

        var postDataResult = postsManager.showPost(currentUser, idPost);
        response.resultCode = postDataResult.getResultCode();
        if (response.resultCode != ServiceResultCodes.SUCCESS)
            return;

        var postData = postDataResult.getPayload();
        var post = postData.getPost();
        var autore = userman.getUserById(post.getIdAuthor());
        if (autore == null) {
            response.resultCode = ServiceResultCodes.ERR_AUTHOR_NOT_FOUND;
            return;
        }
        var postDetail = new PostDetailDto(post.getIdPost(), post.getTitolo(), autore.getUsername(),post.getContenuto());
        postDetail.setNumNeg(postData.getNumNeg());
        postDetail.setNumPos(postData.getNumPos());
        for (var aComment : postData.getPostComments()) {
            var authorOfComment = userman.getUserById(aComment.getIdAuthor());
            var authorName = authorOfComment == null ? "---" : authorOfComment.getUsername();
            var textOfComment = aComment.getText();
            postDetail.addComment(authorName, textOfComment);
        }
        response.payload = new Gson().toJson(postDetail);
    }

    //comando per eliminare un post
    private void execCmdDeletePost(Command cmd, Response response) {
        response.payload = null;
        if (cmd.parameters.size() != 1)
            response.resultCode = ServiceResultCodes.ERR_WRONG_NUM_PARAMS;
        else {
            var currentUser = userman.getUser(userSession.username);
            int idPost;
            try {
                idPost = Integer.parseInt(cmd.parameters.get(0));
            } catch (NumberFormatException ex) {
                response.resultCode = ServiceResultCodes.ERR_IDPOST_NOT_NUMERIC;
                return;
            }
            response.resultCode = postsManager.deletePost(currentUser, idPost);
        }
    }

    // comando per fare il rewin di un post
    private void execCmdRewinPost(Command cmd, Response response) {
        response.payload = null;
        if (cmd.parameters.size() != 1)
            response.resultCode = ServiceResultCodes.ERR_WRONG_NUM_PARAMS;
        else{
            var currentUser = userman.getUser(userSession.username);
            int idPost;
            try {
                idPost = Integer.parseInt(cmd.parameters.get(0));
            } catch (NumberFormatException ex) {
                response.resultCode = ServiceResultCodes.ERR_IDPOST_NOT_NUMERIC;
                return;
            }
            response.resultCode = postsManager.rewinPost(currentUser, idPost);
        }
    }

    // comando per ottenere il proprio wallet sia in bitcoins che wincoins in base al parametro 'isBitcoins'
    private void execCmdGetWallet(Command cmd, Response response, boolean isBitcoins){
        response.payload = null;
        if (cmd.parameters.size() != 0)
            response.resultCode = ServiceResultCodes.ERR_WRONG_NUM_PARAMS;
        else{
            double tasso = 1;
            if (isBitcoins) {
                try {
                    tasso = CurrencyConverter.RateWincoinToBitcoin();
                } catch (IOException e) {
                    response.resultCode = ServiceResultCodes.ERR_BITCOINS_RATE_NOT_AVAILABLE;
                    e.printStackTrace();
                    return;
                }
            }
            var currentUser = userman.getUser(userSession.username);
            var userWallet = walletManager.getWallet(currentUser);
            var transactionsDto = userWallet.getTransactions().stream()
                    .map(x -> new TransactionDto(x.getEarn(),x.getTimestamp())).toList();
            if (tasso != 1)
                for (var t:transactionsDto)
                    t.setEarn(t.getEarn()*tasso);
            var dto = new UserWalletDto(userWallet.getIdUser(),userWallet.getTotalWincoins()*tasso,transactionsDto,isBitcoins,tasso);
            response.payload = new Gson().toJson(dto);
            response.resultCode = ServiceResultCodes.SUCCESS;
        }
    }

    // comando per aggiungere un commento
    private void execCmdAddComment(Command cmd, Response response) {
        if (cmd.parameters.size() != 2)
            response.resultCode = ServiceResultCodes.ERR_WRONG_NUM_PARAMS;
        else{
            var currentUser = userman.getUser(userSession.username);
            int idPost;
            try {
                idPost = Integer.parseInt(cmd.parameters.get(0));
            } catch (NumberFormatException ex) {
                response.resultCode = ServiceResultCodes.ERR_IDPOST_NOT_NUMERIC;
                return;
            }
            response.resultCode =postsManager.addComment(currentUser,idPost,cmd.parameters.get(1));
        }
    }

    // comando per aggiungere un voto
    private void execCmdRatePost(Command cmd, Response response) {
        if (cmd.parameters.size() != 2)
            response.resultCode = ServiceResultCodes.ERR_WRONG_NUM_PARAMS;
        else{
            var currentUser = userman.getUser(userSession.username);
            int idPost;
            try {
                idPost = Integer.parseInt(cmd.parameters.get(0));
            } catch (NumberFormatException ex) {
                response.resultCode = ServiceResultCodes.ERR_IDPOST_NOT_NUMERIC;
                return;
            }

            int voto;
            try {
                voto = Integer.parseInt(cmd.parameters.get(1));
            } catch (NumberFormatException ex) {
                response.resultCode = ServiceResultCodes.ERR_VOTE_NOT_CORRECT;
                return;
            }
            response.resultCode = postsManager.addVote(currentUser,idPost,voto);
        }
    }
}
