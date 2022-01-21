package server.domain;

    // rappresenta la sessione utente. In questo caso contiene il solo username.
    public class UserSession {
        public  String username = null;
        // rende la sessione anonima (senza utente)
        public void clear() {
            username = null;
        }
    }

