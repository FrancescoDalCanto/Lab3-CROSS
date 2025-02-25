package Server;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class CROSSServer implements Runnable {
    private final Socket socket;
    private final Gson gson = new Gson();
    private final SessionManager sessionManager;

    public CROSSServer(Socket socket, SessionManager sessionManager ) {
        this.socket = socket;
        this.sessionManager = sessionManager;
    }

    @Override
    public void run() {
        System.out.println("Gestione della richiesta per il client...");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Ricevuto: " + inputLine);


                // Deserializzazione JSON per ottenere la richiesta
                OperationRequest request = gson.fromJson(inputLine, OperationRequest.class);
                String operazione = request.getOperation().toLowerCase();
                String username = (String) request.getValues().get("username");
                if (username == null) {
                    System.err.println("Errore: username nullo nella richiesta.");
                    out.println("{ \"response\": 400, \"errorMessage\": \"Username mancante nella richiesta\" }");
                    return;
                }

                switch (operazione) {
                    case "register":
                        Operazioni.Register(request.getValues(), out);
                        break;
                    case "login":
                        handleLogin(request, out);
                        break;
                    case "logout":
                        handleLogout(username, out);
                        break;
                    case "limitorder":
                    case "marketorder":
                    case "stoporder":
                        /* Assegno ID dell'ordine (ultimo + 1) */
                        /* Mi salvo in un file l'associzione ID ordine e Username */
                    case "cancelorder":
                    case "updatecredentials":
                    case "history":
                        if (SessionManager.isLoggedIn(username)) {
                            handleAuthenticatedOperation(operazione, request, out);
                        } else {
                            System.out.println("Tentativo di accesso non autorizzato da: " + username);
                            out.println("{ \"response\": 401, \"errorMessage\": \"User not authenticated\" }");
                        }
                        break;

                    default:
                        System.out.println("Operazione sconosciuta ricevuta: " + operazione);
                        out.println(Error.getBadRequestResponse());
                        break;
                }

            }
        } catch (IOException e) {
            System.out.println("Errore nella comunicazione con il client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gestisce il login con gestione della sessione
     */
    private void handleLogin(OperationRequest request, PrintWriter out) {
        String username = (String) request.getValues().get("username");

        if (username == null || username.isEmpty()) {
            System.err.println("Errore: Username nullo o mancante nella richiesta di login.");
            out.println("{ \"response\": 400, \"errorMessage\": \"Username mancante nella richiesta\" }");
            return;
        }

        // Controlla se l'utente è già loggato
        if (sessionManager.isLoggedIn(username)) {
            System.err.println("Errore: Utente già loggato - " + username);
            out.println("{ \"response\": 403, \"errorMessage\": \"User already logged in\" }");
            return;
        }

        // Esegue il login se non è già loggato
        if (Operazioni.Login(request.getValues(), out)) {
            Session newSession = new Session(username, socket);
            sessionManager.addSession(username, newSession);
            System.out.println("DEBUG: Sessione creata per " + username);
        }
    }



    /**
     * Gestisce il logout rimuovendo la sessione
     */
    private void handleLogout(String username, PrintWriter out) {
        if (!SessionManager.isLoggedIn(username)) {
            out.println(Error.getRegistrationErrorResponse(101));
            return;
        }
        SessionManager.removeSession(username);
        out.println(Error.getRegistrationErrorResponse(100));
    }

    /**
     * Gestisce operazioni che richiedono autenticazione
     */
    private void handleAuthenticatedOperation(String operazione, OperationRequest request, PrintWriter out) {

        switch (operazione) {
            case "cancelorder":
                Operazioni.CancelOrder(request.getValues(), out);
                break;
            case "updatecredentials":
                Operazioni.UpdateCredentials(request.getValues(), out);
                break;
            case "limitorder":
                Operazioni.LimitOrder( request.getValues(), out);
                break;
            case "marketorder":
                Operazioni.MarketOrder(request.getValues(), out);
                break;
            case "stoporder":
                Operazioni.StopOrder(request.getValues(), out);
                break;
            case "history":
                Operazioni.History(request.getValues(), out);
                break;
            default:
                out.println(Error.getBadRequestResponse());
        }
    }
}
