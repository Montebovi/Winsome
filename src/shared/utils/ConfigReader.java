package shared.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// Classe per la lettura di config da file testo.
// Sintassi molto semplice, per esempio:
//       # configurazione per la register (basato su RMI)
//       reghost = localhost
//       regport = 7777
//       regsvcname = WINSOME-REGSERVER
//       ......
public class ConfigReader {

    private final String filename;
    private final Map<String, String> map;

    public ConfigReader(String filename){
        map = new HashMap<String, String>();
        this.filename = filename;
    }

    public void LoadConfuguration() throws IOException, IncorrectLineConfigException {
        map.clear();
        var reader =  new BufferedReader(new FileReader(filename));
        String line;
        while ((line = reader.readLine()) != null) {
            processLine( line.trim());
        }
    }

    public int getIntValue(String key, int defaultvalue) throws NumberFormatException{
        if (!map.containsKey(key))
            return defaultvalue;
        return Integer.parseInt(map.get(key));
    }

    public long getLongValue(String key, long defaultvalue) throws NumberFormatException{
        if (!map.containsKey(key))
            return defaultvalue;
        return Long.parseLong(map.get(key));
    }

    public String  getStringValue(String key, String defaultValue){
        return map.getOrDefault(key,defaultValue);
    }

    // processa la linea gestendo i commenti
    private void processLine(String line) throws IncorrectLineConfigException {
        if (line.startsWith("#"))  // è un commento
            return;
        if (line.length() == 0)
            return;
        var words = line.split("=",2);

        if (words.length != 2)
            throw new IncorrectLineConfigException("Linea di configurazione non corretta sintatticamente.");
        var key = words[0].trim().toLowerCase();
        var value = words[1].trim();
        map.putIfAbsent(key,value);
    }
}

