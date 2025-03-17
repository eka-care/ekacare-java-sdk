package care.eka;

import care.eka.abdm.Profile;
import care.eka.auth.Auth;
import care.eka.records.Records;
import care.eka.tools.EkaFileUploader;
import care.eka.utils.exceptions.EkaCareAPIError;
import care.eka.utils.exceptions.EkaCareAuthError;
import care.eka.vitals.Vitals;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Main client for interacting with the Eka Care API.
 */
public class EkaCareClient {
    private final String clientId;
    private final String clientSecret;
    private final String baseUrl;
    private String accessToken;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    // API modules
    private final Auth auth;
    private final Records records;
    private final EkaFileUploader files;
    private final Vitals vitals;
    private final Profile abdmProfile;

    /**
     * Create a new EkaCare client.
     *
     * @param clientId Your Eka Care API client ID
     * @param clientSecret Your Eka Care API client secret
     */
    public EkaCareClient(String clientId, String clientSecret) {
        this(clientId, clientSecret, "https://api.eka.care", null);
    }

    /**
     * Create a new EkaCare client with a custom base URL.
     *
     * @param clientId Your Eka Care API client ID
     * @param clientSecret Your Eka Care API client secret
     * @param baseUrl Base URL for the Eka Care API
     */
    public EkaCareClient(String clientId, String clientSecret, String baseUrl) {
        this(clientId, clientSecret, baseUrl, null);
    }

    /**
     * Create a new EkaCare client with an existing access token.
     *
     * @param clientId Your Eka Care API client ID
     * @param clientSecret Your Eka Care API client secret
     * @param baseUrl Base URL for the Eka Care API
     * @param accessToken An existing access token
     */
    public EkaCareClient(String clientId, String clientSecret, String baseUrl, String accessToken) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.baseUrl = baseUrl;
        this.accessToken = accessToken;

        // Initialize HTTP client
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        
        this.objectMapper = new ObjectMapper();
        
