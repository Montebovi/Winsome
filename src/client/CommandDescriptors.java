package client;

import shared.Command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// classe interna al package per contenere un decrittore di comando
class CommandDescriptor{
    String syntax;
    String description;
    CommandDescriptor(String syntax, String description){
        this.syntax = syntax;
        this.description = description;
    }
}

// classe per gestire i descrittori di comando (usata per fornire Help dei comandi)
public class CommandDescriptors{

    Map<Integer,CommandDescriptor> descriptors;  // contiene mappa codice -> descrittore del comando

    public CommandDescriptors(){
        descriptors = new HashMap<>();
        descriptors.put(Command.CMD_REGISTER,
                new CommandDescriptor(
                        "REGISTER <username> <password> <tag1> <tag2> ... <tagn>",
                        "Regitrazione utente. Sono ammessi al max 5 tags."));

        descriptors.put(Command.CMD_LOGIN,
        new CommandDescriptor(
                "LOGIN <username> <password>",
                "Login utente."));

        descriptors.put(Command.CMD_LOGOUT,
                new CommandDescriptor("LOGOUT <username>","Logout utente."));

        descriptors.put(Command.CMD_LIST_USERS,
        new CommandDescriptor("LIST-USERS",
                "Lista degli utenti registrati sul servizio che hanno almeno un tag in comune con l'utente corrente."));

        descriptors.put(Command.CMD_LIST_FOLLOWERS,
                new CommandDescriptor("LIST-FOLLOWERS",
                        "Lista dei propri follower."));
        descriptors.put(Command.CMD_LIST_FOLLOWING,
                new CommandDescriptor("LIST-FOLLOWING",
                        "Lista degli utenti di cui l'utente corrente è follower."));

        descriptors.put(Command.CMD_FOLLOW_USER,
                new CommandDescriptor("FOLLOW-USER <username>",
                        "Aggiungere utente <username> in elenco degli utenti seguiti dall'utente corrente."));

        descriptors.put(Command.CMD_UNFOLLOW_USER,
                new CommandDescriptor("UNFOLLOW-USER <username>",
                        "Rimuovere utente <username> dall'elenco degli utenti seguiti dall'utente corrente."));

        descriptors.put(Command.CMD_CREATE_POST,
                new CommandDescriptor("CREATE-POST <titolo> <contenuto>",
                        "Creazione di un post. Delimitare con il doppìo apice (\") o il singolo apice (') il titolo ed il contenuto."));

        descriptors.put(Command.CMD_VIEW_BLOG,
                new CommandDescriptor("VIEW-BLOG",
                        "Lista dei post di cui l'utente corrente è autore."));

        descriptors.put(Command.CMD_SHOW_FEED,
                new CommandDescriptor("SHOW-FEED",
                        "Lista dei post nel feed dell'utente corrente."));

        descriptors.put(Command.CMD_SHOW_POST,
                new CommandDescriptor("SHOW-POST <idpost>",
                        "Restituisce titolo, contenuto, numero di voti positivi, numero di voti negativi " +
                                "e commenti del post."));

        descriptors.put(Command.CMD_DELETE_POST,
                new CommandDescriptor("DELETE-POST <idpost>",
                        "Cancella un post (ammessa solo se l'utente corrente è l'autore)."));

        descriptors.put(Command.CMD_ADD_COMMENT,
                new CommandDescriptor("ADD-COMMENT <idpost> <commento>",
                        "Aggiunge un commento al post. Il testo del commento è racchiuso tra virgolette (\")."));

        descriptors.put(Command.CMD_RATE_POST,
                new CommandDescriptor("RATE-POST <idpost> <voto>",
                        "Assegnare un voto positivo o negativo ad un post. Voto vale +1 o -1."));

        descriptors.put(Command.CMD_REWIN_POST,
                new CommandDescriptor("REWIN-POST <idpost>",
                        "Operazione per effettuare il rewin di un post, ovvero per pubblicare nel " +
                                "proprio blog un post presente nel proprio feed. "));

        descriptors.put(Command.CMD_GET_WALLET,
                new CommandDescriptor("GET-WALLET",
                        "Operazione per recuperare il valore del proprio portafoglio."));

        descriptors.put(Command.CMD_GET_WALLETB,
                new CommandDescriptor("GET-WALLETB",
                        "Operazione per recuperare il valore del proprio portafoglio convertito in bitcoin."));

    }

    // restituisce la sintassi di con codice comando
    public String GetCommandSyntax(int commandKey){
        return descriptors.get(commandKey).syntax;
    }

    // restituisce la descrizione di con codice comando
    public String GetCommandDescription(int commandKey){
        return descriptors.get(commandKey).description;
    }
}