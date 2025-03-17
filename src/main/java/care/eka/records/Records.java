package care.eka.records;

import care.eka.EkaCareClient;
import care.eka.utils.exceptions.EkaCareValidationError;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for interacting with Eka Care Records API.
 */
public class Records {
    private final EkaCareClient client;
    private final ObjectMapper objectMapper;

    /**
     * Create a new Records instance.
     *
     * @param client The EkaCare client
     */
    public Records(EkaCareClient client) {
        this.client = client;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Obtain authorization for uploading documents.
     *
     * @param batchRequest List of document requests
     * @return Response containing presigned URLs for upload
     * @throws IOException If an error occurs during the request
     * 
     * Example:
     * <pre>
     * EkaCareClient client = new EkaCareClient("your_id", "your_secret");
     * List&lt;Map&lt;String, Object&gt;&gt; batchRequest = new ArrayList&lt;&gt;();
     * Map&lt;String, Object&gt; request = new HashMap&lt;&gt;();
     * request.put("dt", "lr");  // lab report
     * request.put("dd_e", 1614556800);  // document date in epoch format
     * request.put("tg", List.of("covid", "test"));  // tags
     * 
     * List&lt;Map&lt;String, Object&gt;&gt; files = new ArrayList&lt;&gt;();
     * Map&lt;String, Object&gt; file = new HashMap&lt;&gt;();
     * file.put("contentType", "application/pdf");
     * file.put("file_size", 1024000);  // size in bytes
     * files.add(file);
     * 
     * request.put("files", files);
     * batchRequest.add(request);
     * 
     * JsonNode authResponse = client.getRecords().getAuthorization(batchRequest);
     * </pre>
     */
    public JsonNode getAuthorization(List<Map<String, Object>> batchRequest) throws IOException {
        Map<String, Object> jsonBody = new HashMap<>();
        jsonBody.put("batch_request", batchRequest);

        return client.request("POST", "/mr/api/v1/docs", jsonBody);
    }

    /**
     * Upload a document to Eka Care.
     *
     * @param filePath Path to the file to upload
     * @param documentType Type of document (e.g., 'lr' for lab report, 'ps' for prescription)
     * @param documentDate Document date as epoch timestamp
     * @param tags List of tags for the document
     * @param title Title for the document
     * @return Response containing document ID
     * @throws IOException If an error occurs during the request
     * 
     * Example:
     * <pre>
     * EkaCareClient client = new EkaCareClient("your_id", "your_secret");
     * JsonNode response = client.getRecords().uploadDocument(
     *     "/path/to/lab_report.pdf",
     *     "lr",
     *     List.of("covid", "test"),
     *     "COVID-19 Test Report"
     * );
     * String documentId = response.get("document_id").asText();
     * </pre>
     */
    public JsonNode uploadDocument(
            String filePath,
            String documentType,
            Long documentDate,
            List<String> tags,
            String title) throws IOException {

        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new IOException("File not found: " + filePath);
        }

        // Get file size and content type
        long fileSize = file.length();
        String contentType = getContentType(filePath);

        // Prepare batch request
        List<Map<String, Object>> batchRequest = new ArrayList<>();
        Map<String, Object> request = new HashMap<>();
        request.put("dt", documentType);
        if (documentDate != null) {
            request.put("dd_e", documentDate);
        }
        request.put("tg", tags != null ? tags : new ArrayList<>());

        List<Map<String, Object>> files = new ArrayList<>();
        Map<String, Object> fileInfo = new HashMap<>();
        fileInfo.put("contentType", contentType);
        fileInfo.put("file_size", fileSize);
        files.add(fileInfo);
        request.put("files", files);

        if (title != null && !title.isEmpty()) {
            request.put("title", title);
        }

        batchRequest.add(request);

        // Get presigned URL for upload
        JsonNode authResponse = getAuthorization(batchRequest);
        
        JsonNode batchResponse = authResponse.get("batch_response");
        if (batchResponse == null || batchResponse.size() == 0) {
            throw new EkaCareValidationError("No upload URL received");
        }
        
        String documentId = batchResponse.get(0).get("document_id").asText();
        JsonNode forms = batchResponse.get(0).get("forms");
        
        if (forms == null || forms.size() == 0) {
            throw new EkaCareValidationError("No upload forms received");
        }
        
        JsonNode form = forms.get(0);
        String uploadUrl = form.get("url").asText();
        JsonNode fields = form.get("fields");
        
        // Prepare the form data for the upload
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);
        
        // Add all fields from the presigned URL
        fields.fields().forEachRemaining(entry -> {
            multipartBuilder.addFormDataPart(entry.getKey(), entry.getValue().asText());
        });
        
        // Add the file
        multipartBuilder.addFormDataPart(
                "file",
                file.getName(),
                RequestBody.create(file, MediaType.parse(contentType))
        );
        
        // Create the request
        Request request2 = new Request.Builder()
                .url(uploadUrl)
                .post(multipartBuilder.build())
                .build();
        
        // Execute the request using OkHttp client directly
        OkHttpClient httpClient = new OkHttpClient();
        try (Response response = httpClient.newCall(request2).execute()) {
            if (!response.isSuccessful()) {
                throw new EkaCareValidationError("Upload failed: " + 
                        (response.body() != null ? response.body().string() : "Unknown error"));
            }
        }
        
