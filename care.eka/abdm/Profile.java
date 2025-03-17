package care.eka.abdm;

import care.eka.EkaCareClient;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Client for interacting with ABDM Profile APIs.
 */
public class Profile {
    private final EkaCareClient client;

    /**
     * Create a new Profile instance.
     *
     * @param client The EkaCare client
     */
    public Profile(EkaCareClient client) {
        this.client = client;
    }

    /**
     * Fetch the ABHA profile of the user.
     *
     * @return ABHA profile details
     * @throws IOException If an error occurs during the request
     * 
     * Example:
     * <pre>
     * EkaCareClient client = new EkaCareClient("your_id", "your_secret", "user_token");
     * JsonNode profile = client.getAbdmProfile().getProfile();
     * System.out.println(profile.get("abha_address").asText());
     * </pre>
     */
    public JsonNode getProfile() throws IOException {
        return client.request("GET", "/abdm/v1/profile");
    }

    /**
     * Update the ABHA profile.
     *
     * @param profileData Profile data to update
     * @return Response indicating success
     * @throws IOException If an error occurs during the request
     * 
     * Note:
     * Fields like first_name, gender, date_of_birth cannot be modified for
     * Aadhaar-based KYC verified profiles.
     * 
     * Example:
     * <pre>
     * EkaCareClient client = new EkaCareClient("your_id", "your_secret", "user_token");
     * Map&lt;String, Object&gt; profileData = new HashMap&lt;&gt;();
     * profileData.put("address", "123 Main St");
     * profileData.put("pincode", "110001");
     * client.getAbdmProfile().updateProfile(profileData);
     * </pre>
     */
    public JsonNode updateProfile(Map<String, Object> profileData) throws IOException {
        return client.request("PATCH", "/abdm/v1/profile", profileData);
    }

    /**
     * Delete the ABHA profile.
     *
     * @return Response indicating success
     * @throws IOException If an error occurs during the request
     * 
     * Warning:
     * This permanently removes the ABHA profile with all associated documents and records.
     * 
     * Example:
     * <pre>
     * EkaCareClient client = new EkaCareClient("your_id", "your_secret", "user_token");
     * client.getAbdmProfile().deleteProfile();
     * </pre>
     */
    public JsonNode deleteProfile() throws IOException {
        return client.request("DELETE", "/abdm/v1/profile");
    }

    /**
     * Get the ABHA card as an image.
     *
     * @return PNG image data of the ABHA card
     * @throws IOException If an error occurs during the request
     * 
     * Example:
     * <pre>
     * EkaCareClient client = new EkaCareClient("your_id", "your_secret", "user_token");
     * byte[] cardImage = client.getAbdmProfile().getAbhaCard();
     * Files.write(Paths.get("abha_card.png"), cardImage);
     * </pre>
     */
    public byte[] getAbhaCard() throws IOException {
        return client.requestRaw("GET", "/abdm/v1/profile/asset/card");
    }

    /**
     * Get the data for ABHA QR code display.
     *
     * @param format Response format (currently only "json" is supported)
     * @return QR code data
     * @throws IOException If an error occurs during the request
     * 
     * Example:
     * <pre>
     * EkaCareClient client = new EkaCareClient("your_id", "your_secret", "user_token");
     * JsonNode qrData = client.getAbdmProfile().getAbhaQrCode("json");
     * // Use qrData to generate a QR code image
     * </pre>
     */
    public JsonNode getAbhaQrCode(String format) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("format", format != null ? format : "json");
        
        return client.requestWithParams("GET", "/abdm/v1/profile/asset/qr", params);
    }

    /**
     * Initiate the KYC process.
     *
     * @param method KYC method ("abha-number" or "aadhaar")
     * @param identifier ABHA number or Aadhaar number
     * @return Response containing transaction ID
     * @throws IOException If an error occurs during the request
     * 
     * Example:
     * <pre>
     * EkaCareClient client = new EkaCareClient("your_id", "your_secret", "user_token");
     * JsonNode response = client.getAbdmProfile().initiateKyc("abha-number", "1234-5678-9012");
     * String txnId = response.get("txn_id").asText();
     * </pre>
     */
    public JsonNode initiateKyc(String method, String identifier) throws IOException {
        Map<String, Object> jsonBody = new HashMap<>();
        jsonBody.put("method", method);
        jsonBody.put("identifier", identifier);
        
        return client.request("POST", "/abdm/v1/profile/kyc/init", jsonBody);
    }

    /**
     * Verify OTP for KYC.
     *
     * @param txnId Transaction ID from initiateKyc
     * @param otp OTP received
     * @return Response indicating success
     * @throws IOException If an error occurs during the request
     * 
     * Example:
     * <pre>
     * EkaCareClient client = new EkaCareClient("your_id", "your_secret", "user_token");
     * client.getAbdmProfile().verifyKycOtp("transaction123", "123456");
     * </pre>
     */
    public JsonNode verifyKycOtp(String txnId, String otp) throws IOException {
        Map<String, Object> jsonBody = new HashMap<>();
        jsonBody.put("txn_id", txnId);
        jsonBody.put("otp", otp);
        
        return client.request("POST", "/abdm/v1/profile/kyc/verify", jsonBody);
    }

    /**
     * Resend OTP for KYC.
     *
     * @param txnId Transaction ID from initiateKyc
     * @return Response containing the new transaction ID
     * @throws IOException If an error occurs during the request
     * 
     * Example:
     * <pre>
     * EkaCareClient client = new EkaCareClient("your_id", "your_secret", "user_token");
     * JsonNode response = client.getAbdmProfile().resendKycOtp("transaction123");
     * String newTxnId = response.get("txn_id").asText();
     * </pre>
     */
    public JsonNode resendKycOtp(String txnId) throws IOException {
        Map<String, Object> jsonBody = new HashMap<>();
        jsonBody.put("txn_id", txnId);
        
        return client.request("POST", "/abdm/v1/profile/kyc/resend", jsonBody);
    }
}
