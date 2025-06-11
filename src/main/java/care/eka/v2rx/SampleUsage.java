package care.eka.v2rx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import care.eka.EkaCareClient;

/**
 * Sample usage of the EkaCare Java SDK.
 */
public class SampleUsage {
    public static void main(String[] args) {
        try {
            // Initialize the client with client ID and secret
            EkaCareClient client = new EkaCareClient("EC_173210496011417", "17a252a7-6ea9-4171-af23-583b5d0b6c77");
            
            // Collect all audio files in a list
            List<String> audioFiles = new ArrayList<>();
            audioFiles.add("/Users/nehajagadeesh/Desktop/v2rx_ulcer_test.wav");
            // audioFiles.add("file_path2");

            // Set your own txn id
            String txnId = "test-neha-11jun25-05";

            // Set the Config for running the V2RX action
            // You can refre to : https://developer.eka.care/api-reference/general-tools/medical/voice/overview
            // Mode : dictation | consultation
            Map<String, Object> extraData = new HashMap<>();
            extraData.put("mode", "dictation");
            //Custom keys , you'll get back the exact same in result API
            extraData.put("uhid", "unique_patient_id"); // Replace with actual patient ID
            extraData.put("hfid", "unique_health_facility_id"); // Replace with actual health facility ID

            // Define the output format for the V2RX action
            // Templates and languages can be customized as per your requirements
            Map<String, Object> outputFormat = new HashMap<>();
            outputFormat.put("input_language", Arrays.asList("en-IN", "hi"));

            // output template ids
            Map<String, Object> templateMap1 = new HashMap<>();
            templateMap1.put("template_id", "nic_template");
            templateMap1.put("language_output", "en-IN");
            // set true if you need codified data (if out supports that)
            templateMap1.put("codification_needed", true);
            
            // Sample second template id
            Map<String, Object> templateMap2 = new HashMap<>();
            templateMap2.put("template_id", "clinical_notes_template");
            templateMap2.put("language_output", "fr");
            
            // Add both templates to the output_template list
            outputFormat.put("output_template", Arrays.asList(templateMap1, templateMap2));
            
            // Keep this the same
            String action = "ekascribe-v2";

            // Example 1: Authentication
            authenticationExample(client);

            // Example 2: Upload files
            // fileUploadExample(client, audioFiles, txnId, action, extraData, outputFormat);

            // Below step should be done after the webhook is received

            // Example 3: Get V2RX status
            getV2RxStatusExample(client, txnId, action);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Authentication example.
     */
    private static void authenticationExample(EkaCareClient client) throws IOException {
        System.out.println("=== Authentication Example ===");

        // Login to get tokens
        JsonNode tokenResponse = client.getAuth().login();
        System.out.println("Access Token: " + tokenResponse.get("access_token").asText());
        System.out.println("Refresh Token: " + tokenResponse.get("refresh_token").asText());

        // Set access token manually
        client.setAccessToken(tokenResponse.get("access_token").asText());

        // Refresh token when needed
        JsonNode refreshedTokens = client.getAuth().refreshToken(tokenResponse.get("refresh_token").asText());
        System.out.println("New Access Token: " + refreshedTokens.get("access_token").asText());
    }

    /**
     * File upload example.
     */
    private static void fileUploadExample(EkaCareClient client, List<String> audioFiles, String txnId, String action, Map<String, Object> extraData, Map<String, Object> outputFormat) throws IOException {
        System.out.println("=== File Upload Example ===");
        System.out.println("extraData" +  extraData.toString());
        System.out.println("opformat" +  outputFormat.toString());
        
        // Upload files
        JsonNode response = client.getV2RX().upload(audioFiles, txnId, action, extraData, outputFormat);
        System.out.println("Json Node: " + response.toPrettyString());

        // Print upload results
        JsonNode outputNode = response.get("output");
        if (outputNode != null && !outputNode.isNull()) {
            for (JsonNode output : outputNode) {
                System.out.println("template id: " + output.get("template_id"));
                System.out.println("template name: " + output.get("name"));
                System.out.println("status: " + output.get("status"));
                System.out.println("errors: " + output.get("errors"));
                System.out.println("warnings: " + output.get("warnings"));
                System.out.println("Value: " + output.get("value"));
            }
        } else {
            System.out.println("No output field found in the response or it's null.");
            System.out.println("Please check the API response structure.");
        }
    }

    /**
     * V2RX status example.
     */
    private static void getV2RxStatusExample(EkaCareClient client, String responseId, String action) throws IOException {
        System.out.println("=== V2RX Fetcher Example ===");

        // Fetch session status
        JsonNode sessionStatus = client.getV2RX().getSessionStatus(responseId, action);
        System.out.println("Session Status: " + sessionStatus.toPrettyString());
    }

   

}