package Server;

import CommonClass.CompraVendita;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.List;


import java.time.LocalDate;


public class Operazioni {

    // File degli utenti e lock per la sincronizzazione
    private static final File usersPath = new File("src/Document/Users.json");
    private static final Object usersLock = new Object();

    // File degli ordini e lock per la sincronizzazione
    private static final File ordersPath = new File("src/Document/Orders.json");
    private static final Object ordersLock = new Object();


    private static final CompraVendita cv;

    static {
        try {
            cv = new CompraVendita();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Registra utente dentro al file Json
     * @param values
     * @param out
     */
    public static void Register(Map<String, Object> values, PrintWriter out) {
        String username = (String) values.get("username");
        String password = (String) values.get("password");

        // Genera un salt e calcola l'hash della password
        String salt = HashUtils.generateSalt(16);
        String passwordHash = HashUtils.computeSHA256Hash(password, salt);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<Map<String, String>> users;

        synchronized (usersLock) {
            // Legge la lista degli utenti dal file
            if (usersPath.exists()) {
                try (FileReader reader = new FileReader(usersPath)) {
                    Type listType = new TypeToken<List<Map<String, String>>>() {}.getType();
                    users = gson.fromJson(reader, listType);
                    if (users == null) {
                        users = new ArrayList<>();
                    }
                } catch (IOException e) {
                    out.println(Error.getRegistrationErrorResponse(103));
                    return;
                }
            } else {
                // Se il file non esiste, restituisce un errore
                out.println(Error.getRegistrationErrorResponse(103));
                return;
            }

            // Controlla se l'username è già presente (case-insensitive)
            for (Map<String, String> user : users) {
                if (user.get("username") != null && user.get("username").equalsIgnoreCase(username)) {
                    out.println(Error.getRegistrationErrorResponse(102));
                    return;
                }
            }

            // Crea il nuovo utente e lo aggiunge
            Map<String, String> newUser = new HashMap<>();
            newUser.put("username", username);
            newUser.put("passwordHash", passwordHash);
            newUser.put("salt", salt);
            users.add(newUser);

            // Scrive la lista aggiornata nel file JSON (formattato)
            try (FileWriter writer = new FileWriter(usersPath)) {
                gson.toJson(users, writer);
            } catch (IOException e) {
                out.println(Error.getRegistrationErrorResponse(103));
                return;
            }
        }
        // Restituisce OK (codice 100)
        out.println(Error.getRegistrationErrorResponse(100));
    }

    /**
     * Esegue il Login di un utente
     * @param values
     * @param out
     * @return
     */
    public static boolean Login(Map<String, Object> values, PrintWriter out) {
        String username = (String) values.get("username");
        String password = (String) values.get("password");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            out.println(Error.getRegistrationErrorResponse(10));
            return false;
        }

        if (SessionManager.isLoggedIn(username)) {
            out.println(Error.getRegistrationErrorResponse(102));
            return false;
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<Map<String, String>> users;

        try (Reader reader = new FileReader(usersPath)) {
            Type listType = new TypeToken<List<Map<String, String>>>() {}.getType();
            users = gson.fromJson(reader, listType);
            if (users == null) {
                users = new ArrayList<>();
            }
        } catch (IOException e) {
            System.err.println("Errore nel caricamento degli utenti: " + e.getMessage());
            out.println(Error.getInternalServerErrorResponse());
            return false;
        }

        // Cerca l'utente nel JSON
        for (Map<String, String> user : users) {
            if (user.get("username").equals(username)) {
                String storedSalt = user.get("salt");
                String storedHash = user.get("passwordHash");

                // Hash della password inserita con il salt memorizzato
                String computedHash = HashUtils.computeSHA256Hash(password, storedSalt);

                if (computedHash.equals(storedHash)) {
                    // Login riuscito, aggiungere la sessione
                    SessionManager.addSession(username, new Session(username, null));
                    out.println(Error.getRegistrationErrorResponse(100));
                    return true;
                }
            }
        }

        // Se nessuna corrispondenza trovata
        out.println(Error.getRegistrationErrorResponse(101));
        return false;
    }

    /**
     *
     * @param values
     * @param out
     */
    public static void LimitOrder(Map<String, Object> values, PrintWriter out) {
        String username = (String) values.get("username");
        String side = (String) values.get("side");
        int size = ((Number) values.get("size")).intValue();
        int orderId = ((Number) values.get("orderId")).intValue();
        float price = ((Number) values.get("price")).floatValue();

        // Ottieni la data corrente
        LocalDate currentDate = LocalDate.now();

        // Ottieni l'anno corrente come stringa
        String annoCorrente = Integer.toString(currentDate.getYear());

        // Ottieni il mese corrente come intero e formattalo a due cifre
        String meseCorrente = String.format("%02d", currentDate.getMonthValue());

        // Concateno mese e anno per avere il formato richiesto
        String date = meseCorrente.concat(annoCorrente);

        System.out.println("Data formattata: " + date);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<Map<String, String>> orders = new ArrayList<>();

        synchronized (ordersLock) {
            if (ordersPath.exists()) {
                try (FileReader reader = new FileReader(ordersPath)) {
                    Type listType = new TypeToken<List<Map<String, String>>>() {}.getType();
                    orders = gson.fromJson(reader, listType);
                    if (orders == null) {
                        orders = new ArrayList<>();
                    }
                } catch (IOException e) {
                    System.err.println("Errore nella lettura del file JSON: " + e.getMessage());
                    out.println(Error.getRegistrationErrorResponse(103));
                    return;
                }
            } else {
                // Se il file non esiste, restituisce un errore
                out.println(Error.getRegistrationErrorResponse(103));
                return;
            }

            Map<String, String> newOrder = new HashMap<>();
            newOrder.put("username", username);
            newOrder.put("type","Limit");
            newOrder.put("side", side);
            newOrder.put("size", String.valueOf(size));
            newOrder.put("price", String.valueOf(price));
            newOrder.put("orderID", String.valueOf(orderId));
            newOrder.put("date", date);
            orders.add(newOrder);

            Order order = new Order(orders);


            try (FileWriter writer = new FileWriter(ordersPath)) {
                gson.toJson(orders, writer);
                System.out.println("Ordine salvato con successo.");
            } catch (IOException e) {
                System.err.println("Errore nella scrittura del file JSON: " + e.getMessage());
                out.println(Error.getRegistrationErrorResponse(103));
                return;
            }
        }

        out.println(Error.getRegistrationErrorResponse(100));
    }

    /**
     *
     * @param values
     * @param out
     */
    public static void MarketOrder(Map<String, Object> values, PrintWriter out) {
        String username = (String) values.get("username");
        int orderId = ((Number) values.get("orderId")).intValue();
        String side = (String) values.get("side");
        int size = ((Number) values.get("size")).intValue();

        // Ottengo la data corrente
        LocalDate currentDate = LocalDate.now();
        // Estraggo l'esame corrente
        String annoCorrente = Integer.toString(currentDate.getYear());
        // Estraggo il mese corrente e lo formatto sempre a due cifre
        String meseCorrente = Integer.toString(Integer.parseInt("%02d"), currentDate.getMonthValue());

        // Concateno mese e anno per avere il formato richiesto
        String date = meseCorrente.concat(annoCorrente);


        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<Map<String, String>> orders = new ArrayList<>();

        synchronized (ordersLock) {
            if (ordersPath.exists()) {
                try (FileReader reader = new FileReader(ordersPath)) {
                    Type listType = new TypeToken<List<Map<String, String>>>() {
                    }.getType();
                    orders = gson.fromJson(reader, listType);
                    if (orders == null) {
                        orders = new ArrayList<>();
                    }
                } catch (IOException e) {
                    System.err.println("Errore nella lettura del file JSON: " + e.getMessage());
                    out.println(Error.getRegistrationErrorResponse(103));
                    return;
                }
            } else {
                // Se il file non esiste, restituisce un errore
                out.println(Error.getRegistrationErrorResponse(103));
                return;
            }
        }
        // Preparo la mappa per inserirla nel documento JSON
        Map<String, String> newOrder = new HashMap<>();
        newOrder.put("username", username);
        newOrder.put("type","market");
        newOrder.put("side", String.valueOf(side));
        newOrder.put("size", String.valueOf(size));
        newOrder.put("orderID", String.valueOf(orderId));
        newOrder.put("date", date);
        // La mappa la inserisco in una lista per inserire un unico "blocco"
        orders.add(newOrder);

        try (FileWriter writer = new FileWriter(ordersPath)) {
            gson.toJson(orders, writer);
            System.out.println("Ordine salvato con successo.");
        } catch (IOException e) {
            System.err.println("Errore nella scrittura del file JSON: " + e.getMessage());
            out.println(Error.getRegistrationErrorResponse(103));
            return;
        }
        out.println(Error.getRegistrationErrorResponse(100));
    }

    /**
     *
     * @param values
     * @param out
     */
    public static void StopOrder(Map<String, Object> values, PrintWriter out) {
        String username = (String) values.get("username");
        String side = (String) values.get("side");
        int size = ((Number) values.get("size")).intValue();
        int orderId = ((Number) values.get("orderId")).intValue();
        float price = ((Number) values.get("stopPrice")).floatValue();

        // Ottengo la data corrente
        LocalDate currentDate = LocalDate.now();
        // Estraggo l'esame corrente
        String annoCorrente = Integer.toString(currentDate.getYear());
        // Estraggo il mese corrente e lo formatto sempre a due cifre
        String meseCorrente = Integer.toString(Integer.parseInt("%02d"), currentDate.getMonthValue());

        // Concateno mese e anno per avere il formato richiesto
        String date = meseCorrente.concat(annoCorrente);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<Map<String, String>> orders = new ArrayList<>();

        synchronized (ordersLock) {
            if (ordersPath.exists()) {
                try (FileReader reader = new FileReader(ordersPath)) {
                    Type listType = new TypeToken<List<Map<String, String>>>() {
                    }.getType();
                    orders = gson.fromJson(reader, listType);
                    if (orders == null) {
                        orders = new ArrayList<>();
                    }
                } catch (IOException e) {
                    System.err.println("Errore nella lettura del file JSON: " + e.getMessage());
                    out.println(Error.getRegistrationErrorResponse(103));
                    return;
                }
            } else {
                // Se il file non esiste, restituisce un errore
                out.println(Error.getRegistrationErrorResponse(103));
                return;
            }
        }
        Map<String, String> newOrder = new HashMap<>();
        newOrder.put("username", username);
        newOrder.put("type","stop");
        newOrder.put("side", String.valueOf(side));
        newOrder.put("size", String.valueOf(size));
        newOrder.put("price", String.valueOf(price));
        newOrder.put("orderID", String.valueOf(orderId));
        newOrder.put("date", date);
        orders.add(newOrder);

        try (FileWriter writer = new FileWriter(ordersPath)) {
            gson.toJson(orders, writer);
            System.out.println("Ordine salvato con successo.");
        } catch (IOException e) {
            System.err.println("Errore nella scrittura del file JSON: " + e.getMessage());
            out.println(Error.getRegistrationErrorResponse(103));
            return;
        }

        out.println(Error.getRegistrationErrorResponse(100));
    }


    /**
     *
     * @param values Mappa contenente i parametri della richiesta, incluso il mese nel formato MMYYYY.
     * @param out PrintWriter per inviare la risposta al client.
     */
    public static void History(Map<String, Object> values, PrintWriter out) {
        /* Leggo dal file di associazione tutti gli ordini relativi all'utente */


        if (values == null) {
            out.println("{ \"response\": 400, \"errorMessage\": \"Dati di richiesta mancanti\" }");
            return;
        }

        String month = (String) values.get("date");
        String username = (String) values.get("username"); // Aggiunto il recupero dell'username

        if (month == null || !month.matches("\\d{6}")) {
            out.println("{ \"response\": 400, \"errorMessage\": \"Formato mese non valido. Usa MMYYYY.\" }");
            return;
        }

        if (username == null || username.isEmpty()) {
            out.println("{ \"response\": 400, \"errorMessage\": \"Username mancante.\" }");
            return;
        }

        System.out.println("DEBUG: Mese ricevuto -> " + month);
        System.out.println("DEBUG: Username ricevuto -> " + username);

        List<Map<String, Object>> filteredOrders = new ArrayList<>();

        synchronized (ordersLock) {
            File file = new File(String.valueOf(ordersPath));
            if (!file.exists() || !file.canRead()) {
                out.println("{ \"response\": 500, \"errorMessage\": \"File degli ordini non trovato o non accessibile.\" }");
                return;
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            List<Map<String, Object>> completedOrders;

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                completedOrders = gson.fromJson(br, new TypeToken<List<Map<String, Object>>>() {}.getType());
                if (completedOrders == null) completedOrders = new ArrayList<>();
            } catch (IOException e) {
                e.printStackTrace();
                out.println("{ \"response\": 500, \"errorMessage\": \"Errore nel recupero degli ordini.\" }");
                return;
            }

            // Filtra gli ordini per data e username
            for (Map<String, Object> order : completedOrders) {
                String orderDate = (String) order.get("date");
                String orderUser = (String) order.get("username"); // Supponiamo che gli ordini abbiano il campo "username"

                if (orderDate != null && orderDate.equals(month) && orderUser != null && orderUser.equals(username)) {
                    filteredOrders.add(order);
                }
            }
        }

        // Scrittura dei risultati in un file per il client
        String clientFileName = "history_" + username + ".json";
        File clientFile = new File(clientFileName);

        try (FileWriter writer = new FileWriter(clientFile)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(filteredOrders, writer);
            System.out.println("DEBUG SERVER: File JSON creato -> " + clientFileName);
        } catch (IOException e) {
            e.printStackTrace();
            out.println("{ \"response\": 500, \"errorMessage\": \"Errore nella scrittura del file.\" }");
            return;
        }

        // Avvisa il client che il file è pronto
        out.println("{ \"response\": 100, \"file\": \"" + clientFileName + "\" }");
        out.flush();
    }

    /**
     *
     * @param values
     * @param out
     */
    public static void CancelOrder(Map<String, Object> values, PrintWriter out) {

    }

    /**
     *
     * @param values
     * @param out
     */
    public static void UpdateCredentials(Map<String, Object> values, PrintWriter out) {
        String username = (String) values.get("username");
        String current_password = (String) values.get("currentPassword");
        String new_password = (String) values.get("newPassword");

        if (username == null || current_password == null || new_password == null ||
                username.isEmpty() || current_password.isEmpty() || new_password.isEmpty()) {
            out.println(Error.getBadRequestResponse());
            return;
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<Map<String, String>> users;

        synchronized (usersLock) {
            // Caricamento degli utenti dal file JSON
            try (Reader reader = new FileReader(usersPath)) {
                Type listType = new TypeToken<List<Map<String, String>>>() {
                }.getType();
                users = gson.fromJson(reader, listType);
                if (users == null) {
                    users = new ArrayList<>();
                }
            } catch (IOException e) {
                System.err.println("Errore nella lettura del file utenti: " + e.getMessage());
                out.println(Error.getInternalServerErrorResponse());
                return;
            }

            // Cerca l'utente nel JSON
            boolean userFound = false;
            for (Map<String, String> user : users) {
                if (user.get("username").equals(username)) {
                    userFound = true;
                    String storedSalt = user.get("salt");
                    String storedHash = user.get("passwordHash");

                    // Verifica la password attuale
                    String computedHash = HashUtils.computeSHA256Hash(current_password, storedSalt);
                    if (!computedHash.equals(storedHash)) {
                        out.println(Error.getUnauthorizedResponse());
                        return;
                    }

                    // Genera un nuovo salt e calcola l'hash della nuova password
                    String newSalt = HashUtils.generateSalt(16);
                    String newPasswordHash = HashUtils.computeSHA256Hash(new_password, newSalt);

                    // Aggiorna i dati dell'utente
                    user.put("passwordHash", newPasswordHash);
                    user.put("salt", newSalt);

                    break;
                }
            }

            if (!userFound) {
                out.println(Error.getNotUser());
                return;
            }

            // Salva il file aggiornato
            try (Writer writer = new FileWriter(usersPath)) {
                gson.toJson(users, writer);
            } catch (IOException e) {
                System.err.println("Errore nella scrittura del file utenti: " + e.getMessage());
                out.println(Error.getInternalServerErrorResponse());
                return;
            }
        }
        out.println(Error.getRegistrationErrorResponse(100));
    }

}
