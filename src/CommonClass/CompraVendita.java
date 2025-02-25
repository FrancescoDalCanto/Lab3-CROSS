package CommonClass;

import Server.Order;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

public class CompraVendita {
    private static final ConcurrentHashMap<Integer, Order> ordersMap = new ConcurrentHashMap<>();
    private static final String ORDER_FILE = "src/Document/Orders.json";
    private static final Gson gson = new Gson();

    public CompraVendita() throws Exception {
        loadOrders();
    }



    /* Carico File */
    public static void loadOrders() throws Exception {
        File file = new File(ORDER_FILE);
        if (file.exists()) {
            String content = new String(Files.readAllBytes(Paths.get(ORDER_FILE)));
            if (!content.isEmpty()) {
                ConcurrentHashMap<Integer, Order> tempMap = gson.fromJson(content, new TypeToken<ConcurrentHashMap<Integer, Order>>() {}.getType());
                ordersMap.putAll(tempMap);
            }
        }
    }

    /* Esegue File */
    public static void matching(){

    }


    /* Inserimeto ordine con ricalcolo */
    public void saveOrders(Order order) throws Exception {
        ordersMap.put(order.getId(), order);
        /* lo devo implementare*/
        matching();
    }
}
