package client;

import shared.utils.ConfigReader;
import shared.utils.IncorrectLineConfigException;

import java.io.FileNotFoundException;
import java.io.IOException;

// classe per la configurazione sul client
public class ClientConfig {
    public String registerHost = "localhost";
    public int registerPort = 7777;
    public String regServiceName ="WINSOME-REGSERVER";

    public String followerSvcHost = "localhost";
    public String followerSvcName = "FollowerNotifyServer";
    public int followerSvcPort = 7779;

    public int port = 7778;
    public String host = "localhost";

    public int multicatsPort = 44444;
    public String multicastAddress = "239.255.32.32";

    // caricamento del config da file
    public void LoadConfig(String filename){
        var cfgReader = new ConfigReader(filename);

        try {
            cfgReader.LoadConfuguration();
        }
        catch (FileNotFoundException e) {
            System.out.println("File di configurazione non trovato. Utilizzo dei valori predefiniti.");
        }
        catch (IOException e) {
            e.printStackTrace();
        } catch (IncorrectLineConfigException e) {
            System.out.println("File di configurazione non corretto. Utilizzo dei valori predefiniti.");
        }

        try {
            registerHost = cfgReader.getStringValue("reghost", registerHost);
            registerPort = cfgReader.getIntValue("regport", registerPort);
            regServiceName= cfgReader.getStringValue("regsvcname", regServiceName);

            // connessione al server TCP
            port = cfgReader.getIntValue("tcpport",port);
            host = cfgReader.getStringValue("server",host);

            followerSvcHost = cfgReader.getStringValue("regfollwersvchost",followerSvcHost);
            followerSvcName= cfgReader.getStringValue("regfollwersvcname", followerSvcName);
            followerSvcPort = cfgReader.getIntValue("regfollwersvcport", followerSvcPort);

             multicatsPort = cfgReader.getIntValue("mcastport",multicatsPort);
             multicastAddress = cfgReader.getStringValue("multicast",multicastAddress);
        }
        catch(NumberFormatException exc){
            System.out.println("Valore numerico errato in configurazione. Utilizzo dei valori predefiniti.");
        }

    }

}
