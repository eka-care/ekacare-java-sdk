package care.eka.tools;

import care.eka.EkaCareClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * Eka File Uploader SDK
 * A simple and efficient way to handle authenticated file uploads to S3
 */
public class EkaFileUploader {
    private final EkaCareClient client;
    private final ObjectMapper objectMapper;
    
    /**
     * Create a new EkaFileUploader instance.
     *
     * @param client The EkaCare client
     */
    public EkaFileUploader(EkaCareClient client) {
        this.client = client;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Get S3 upload location.
     *
     * @param txnId Transaction ID for grouping uploads
     * @param action Action to perform on the file (default: "default")
     * @param extraData Extra data to send with the upload request
     * @return Upload location information
     * @throws IOException If an error occurs during the request
     */
    public JsonNode getUploadLocation(String txnId, String action, Map<String, Object> extraData) throws IOException {
        try {
            Map<String, String> params = new HashMap<>();
            if (txnId != null && !txnId.isEmpty()) {
                params.put("txn_id", txnId);
                params.put("action", action != null ? action : "default");
            }
            
            return client.request(
                "POST",
                "/v1/file-upload" + (params.isEmpty() ? "" : "?" + buildQueryString(params)),
                extraData != null ? extraData : new HashMap<>());
                
        } catch (Exception e) {
            throw new EkaUploadError("Error getting upload location: " + e.getMessage(), e);
        }
    }
    
    /**
     * Push EkaScribe JSON to S3.
     *
     * @param audioFiles List of audio file paths
     * @param txnId Transaction ID
     * @param extraData Extra data to include in JSON
     * @param uploadInfo Upload information from getUploadLocation
     * @return Upload result containing key, content type, and size
     * @throws IOException If an error occurs during the request
     */
    public JsonNode pushEkascribeJson(List<String> audioFiles, String txnId, 
                                     Map<String, Object> extraData, JsonNode uploadInfo) throws IOException {
        try {
            JsonNode s3PostData = uploadInfo.get("uploadData");
            String folderPath = uploadInfo.get("folderPath").asText();
            
            // Create fields map from s3PostData
            Map<String, String> fields = new HashMap<>();
            s3PostData.get("fields").fields().forEachRemaining(entry -> 
                fields.put(entry.getKey(), entry.getValue().asText()));
            
            // Set the key for JSON file
            fields.put("key", folderPath + txnId + ".json");
            
            // Create data object
            Map<String, Object> data = new HashMap<>();
            data.put("client-id", client.getClientId());
            data.put("transaction-id", txnId);
            
            List<String> audioFileNames = audioFiles.stream()
                .map(path -> new File(path).getName())
                .collect(Collectors.toList());
            
            data.put("audio-file", audioFileNames);
            
            // Add extra data if provided
            if (extraData != null) {
                data.putAll(extraData);
            }
            
            // Convert data to JSON
            String jsonString = objectMapper.writeValueAsString(data);
            byte[] jsonBytes = jsonString.getBytes();
            
            // Create multipart request
            OkHttpClient httpClient = new OkHttpClient();
            MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);
            
            // Add fields
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                multipartBuilder.addFormDataPart(entry.getKey(), entry.getValue());
            }
            
            // Add file
            multipartBuilder.addFormDataPart(
                "file", 
                "data.json",
                    okhttp3.RequestBody.create(MediaType.parse("application/json"), jsonBytes)
            );
            
            // Build request
            Request request = new Request.Builder()
                .url(s3PostData.get("url").asText())
                .post(multipartBuilder.build())
                .build();
            
            // Execute request
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.code() != 204) {
                    throw new EkaUploadError("Upload failed: " + 
                            (response.body() != null ? response.body().string() : "Unknown error"));
                }
            }
            
            // Create response
            ObjectNode result = objectMapper.createObjectNode();
            result.put("key", folderPath + txnId + ".json");
            result.put("contentType", "application/json");
            result.put("size", jsonBytes.length);
            
            return result;
            
        } catch (Exception e) {
            throw new EkaUploadError("Error pushing JSON: " + e.getMessage(), e);
        }
    }
    
    /**
     * Upload files to S3.
     *
     * @param filePaths List of file paths to upload
     * @param txnId Transaction ID for grouping uploads (optional)
     * @param action Action to perform on the file (default: "default")
     * @param extraData Extra data to send with the upload request
     * @return List of upload results containing key, content type, and size
     * @throws IOException If an error occurs during the request
     * 
     * Example:
     * <pre>
     * EkaCareClient client = new EkaCareClient("your_id", "your_secret");
     * List&lt;String&gt; filePaths = List.of("/path/to/file1.jpg", "/path/to/file2.pdf");
     * List&lt;JsonNode&gt; results = client.getFiles().upload(filePaths, "transaction123", "default", null);
     * </pre>
     */
    public List<JsonNode> upload(List<String> filePaths, String txnId, String action, 
                               Map<String, Object> extraData) throws IOException {
        try {
            List<JsonNode> returnList = new ArrayList<>();
            
            // Generate UUID if txnId is not provided
            if (txnId == null || txnId.isEmpty()) {
                txnId = UUID.randomUUID().toString();
            }
            
            // Use default action if not provided
            if (action == null || action.isEmpty()) {
                action = "default";
            }
            
            // Get upload location
            JsonNode uploadInfo = getUploadLocation(txnId, action, extraData);
            
            // Upload each file
            for (String filePath : filePaths) {
                File file = new File(filePath);
                long fileSize = file.length();
                
                if (fileSize > 100 * 1024 * 1024) { // 100MB threshold
                    returnList.add(uploadLargeFile(
                        uploadInfo.get("uploadData"),
                        uploadInfo.get("folderPath").asText(),
                        filePath
                    ));
                } else {
                    returnList.add(uploadSingleFile(
                        uploadInfo.get("uploadData"),
                        uploadInfo.get("folderPath").asText(),
                        filePath
                    ));
                }
            }
            
            // Push EkaScribe JSON if action is ekascribe
            if ("ekascribe".equals(action)) {
                pushEkascribeJson(filePaths, txnId, extraData, uploadInfo);
            }
            
            return returnList;
            
        } catch (Exception e) {
            throw new EkaUploadError("Upload failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Upload a single file to S3.
     *
     * @param uploadData Upload data from getUploadLocation
     * @param folderPath Folder path for the file
     * @param filePath Path to the file to upload
     * @return Upload result containing key, content type, and size
     * @throws IOException If an error occurs during the request
     */
    private JsonNode uploadSingleFile(JsonNode uploadData, String folderPath, String filePath) throws IOException {
        try {
            File file = new File(filePath);
            String fileName = file.getName();
            String contentType = getContentType(filePath);
            
            // Create fields map from uploadData
            Map<String, String> fields = new HashMap<>();
            uploadData.get("fields").fields().forEachRemaining(entry -> 
                fields.put(entry.getKey(), entry.getValue().asText()));
            
            // Set the key for the file
            fields.put("key", folderPath + fileName);
            
            // Create multipart request
            OkHttpClient httpClient = new OkHttpClient();
            MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);
            
            // Add fields
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                multipartBuilder.addFormDataPart(entry.getKey(), entry.getValue());
            }
            
            // Add file
            multipartBuilder.addFormDataPart(
                "file", 
                fileName, okhttp3.RequestBody.create(MediaType.parse(contentType), file)
            );
            
            // Build request
            Request request = new Request.Builder()
                .url(uploadData.get("url").asText())
                .post(multipartBuilder.build())
                .build();
            
            // Execute request
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.code() != 204) {
                    throw new EkaUploadError("Upload failed: " + 
                            (response.body() != null ? response.body().string() : "Unknown error"));
                }
            }
            
            // Create response
            ObjectNode result = objectMapper.createObjectNode();
            result.put("key", folderPath + fileName);
            result.put("contentType", contentType);
            result.put("size", file.length());
            
            return result;
            
        } catch (Exception e) {
            throw new EkaUploadError("Error uploading file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Upload a large file to S3 using multipart upload.
     *
     * @param uploadData Upload data from getUploadLocation
     * @param folderPath Folder path for the file
     * @param filePath Path to the file to upload
     * @param partSize Size of each part in bytes (default: 10MB)
     * @return Upload result containing key, content type, and size
     * @throws IOException If an error occurs during the request
     */
    private JsonNode uploadLargeFile(JsonNode uploadData, String folderPath, String filePath) throws IOException {
        try {
            File file = new File(filePath);
            String fileName = file.getName();
            String contentType = getContentType(filePath);
            String key = folderPath + fileName;
            long fileSize = file.length();
            int partSize = 10 * 1024 * 1024; // 10MB
            
            // Create S3 client
            S3Client s3Client = S3Client.builder().build();
            
            // Start multipart upload
            CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
                .bucket(uploadData.get("fields").get("bucket").asText())
                .key(key)
                .contentType(contentType)
                .build();
            
            CreateMultipartUploadResponse createResponse = s3Client.createMultipartUpload(createRequest);
            String uploadId = createResponse.uploadId();
            
            try {
                // Calculate part count
                long partCount = (fileSize + partSize - 1) / partSize;
                List<CompletedPart> completedParts = new ArrayList<>();
                
                // Upload parts
                byte[] fileContent = Files.readAllBytes(file.toPath());
                for (int i = 0; i < partCount; i++) {
                    int partNumber = i + 1;
                    int startByte = i * partSize;
                    int endByte = (int) Math.min(startByte + partSize, fileSize);
                    int partLength = endByte - startByte;
                    
                    byte[] partBytes = Arrays.copyOfRange(fileContent, startByte, endByte);
                    
                    UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                        .bucket(uploadData.get("fields").get("bucket").asText())
                        .key(key)
                        .uploadId(uploadId)
                        .partNumber(partNumber)
                        .build();
                    
                    UploadPartResponse uploadPartResponse = s3Client.uploadPart(
                        uploadPartRequest, 
                        RequestBody.fromBytes(partBytes)
                    );
                    
                    CompletedPart part = CompletedPart.builder()
                        .partNumber(partNumber)
                        .eTag(uploadPartResponse.eTag())
                        .build();
                    
                    completedParts.add(part);
                }
                
                // Complete multipart upload
                CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder()
                    .parts(completedParts)
                    .build();
                
                CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                    .bucket(uploadData.get("fields").get("bucket").asText())
                    .key(key)
                    .uploadId(uploadId)
                    .multipartUpload(completedMultipartUpload)
                    .build();
                
                s3Client.completeMultipartUpload(completeRequest);
                
                // Create response
                ObjectNode result = objectMapper.createObjectNode();
                result.put("key", key);
                result.put("contentType", contentType);
                result.put("size", fileSize);
                
                return result;
                
            } catch (Exception e) {
                // Abort multipart upload on failure
                AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
                    .bucket(uploadData.get("fields").get("bucket").asText())
                    .key(key)
                    .uploadId(uploadId)
                    .build();
                
                s3Client.abortMultipartUpload(abortRequest);
                throw new EkaUploadError("Multipart upload failed: " + e.getMessage(), e);
            }
            
        } catch (Exception e) {
            throw new EkaUploadError("Error uploading large file: " + e.getMessage(), e);
        }
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
        contentTypes.put(".mp3", "audio/mpeg");
        contentTypes.put(".wav", "audio/wav");
        
        return contentTypes.getOrDefault(extension, "application/octet-stream");
    }
    
    /**
     * Build a query string from a map of parameters.
     *
     * @param params Map of query parameters
     * @return URL-encoded query string
     */
    private String buildQueryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (first) {
                first = false;
            } else {
                sb.append("&");
            }
            
            sb.append(encodeUrlParam(entry.getKey()));
            sb.append("=");
            sb.append(encodeUrlParam(entry.getValue()));
        }
        
        return sb.toString();
    }
    
    /**
     * URL encode a parameter.
     *
     * @param param Parameter to encode
     * @return URL-encoded parameter
     */
    private String encodeUrlParam(String param) {
        try {
            return java.net.URLEncoder.encode(param, "UTF-8");
        } catch (Exception e) {
            return param; // Fallback to unencoded param
        }
    }
}
