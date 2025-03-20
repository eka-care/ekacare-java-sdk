package care.eka.v2rx;

import care.eka.EkaCareClient;
import com.fasterxml.jackson.databind.ObjectMapper;

public class V2RX {
    private final EkaCareClient client;
    private final ObjectMapper objectMapper;

    public V2RX(EkaCareClient client) {
        this.client = client;
        this.objectMapper = new ObjectMapper();
    }


}
