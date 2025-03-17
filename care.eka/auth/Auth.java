package care.eka.auth;

import care.eka.EkaCareClient;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Authentication handler for Eka Care API.
 */
public class Auth {
    private final EkaCareClient client;

    /**
     * Create a new Auth instance.
     *
     * @param client The EkaCare client
     */
    public Auth(EkaCareClient client) {
        this.client = client;
    }

    /**
     * Get an access token using client credentials.
     *
     * @return Token response containing access_token, refresh_token, etc.
     * @throws IOException If an error occurs during the request
     * 
     * Example:
     * <pre>
     * EkaCareClient client = new EkaCareClient("your_id", "your_secret");
     * JsonNode tokenResponse = client.getAuth().login();
     * System.out.println(tokenResponse.get("access_token").asText());
     * </pre>
     */
    public JsonNode login() throws IOException {
        Map<String, Object> jsonBody = new HashMap<>();
        jsonBody.put("client_id", client.getClientId());
        jsonBody.put("client_secret", client.getClientSecret());

        return client.request(
                "POST",
                "/connect-auth/v1/account/login",
                jsonBody,
                null,
                null,
                false
        );
    }

    /**
     * Refresh an access token using a refresh token.
     *
     * @param refreshToken The refresh token
     * @return Token response containing new access_token, refresh_token, etc.
     * @throws IOException If an error occurs during the request
     * 
     * Example:
     * <pre>
     * EkaCareClient client = new EkaCareClient("your_id", "your_secret");
     * JsonNode newTokens = client.getAuth().refreshToken("your_refresh_token");
     * client.setAccessToken(newTokens.get("access_token").asText());
     * </pre>
     */
    public JsonNode refreshToken(String refreshToken) throws IOException {
        Map<String, Object> jsonBody = new HashMap<>();
        jsonBody.put("refresh_token", refreshToken);

        return client.request(
                "POST",
                "/connect-auth/v1/account/refresh",
                jsonBody,
                null,
                null,
                false
        );
    }
}
