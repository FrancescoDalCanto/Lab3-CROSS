package Client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CROSSClient {
    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    private final Gson gson = new Gson();

    //Variabile per tener traccia dell'utente
    private String currentUsername = null;

    // Per questo esempio, salviamo il salt generato in fase di registrazione
    // in modo da poterlo usare nelle successive operazioni (login, update)
    private static String storedSalt = null;

    public CROSSClient(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    /**
     * Metodo per la registrazione di un nuovo utente senza chiudere il socket.
     */
    protected synchronized void handleRegister(Scanner scanner) {
        String username;
        String password;

        // Richiedi username e password con validazione
        do {
            System.out.print("Username: ");
            username = scanner.nextLine().trim();

            System.out.print("Password: ");
            password = scanner.nextLine().trim();

            if (username.isEmpty() || password.isEmpty()) {
                System.out.println("Username e Password non possono essere vuoti. Riprova.");
            } else {
                break;
            }
        } while (true);

        // Creazione del JSON per la registrazione
        // NOTA: In questo caso inviamo la password in chiaro, con chiave "password"
        ConcurrentMap<String, Object> request = new ConcurrentHashMap<>();
        request.put("operation", "register");

        ConcurrentHashMap<String, String> values = new ConcurrentHashMap<>();
        values.put("username", username);
        values.put("password", password);

        request.put("values", values);
        String jsonRequest = gson.toJson(request);

        System.out.println("JSON inviato al server: " + jsonRequest);
        out.println(jsonRequest);

        try {
            // Ricezione della risposta dal server
            String serverResponse = in.readLine();
            System.out.println("JSON ricevuto dal server: " + serverResponse);

            // Decodifica della risposta JSON
            ConcurrentHashMap<String, Object> response = gson.fromJson(serverResponse, ConcurrentHashMap.class);
            double responseCode = (double) response.get("response");
            String errorMessage = (String) response.get("errorMessage");

            // Interpretazione della risposta
            if (responseCode == 100) {
                System.out.println("Registrazione completata con successo!");
            } else if (responseCode == 101) {
                System.out.println("Errore: Password non valida.");
            } else if (responseCode == 102) {
                System.out.println("Errore: Username già in uso.");
            } else {
                System.out.println("Errore generico: " + errorMessage);
            }
        } catch (IOException e) {
            System.out.println("Errore di comunicazione con il server.");
            e.printStackTrace();
        }
    }

    /**
     * Metodo per il login di un utente.
     * In questo esempio, la password viene inviata in chiaro.
     */
    protected synchronized boolean handleLogin(Scanner scanner) {
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        // Creazione del JSON per il login
        ConcurrentMap<String, Object> request = new ConcurrentHashMap<>();
        request.put("operation", "login");

        ConcurrentHashMap<String, String> values = new ConcurrentHashMap<>();
        values.put("username", username);
        values.put("password", password);

        request.put("values", values);
        String jsonRequest = gson.toJson(request);

        System.out.println("JSON inviato al server: " + jsonRequest);
        out.println(jsonRequest);

        try {
            // Ricezione della risposta dal server
            String serverResponse = in.readLine();
            System.out.println("JSON ricevuto dal server: " + serverResponse);

            // Decodifica della risposta JSON
            ConcurrentHashMap<String, Object> response = gson.fromJson(serverResponse, ConcurrentHashMap.class);
            double responseCode = (double) response.get("response");
            String errorMessage = (String) response.get("errorMessage");

            // Interpretazione della risposta
            if (responseCode == 100) {
                System.out.println("Login completato con successo!");
                // Salviamo lo username nel campo della classe
                currentUsername = username;
                return  true;
            } else {
                System.out.println("Errore: " + errorMessage);
            }
        } catch (IOException e) {
            System.out.println("Errore di comunicazione con il server.");
            e.printStackTrace();
        }
        return false;
    }


    /**
     * Metodo per il logout di un utente.
     **/
    protected synchronized boolean handleLogout(Scanner scanner) {

        // Creazione della richiesta JSON per il logout
        ConcurrentHashMap<String, Object> request = new ConcurrentHashMap<>();
        request.put("operation", "logout");

        ConcurrentHashMap<String, Object> values = new ConcurrentHashMap<>();
        values.put("username", currentUsername);

        request.put("values", values);
        String jsonRequest = gson.toJson(request);

        System.out.println("JSON inviato al server: " + jsonRequest);
        out.println(jsonRequest);

        try {
            String serverResponse = in.readLine();
            System.out.println("JSON ricevuto dal server: " + serverResponse);

            // Decodifica della risposta JSON
            ConcurrentHashMap<String, Object> response = gson.fromJson(serverResponse, ConcurrentHashMap.class);
            double responseCode = (double) response.get("response");
            String errorMessage = (String) response.get("errorMessage");

            // Interpretazione della risposta
            if (responseCode == 100) {
                System.out.println("Logout effettuato con successo!");
                return  false;
            } else {
                System.out.println("Errore: " + errorMessage);
            }
        } catch (IOException e) {
            System.out.println("Errore di comunicazione con il server.");
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Metodo per l'inserimento di un ordine.
     * Questo metodo non è stato modificato in relazione all'hashing delle password.
     **/
    protected synchronized void handleInsert(Scanner scanner) {
        Ordini ordini = null;
        try {
            ordini = new Ordini("127.0.0.1", 8080); // Corretto indirizzo IP
            // Qui imposto l'utente loggato (se serve).
            // ordini.setUsername(currentUsername);
        } catch (IOException e) {
            System.err.println("Errore di connessione al server: " + e.getMessage());
            return;
        }

        // Ora sai che l'utente è in currentUsername.
        // Se serve, lo stampi o lo usi per inviare una richiesta al server.
        // System.out.println("L'utente loggato è: " + currentUsername);

        System.out.print("Inserire il tipo (limit, market, stop): ");
        String type = scanner.nextLine().trim().toLowerCase();

        System.out.print("Inserisci la dimensione: ");
        int dim = scanner.nextInt();
        scanner.nextLine(); // Consuma il newline rimasto nel buffer

        System.out.print("Inserisci il tipo (bid/ask): ");
        String side = scanner.nextLine().trim().toLowerCase();

        System.out.print("Inserisci il valore: ");
        Double limitPrice = scanner.nextDouble();
        scanner.nextLine(); // Consuma il newline rimasto nel buffer

        int orderId = -1;
        switch (type) {
            case "limit":
                orderId = ordini.insertLimitOrder(side, dim, limitPrice, currentUsername);
                break;
            case "market":
                orderId = ordini.insertMarketOrder(side, dim, currentUsername);
                break;
            case "stop":
                orderId = ordini.insertStopOrder(side, dim, limitPrice, currentUsername);
                break;
            default:
                System.out.println("Tipo di ordine sconosciuto.");
        }

        if (orderId != -1) {
            System.out.println("Ordine inserito con ID: " + orderId);
        } else {
            System.out.println("Errore nell'inserimento dell'ordine.");
        }

        try {
            ordini.close(); // Chiude il socket per evitare memory leak
        } catch (IOException e) {
            System.err.println("Errore nella chiusura del client: " + e.getMessage());
        }
    }


    /**
     * Metodo per la cancellazione di un ordine.
     **/
    protected synchronized void handleDelete(Scanner scanner) {

    }

    /**
     * Metodo per la history di un utente.
     * (Implementazione da completare se necessario)
     **/
    protected synchronized void handleHistory(Scanner scanner) {
        System.out.print("Inserire la data (MMYYYY): ");
        String date = scanner.nextLine().trim();

        if (date.length() != 6) {
            System.out.println("La data deve essere in questo formato MMYYYY (quindi a 6 cifre totali).");
            return;
        }

        Map<String, Object> request = new HashMap<>();
        request.put("operation", "history");

        Map<String, Object> values = new HashMap<>();
        values.put("date", date);
        values.put("username", currentUsername);

        request.put("values", values);
        String jsonRequest = gson.toJson(request);

        out.println(jsonRequest);
        System.out.println("JSON inviato al server: " + jsonRequest);

        try {
            String serverResponse = in.readLine();
            System.out.println("JSON ricevuto dal server: " + serverResponse);

            Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> response = gson.fromJson(serverResponse, mapType);

            if (response == null || !response.containsKey("response")) {
                System.out.println("Errore: risposta del server non valida.");
                return;
            }

            double responseCode = (double) response.get("response");
            if (responseCode != 100) {
                System.out.println("Errore: " + response.get("errorMessage"));
                return;
            }

            String fileName = (String) response.get("file");
            File file = new File(fileName);

            if (!file.exists()) {
                System.out.println("Errore: il file non esiste o non è accessibile.");
                return;
            }

            // Legge il file JSON
            List<Map<String, String>> orders;
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                orders = gson.fromJson(reader, new TypeToken<List<Map<String, String>>>() {}.getType());
            }

            if (orders != null && !orders.isEmpty()) {
                System.out.println("Storico ordini:");
                for (Map<String, String> order : orders) {
                    System.out.println("ID Ordine: " + order.get("orderID"));
                    System.out.println("Tipo: " + order.get("type"));
                    System.out.println("Side: " + order.get("side"));
                    System.out.println("Dimensione: " + order.get("size"));
                    System.out.println("Prezzo: " + order.get("price"));
                    System.out.println("Data: " + order.get("date"));
                    System.out.println("------------------------------------------------");
                }
            } else {
                System.out.println("Nessun ordine trovato per la data specificata.");
            }

            // Cancella il file dopo aver letto i dati
            if (file.delete()) {
                System.out.println("DEBUG CLIENT: File " + fileName + " eliminato dopo la lettura.");
            } else {
                System.out.println("Errore nella cancellazione del file.");
            }

        } catch (IOException e) {
            System.out.println("Errore di comunicazione con il server.");
            e.printStackTrace();
        }
    }



    /**
     * Metodo per l'update delle credenziali di un utente.
     * In questo esempio viene richiesta la password corrente e la nuova password.
     */
    protected synchronized void handleUpdate(Scanner scanner) {
        System.out.print("Inserire username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Inserire la password corrente: ");
        String currentPassword = scanner.nextLine().trim();
        System.out.print("Inserire la nuova password: ");
        String newPassword = scanner.nextLine().trim();

        // Verifico se la nuova password coincide con quella attuale
        if (newPassword.equals(currentPassword)) {
            System.out.println("Errore: la nuova password non può essere uguale a quella corrente.");
            return;
        }

        // Creazione della richiesta JSON per l'update delle credenziali
        ConcurrentHashMap<String, Object> request = new ConcurrentHashMap<>();
        request.put("operation", "updateCredentials");

        ConcurrentHashMap<String, Object> values = new ConcurrentHashMap<>();
        values.put("username", username);
        values.put("currentPassword", currentPassword);
        values.put("newPassword", newPassword);

        request.put("values", values);
        String jsonRequest = gson.toJson(request);

        // Invio della richiesta al server
        System.out.println("JSON inviato al server: " + jsonRequest);
        out.println(jsonRequest);

        try {
            // Ricezione della risposta dal server
            String serverResponse = in.readLine();
            System.out.println("JSON ricevuto dal server: " + serverResponse);

            // Decodifica della risposta JSON
            ConcurrentHashMap<String, Object> response = gson.fromJson(serverResponse, ConcurrentHashMap.class);
            double responseCode = (double) response.get("response");
            String errorMessage = (String) response.get("errorMessage");

            // Interpretazione della risposta
            if (responseCode == 100) {
                System.out.println("Update completato con successo!");
            } else {
                System.out.println("Errore: " + errorMessage);
            }
        } catch (IOException e) {
            System.out.println("Errore di comunicazione con il server.");
            e.printStackTrace();
        }
    }
}