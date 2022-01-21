package shared;

import shared.utils.ServiceResultCodes;

// classe per i response dal server (inviata in formato json)
public class Response {
    public int resultCode;  // codici definiti in shared.utils.ServiceResultCodes
    public String payload;  // contenuto che dipende dal comando e da resultcode.

    public Response(int resultCode, String payload){
        this.resultCode = resultCode;
        this.payload = payload;
    }

    // transforma il codice di response in messaggio.
    public String getResponseMessage(){
        switch (resultCode){
            case ServiceResultCodes.SUCCESS: return "Operazione eseguita con successo.";
            // ------- errori
            case ServiceResultCodes.ERR_INVALID_USERNAME: return "Nome utente non valido";
            case ServiceResultCodes.ERR_USERNAME_NOT_EXISTING: return "Nome utente non esistente";
            case ServiceResultCodes.ERR_ALREADY_FOLLOWERS: return "Utente corrente è già follower.";
            case ServiceResultCodes.ERR_PASSWORD_TOOSHORT: return "Password troppo corta.";
            case ServiceResultCodes.ERR_TOO_MUCH_TAGS: return "Il numero di tags eccede il limite consentito.";
            case ServiceResultCodes.ERR_WRONG_PWD: return "Password non corretta.";
            case ServiceResultCodes.ERR_WRONG_NUM_PARAMS: return "Numero parametri non valido";
            case ServiceResultCodes.ERR_USER_NOT_FOLLOWED: return "Utente corrente non risulta follower.";
            case ServiceResultCodes.ERR_USERNAME_USED: return  "Nome utente già presente.";
            case ServiceResultCodes.ERR_CONTENT_TOO_LONG: return "Contenuto del post eccede la dimensione massima.";
            case ServiceResultCodes.ERR_IDPOST_NOT_NUMERIC: return "Id del post non valido (deve essere numerico).";
            case ServiceResultCodes.ERR_ACTION_DENIED: return "Operazione non ammessa.";
            case ServiceResultCodes.ERR_POST_NOT_FOUND: return "Post non trovato.";
            case ServiceResultCodes.ERR_AUTHOR_NOT_FOUND: return "Autore non trovato.";
            case ServiceResultCodes.ERR_POST_NOT_ACCESSIBLE: return "Post non accessibile.";
            case ServiceResultCodes.ERR_TITLE_TOO_LONG: return "Titolo del post eccede la dimensione massima.";
            case ServiceResultCodes.ERR_BITCOINS_RATE_NOT_AVAILABLE: return "Tasso per bitcoins non disponibile.";
            case ServiceResultCodes.ERR_VOTE_NOT_CORRECT: return "Valore del voto non corretto.";
            //-----
            default: return "Codice di risposta sconosciuto ("+resultCode+").";
        }
    }
}
