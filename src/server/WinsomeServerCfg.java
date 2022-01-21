package server;

import shared.utils.ConfigReader;
import shared.utils.IncorrectLineConfigException;

import java.io.FileNotFoundException;
import java.io.IOException;

// configurazione del server
public class WinsomeServerCfg {

    public String registerHost = "localhost";
    public int registerPort = 7777;
    public String regServiceName = "WINSOME-REGSERVER";

    public String serviceHost = "localhost";
    public int servicePort = 7778;

    public String followerSvcHost = "localhost";
    public String followerSvcName = "FollowerNotifyServer";
    public int followerSvcPort = 7779;

    public int multicatsPort = 44444;
    public String multicastAddress = "239.255.32.32";
    //------------------------------------

    public int corePoolSize = 4;
    public int maximumPoolSize = 8;
    public long keepAliveTime = 20*1000;  //20 sec.
    public int sizeTaskQueue = 5;
    public long WalletServicePeriodInSec = 30;  // default in secondi
    public int percEarnForAuthor = 70; // default 70% guadagno per autore

    // caricammento da file
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
            regServiceName = cfgReader.getStringValue("regsvcname", regServiceName);

            // connessione al server TCP
            servicePort = cfgReader.getIntValue("tcpport", servicePort);
            serviceHost = cfgReader.getStringValue("server", serviceHost);

            followerSvcHost = cfgReader.getStringValue("regfollwersvchost", followerSvcHost);
            followerSvcName = cfgReader.getStringValue("regfollwersvcname", followerSvcName);
            followerSvcPort = cfgReader.getIntValue("regfollwersvcport", followerSvcPort);

            multicatsPort = cfgReader.getIntValue("mcastport", multicatsPort);
            multicastAddress = cfgReader.getStringValue("multicast", multicastAddress);

            corePoolSize = cfgReader.getIntValue("corepoolsize", corePoolSize);
            maximumPoolSize = cfgReader.getIntValue("maximumpoolsize", maximumPoolSize);
            keepAliveTime = cfgReader.getLongValue("keepalivetime", keepAliveTime);
            sizeTaskQueue = cfgReader.getIntValue("sizetaskqueue", sizeTaskQueue);
            WalletServicePeriodInSec = cfgReader.getLongValue("walletserviceperiodinsec", WalletServicePeriodInSec);
            percEarnForAuthor = cfgReader.getIntValue("percearnforauthor", percEarnForAuthor);
        }
        catch(NumberFormatException exc){
            System.out.println("Valore numerico errato in configurazione. Utilizzo dei valori predefiniti.");
        }
    }
}
