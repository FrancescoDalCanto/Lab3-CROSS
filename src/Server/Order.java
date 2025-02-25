package Server;

import java.util.List;
import java.util.Map;

public class Order {
    int orderId;
    String userId;
    String type;
    String orderType;
    int size;
    int price;
    int stopPrice;
    long timestamp;

    public Order(List<Map<String, String>> orders) {
        
    }


    public int getId(){
        return orderId;
    }
}
