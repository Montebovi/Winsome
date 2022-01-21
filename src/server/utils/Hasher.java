package server.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

// classe che incapsula il calcolo e verifica dello hash.
// usa SHA-256 e codica lo hash in string base64.
public class Hasher {

    public String Hash(String stringToHash) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(stringToHash.getBytes());
        var stringHash =Base64.getEncoder().encodeToString(messageDigest.digest());
        return stringHash;
    }

    // controllo se hash di clearString coincide con hashedString
    public boolean isValidHashPassword(String clearString, String hashedString) throws NoSuchAlgorithmException {
        var h = this.Hash(clearString);
        return h.equals(hashedString);
    }
}