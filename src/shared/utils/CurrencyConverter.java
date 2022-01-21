package shared.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

// Classe che implementa la simulazione della conversione da wincoins a bitcoins attraverso il servizio RANDOM.ORG
public  class CurrencyConverter {

    private static final String RANDOM_ORG_URL = "https://www.random.org/decimal-fractions/?num=1&dec=10&col=1&format=plain&rnd=new";

    // versione con lo stream
//    public static double getRandom2() throws IOException {
//        URL url = new URL(RANDOM_ORG_URL);
//        InputStream uin = url.openStream();
//        BufferedReader in = new BufferedReader(new InputStreamReader(uin));
//        var line = in.readLine();
//        return Double.parseDouble(line);
//    }

    // ritorna un numero random double tra 0 e 1
    private static double getRandom() throws IOException {
        URL url = new URL(RANDOM_ORG_URL);
        Scanner sc = new Scanner(url.openStream());
        return Double.parseDouble(sc.next());
    }

    // il tasso di conversione viene simulato con un valore random tra 0.5 e 1.5
    public static double RateWincoinToBitcoin() throws IOException {
        return getRandom()+0.5;
    }
}
