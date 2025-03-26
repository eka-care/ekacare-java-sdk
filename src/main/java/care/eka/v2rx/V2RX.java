package care.eka.v2rx;

import care.eka.EkaCareClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class V2RX {
    private final EkaCareClient client;
    private final ObjectMapper objectMapper;

    public V2RX(EkaCareClient client) {
        this.client = client;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Fetch the status of a voice recording session.
     *
     * @param sessionId The ID of the voice recording session
     * @return JsonNode containing the session status information
     * @throws IOException If an error occurs during the request
     *
     * Example:
     * <pre>
     * EkaCareClient client = new EkaCareClient("your_id", "your_secret");
     * JsonNode sessionStatus = client.getV2RX().fetchSessionStatus("session123");
     * System.out.println(sessionStatus.get("status").asText());
     * </pre>
     */
    public JsonNode getSessionStatus(String sessionId) throws IOException {
        if (sessionId == null || sessionId.isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }

        String endpoint = "/voice-record/api/status/" + sessionId;

        // Make a GET request to the endpoint
        return client.request("GET", endpoint);
    }

}
