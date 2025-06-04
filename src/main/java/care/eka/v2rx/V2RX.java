package care.eka.v2rx;

import care.eka.EkaCareClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom exception for EkaScribe upload related errors
 */
class EkaScribeError extends RuntimeException {
    public EkaScribeError(String message) {
        super(message);
    }
    
    public EkaScribeError(String message, Throwable cause) {
        super(message, cause);
    }
}

public class V2RX {
    private final EkaCareClient client;
    private final ObjectMapper objectMapper;

    public V2RX(EkaCareClient client) {
        this.client = client;
        this.objectMapper = new ObjectMapper();
    }

    private String getS3BucketName(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host != null && host.contains("s3")) {
                String[] parts = host.split("\\.");
                if (parts.length > 0) {
                    return parts[0];
                }
            }
        } catch (URISyntaxException e) {
            // Handle parsing error
            return null;
        }
        return null;
    }

    /**
     * Upload files for V2RX processing
     *
     * @param filePaths List of file paths to upload
     * @param txnId Transaction ID (optional)
     * @param action Action type (default: "default")
     * @param extraData Additional data map
     * @param outputFormat Output format configuration map
     * @return JsonNode containing upload result
     * @throws IOException If upload fails
     * @throws EkaScribeError If EkaScribe specific processing fails
     */

    public JsonNode upload(List<String> filePaths, String txnId, String action, 
                          Map<String, Object> extraData, Map<String, Object> outputFormat) 
                          throws IOException, EkaScribeError {
        
        // Set default values
        if (action == null) action = "default";
        if (extraData == null) extraData = new HashMap<>();
        if (outputFormat == null) outputFormat = new HashMap<>();

        // Upload files using the file uploader
        List<JsonNode> uploadResults = client.getFiles().upload(filePaths, txnId, action, extraData, outputFormat);
        JsonNode fileUploadResult = objectMapper.createArrayNode().addAll(uploadResults);
        
        if ("ekascribe-v2".equals(action)) {
            try {
                // Get upload info from the file uploader
                JsonNode uploadInfo = client.getFiles().getLastUploadInfo();
                
                String uploadUrl = uploadInfo.get("uploadData").get("url").asText();
                String bucketName = getS3BucketName(uploadUrl);
                String folderPath = uploadInfo.get("folderPath").asText();
                
                String s3Url = String.format("s3://%s/%s", bucketName, folderPath);
                
                // Create S3 file paths
                List<String> s3FilePaths = new ArrayList<>();
                for (String filePath : filePaths) {
                    Path path = Paths.get(filePath);
                    String fileName = path.getFileName().toString();
                    s3FilePaths.add(s3Url + fileName);
                }
                
                // Create payload
                ObjectNode payload = objectMapper.createObjectNode();
                payload.put("s3_url", s3Url);
                payload.put("batch_s3_url", s3Url);
                payload.set("additional_data", objectMapper.valueToTree(extraData));
                payload.put("mode", extraData.get("mode") != null ? extraData.get("mode").toString() : null);
                payload.put("input_language", outputFormat.get("input_language") != null ? 
                           outputFormat.get("input_language").toString() : null);
                payload.put("speciality", "speciality");
                payload.put("Section", "section");
                if (outputFormat.get("output_template") != null) {
                    payload.set("output_format_template", objectMapper.valueToTree(outputFormat.get("output_template")));
                }
                payload.put("transfer", "non-vaded");
                payload.set("client_generated_files", objectMapper.valueToTree(s3FilePaths));
                
                // Make POST request to initialize transaction
                String endpoint = "/voice/api/v2/transaction/init/" + txnId;
                Map<String, Object> jsonBody = new HashMap<>();
                jsonBody = objectMapper.convertValue(payload, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>(){});
                String method = "POST";
                JsonNode response = client.request(method, endpoint, null, jsonBody, null, true);
                // Check if response status is not 201 (Created)
                // Check if response status is 201 (Created), if not, throw error
                if (!response.get("status").asText().equals("success")) {
                    throw new EkaScribeError("Upload initialisation failed: " + response.toString());
                }

                
            } catch (Exception e) {
                throw new EkaScribeError("Upload failed: " + e.getMessage(), e);
            }
        }
        
        return fileUploadResult;
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
    public JsonNode getSessionStatus(String sessionId, String action) throws IOException {
        if (sessionId == null || sessionId.isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
        // set default value
        if(action == null) action = "ekascribe";

        String endpoint = "/voice-record/api/status/" + sessionId;

        // Check if action is "ekascribe-v2" and modify endpoint accordingly
        if ("ekascribe-v2".equals(action)) {
            endpoint = "/voice/api/v3/status/" + sessionId;
        }

        // Make a GET request to the endpoint
        return client.request("GET", endpoint);
    }

}