        // Create response with document ID
        ObjectNode result = objectMapper.createObjectNode();
        result.put("document_id", documentId);
        return result;
    }

    /**
     * List documents.
     *
     * @param updatedAfter Filter documents updated after this timestamp
     * @param nextToken Token for pagination
     * @return Response containing list of documents
     * @throws IOException If an error occurs during the request
     * 
     * Example:
     * <pre>
     * EkaCareClient client = new EkaCareClient("your_id", "your_secret");
     * JsonNode documents = client.getRecords().listDocuments(null, null);
     * for (JsonNode doc : documents.get("items")) {
     *     System.out.println(doc.get("record").get("item").get("document_id").asText());
     * }
     * </pre>
     */
    public JsonNode listDocuments(Long updatedAfter, String nextToken) throws IOException {
        Map<String, String> params = new HashMap<>();
        if (updatedAfter != null) {
            params.put("u_at__gt", String.valueOf(updatedAfter));
        }
        if (nextToken != null && !nextToken.isEmpty()) {
            params.put("offset", nextToken);
        }
        
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        
        return client.request("GET", "/mr/api/v1/docs", params, null, headers, true);
    }

    /**
     * Get details of a specific document.
     *
     * @param documentId ID of the document
     * @return Document details
     * @throws IOException If an error occurs during the request
     * 
     * Example:
     * <pre>
     * EkaCareClient client = new EkaCareClient("your_id", "your_secret");
     * JsonNode document = client.getRecords().getDocument("document123");
     * System.out.println(document.get("document_type").asText());
     * </pre>
     */
    public JsonNode getDocument(String documentId) throws IOException {
        return client.request("GET", "/mr/api/v1/docs/" + documentId);
    }

    /**
     * Update a document's metadata.
     *
     * @param documentId ID of the document
     * @param documentType Type of document (e.g., 'lr' for lab report)
     * @param documentDate Document date as epoch timestamp
     * @param tags List of tags for the document
     * @param ndhm Whether to link the document to NDHM
     * @param oid OID to associate with the document
     * @return Empty response on success
     * @throws IOException If an error occurs during the request
     * 
     * Example:
     * <pre>
     * EkaCareClient client = new EkaCareClient("your_id", "your_secret");
     * client.getRecords().updateDocument(
     *     "document123",
     *     "ps",
     *     null,
     *     List.of("medication"),
     *     null,
     *     null
     * );
     * </pre>
     */
    public JsonNode updateDocument(
            String documentId,
            String documentType,
            Long documentDate,
            List<String> tags,
            Boolean ndhm,
            String oid) throws IOException {

        Map<String, Object> data = new HashMap<>();
        if (documentType != null && !documentType.isEmpty()) {
            data.put("dt", documentType);
        }
        if (documentDate != null) {
            data.put("dd_e", documentDate);
        }
        if (tags != null) {
            data.put("tg", tags);
        }
        if (ndhm != null) {
            data.put("ndhm", ndhm);
        }
        if (oid != null && !oid.isEmpty()) {
            data.put("oid", oid);
        }
        
        return client.request("PATCH", "/mr/api/v1/docs/" + documentId, data);
    }

    /**
     * Delete a document.
     *
     * @param documentId ID of the document
     * @return Empty response on success
     * @throws IOException If an error occurs during the request
     * 
     * Example:
     * <pre>
     * EkaCareClient client = new EkaCareClient("your_id", "your_secret");
     * client.getRecords().deleteDocument("document123");
     * </pre>
     */
    public JsonNode deleteDocument(String documentId) throws IOException {
        return client.request("DELETE", "/mr/api/v1/docs/" + documentId);
    }

    /**
     * Retrieve health records in FHIR format.
     *
     * @param identifier Care context ID
     * @param hipId Health Information Provider ID
     * @param healthId ABHA address
     * @return FHIR bundle containing health records
     * @throws IOException If an error occurs during the request
     * 
     * Example:
     * <pre>
     * EkaCareClient client = new EkaCareClient("your_id", "your_secret");
     * JsonNode records = client.getRecords().retrieveHealthRecords(
     *     "care_context_123",
     *     "hip123",
     *     "user@abdm"
     * );
     * </pre>
     */
    public JsonNode retrieveHealthRecords(String identifier, String hipId, String healthId) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("identifier", identifier);
        if (hipId != null && !hipId.isEmpty()) {
            params.put("hip_id", hipId);
        }
        if (healthId != null && !healthId.isEmpty()) {
            params.put("health_id", healthId);
        }
        
        return client.requestWithParams("GET", "/health/api/v1/fhir/retrieve", params);
    }

    /**
     * Determine content type based on file extension.
     *
     * @param filePath Path to the file
     * @return Content type string
     */
    private String getContentType(String filePath) {
        String extension = filePath.substring(filePath.lastIndexOf(".")).toLowerCase();
        
        Map<String, String> contentTypes = new HashMap<>();
        contentTypes.put(".jpg", "image/jpeg");
        contentTypes.put(".jpeg", "image/jpeg");
        contentTypes.put(".png", "image/png");
        contentTypes.put(".pdf", "application/pdf");
        
        return contentTypes.getOrDefault(extension, "application/octet-stream");
    }
}
