package Client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ClientMain {
    private String host;
    private int port;

    // Costruttore per leggere i dati dal file JSON
    public ClientMain() {
        try (FileReader reader = new FileReader("src/Document/Connection.json")) {
            // Definisco il tipo ConcurrentHashMap<String, Object>
            Type tipoMappa = new TypeToken<ConcurrentHashMap<String, Object>>() {}.getType();       // serve per ottenere il tipo generico ConcurrentHashMap<String, Object> in modo che possa essere utilizzato in Gson per la serializzazione e deserializzazione
            ConcurrentHashMap<String, Object> map = new Gson().fromJson(reader, tipoMappa);

            // Carico i dati dal file
            this.host = map.get("host").toString();
            this.port = (int) Double.parseDouble(map.get("port").toString());


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Metodo per avviare la connessione al server
    public void startClient() throws Exception {
        try (Socket socket = new Socket(host, port)) {
            System.out.println("Connesso al server " + host + " sulla porta " + port);

            // Avvio la CLI
            CLI cli = new CLI(socket);
            cli.run();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ClientMain client = new ClientMain();
        // Avvio la connessione
        try {
            client.startClient();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