        // Initialize API modules
        this.auth = new Auth(this);
        this.records = new Records(this);
        this.files = new EkaFileUploader(this);
        this.vitals = new Vitals(this);
        this.abdmProfile = new Profile(this);
    }

    /**
     * Get the current access token or request a new one if needed.
     *
     * @return The access token
     */
    public String getAccessToken() {
        if (accessToken == null || accessToken.isEmpty()) {
            try {
                JsonNode tokenResponse = auth.login();
                accessToken = tokenResponse.get("access_token").asText();
            } catch (Exception e) {
                throw new RuntimeException("Failed to obtain access token", e);
            }
        }
        return accessToken;
    }

    /**
     * Manually set the access token.
     *
     * @param token The new access token
     */
    public void setAccessToken(String token) {
        this.accessToken = token;
    }

    /**
     * Make a request to the Eka Care API.
     *
     * @param method HTTP method (GET, POST, PUT, DELETE, etc.)
     * @param endpoint API endpoint path
     * @param params Query parameters
     * @param jsonBody JSON body as Map
     * @param headers Additional headers
     * @param authRequired Whether auth token is required
     * @return Response as JsonNode
     */
    public JsonNode request(String method, String endpoint, Map<String, String> params, 
                            Map<String, Object> jsonBody, Map<String, String> headers, 
                            boolean authRequired) throws IOException {
        
        // Build the URL with query parameters
        HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + endpoint).newBuilder();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
            }
        }
        
        // Prepare request body if needed
        RequestBody requestBody = null;
        if (jsonBody != null) {
            String json = objectMapper.writeValueAsString(jsonBody);
            requestBody = RequestBody.create(json, MediaType.parse("application/json"));
        }
        
        // Build the request
        Request.Builder requestBuilder = new Request.Builder()
                .url(urlBuilder.build());
        
        // Set method and body
        switch (method.toUpperCase()) {
            case "GET":
                requestBuilder.get();
                break;
            case "POST":
                requestBuilder.post(requestBody != null ? requestBody : 
                        RequestBody.create(new byte[0], null));
                break;
            case "PUT":
                requestBuilder.put(requestBody != null ? requestBody : 
                        RequestBody.create(new byte[0], null));
                break;
            case "DELETE":
                requestBuilder.delete(requestBody != null ? requestBody : null);
                break;
            case "PATCH":
                requestBuilder.patch(requestBody != null ? requestBody : 
                        RequestBody.create(new byte[0], null));
                break;
            default:
                throw new IllegalArgumentException("Unsupported method: " + method);
        }
        
        // Add headers
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.header(entry.getKey(), entry.getValue());
            }
        }
        
        // Add authentication if required
        if (authRequired) {
            requestBuilder.header("Authorization", "Bearer " + getAccessToken());
        }
        
        // Add client ID header
        if (clientId != null && !clientId.isEmpty()) {
            requestBuilder.header("client-id", clientId);
        }
        
        // Execute the request
        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            if (!response.isSuccessful()) {
                if (response.code() == 401) {
                    throw new EkaCareAuthError("Authentication error: " + response.code() + 
                            " - " + response.body().string());
                }
                throw new EkaCareAPIError("API error " + response.code() + ": " + 
                        response.body().string());
            }
            
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return objectMapper.createObjectNode()
                        .put("status", "success")
                        .put("status_code", response.code());
            }
            
            String contentType = response.header("Content-Type", "");
            if (contentType.contains("application/json")) {
                return objectMapper.readTree(responseBody.string());
            } else if (contentType.contains("image/") || contentType.contains("application/pdf")) {
                // Binary content handling would be implemented separately
                // For now, we'll return a simple JSON node indicating binary content
                return objectMapper.createObjectNode()
                        .put("content_type", contentType)
                        .put("content_length", responseBody.contentLength());
            } else {
                try {
                    return objectMapper.readTree(responseBody.string());
                } catch (Exception e) {
                    return objectMapper.createObjectNode()
                            .put("text_response", responseBody.string());
                }
            }
        }
    }
    
    /**
     * Make a request to the Eka Care API with default parameters.
     *
     * @param method HTTP method
     * @param endpoint API endpoint path
     * @return Response as JsonNode
     */
    public JsonNode request(String method, String endpoint) throws IOException {
        return request(method, endpoint, null, null, null, true);
    }
    
    /**
     * Make a request with JSON body.
     *
     * @param method HTTP method
     * @param endpoint API endpoint path
     * @param jsonBody JSON body as Map
     * @return Response as JsonNode
     */
    public JsonNode request(String method, String endpoint, Map<String, Object> jsonBody) throws IOException {
        return request(method, endpoint, null, jsonBody, null, true);
    }
    
    /**
     * Make a request with query parameters.
     *
     * @param method HTTP method
     * @param endpoint API endpoint path
     * @param params Query parameters
     * @return Response as JsonNode
     */
    public JsonNode requestWithParams(String method, String endpoint, Map<String, String> params) throws IOException {
        return request(method, endpoint, params, null, null, true);
    }
    
    /**
     * Make a raw request to get binary content.
     *
     * @param method HTTP method
     * @param endpoint API endpoint path
     * @return Raw response body as byte array
     */
    public byte[] requestRaw(String method, String endpoint) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + endpoint);
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + getAccessToken())
                .header("client-id", clientId);
        
        if (method.equalsIgnoreCase("GET")) {
            requestBuilder.get();
        } else {
            throw new IllegalArgumentException("Only GET is supported for raw requests");
        }
        
        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            if (!response.isSuccessful()) {
                throw new EkaCareAPIError("API error " + response.code() + ": " + 
                        response.body().string());
            }
            
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return new byte[0];
            }
            
            return responseBody.bytes();
        }
    }

    /**
     * Get the Auth module.
     *
     * @return Auth module
     */
    public Auth getAuth() {
        return auth;
    }

    /**
     * Get the Records module.
     *
     * @return Records module
     */
    public Records getRecords() {
        return records;
    }

    /**
     * Get the EkaFileUploader module.
     *
     * @return EkaFileUploader module
     */
    public EkaFileUploader getFiles() {
        return files;
    }

    /**
     * Get the Vitals module.
     *
     * @return Vitals module
     */
    public Vitals getVitals() {
        return vitals;
    }

    /**
     * Get the ABDM Profile module.
     *
     * @return Profile module
     */
    public Profile getAbdmProfile() {
        return abdmProfile;
    }
}
