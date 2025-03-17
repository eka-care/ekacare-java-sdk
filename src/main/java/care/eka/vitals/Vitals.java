package care.eka.vitals;

import care.eka.EkaCareClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for interacting with Eka Care Vitals API.
 */
public class Vitals {
    private final EkaCareClient client;
    private final ObjectMapper objectMapper;

    /**
     * Create a new Vitals instance.
     *
     * @param client The EkaCare client
     */
    public Vitals(EkaCareClient client) {
        this.client = client;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Update patient vitals.
     *
     * @param txnId Transaction ID for the vitals update
     * @param vitalsData List of vital records to update
     * @return Response indicating success or failure
     * @throws IOException If an error occurs during the request
     * 
     * Example:
     * <pre>
     * EkaCareClient client = new EkaCareClient("your_id", "your_secret");
     * JsonNode heartRate = client.getVitals().createHeartRateVital(75, "2023-01-01T12:00:00");
     * JsonNode bloodGlucose = client.getVitals().createBloodGlucoseVital(120, "2023-01-01T08:00:00", "fasting");
     * List&lt;JsonNode&gt; vitals = List.of(heartRate, bloodGlucose);
     * JsonNode response = client.getVitals().updateVitals("txn123", vitals);
     * </pre>
     */
    public JsonNode updateVitals(String txnId, List<JsonNode> vitalsData) throws IOException {
        Map<String, Object> jsonBody = new HashMap<>();
        
        // Convert List<JsonNode> to a format expected by the API
        jsonBody.put("vitals", vitalsData);
        
        return client.request("PUT", "/api/v1/vitals/" + txnId, jsonBody);
    }

    /**
     * Create a heart rate vital record.
     *
     * @param value Heart rate value
     * @param measuredAt Timestamp of measurement (ISO format)
     * @param unit Unit of measurement
     * @return Vital record ready for submission
     * 
     * Example:
     * <pre>
     * EkaCareClient client = new EkaCareClient("your_id", "your_secret");
     * JsonNode heartRate = client.getVitals().createHeartRateVital(75, "2023-01-01T12:00:00");
     * </pre>
     */
    public JsonNode createHeartRateVital(float value, String measuredAt, String unit) {
        if (unit == null || unit.isEmpty()) {
            unit = "{Counts}/min";
        }
        
        ObjectNode vital = objectMapper.createObjectNode();
        vital.put("vital_display_name", "heart_rate");
        vital.put("vital_loinc_code", "8867-4");
        
        ObjectNode vitalValue = vital.putObject("vital_value");
        vitalValue.put("vital_numeric_value", value);
        vitalValue.putNull("vital_boolean_value");
        vitalValue.putNull("vital_string_value");
        
        vital.put("unit_display_name", unit);
        vital.put("unit_ucum_code", "<ucum code>");
        
        ObjectNode measuredAtObj = vital.putObject("measured_at");
        measuredAtObj.put("DateTime", measuredAt);
        measuredAtObj.putNull("instant");
        
        ObjectNode period = measuredAtObj.putObject("period");
        period.putNull("startDateTime");
        period.putNull("endDateTime");
        
        return vital;
    }
    
    /**
     * Create a heart rate vital record with default unit.
     *
     * @param value Heart rate value
     * @param measuredAt Timestamp of measurement (ISO format)
     * @return Vital record ready for submission
     */
    public JsonNode createHeartRateVital(float value, String measuredAt) {
        return createHeartRateVital(value, measuredAt, "{Counts}/min");
    }

    /**
     * Create a blood glucose vital record.
     *
     * @param value Blood glucose value
     * @param measuredAt Timestamp of measurement (ISO format)
     * @param glucoseType Type of glucose measurement (random, fasting, after_food)
     * @param unit Unit of measurement
     * @return Vital record ready for submission
     * 
     * Example:
     * <pre>
     * EkaCareClient client = new EkaCareClient("your_id", "your_secret");
     * JsonNode glucose = client.getVitals().createBloodGlucoseVital(120, "2023-01-01T08:00:00", "fasting", "mg/dL");
     * </pre>
     */
    public JsonNode createBloodGlucoseVital(float value, String measuredAt, String glucoseType, String unit) {
        if (unit == null || unit.isEmpty()) {
            unit = "mg/dL";
        }
        
        if (glucoseType == null || glucoseType.isEmpty()) {
            glucoseType = "random";
        }
        
        Map<String, Map<String, String>> vitalTypes = new HashMap<>();
        vitalTypes.put("random", Map.of(
            "name", "random_blood_glucose",
            "loinc_code", "74774-1"
        ));
        vitalTypes.put("fasting", Map.of(
            "name", "fasting_blood_glucose",
            "loinc_code", "1558-6"
        ));
        vitalTypes.put("after_food", Map.of(
            "name", "after_food_blood_glucose",
            "loinc_code", "1521-4"
        ));
        
        Map<String, String> vitalType = vitalTypes.getOrDefault(glucoseType, vitalTypes.get("random"));
        
        ObjectNode vital = objectMapper.createObjectNode();
        vital.put("vital_display_name", vitalType.get("name"));
        vital.put("vital_loinc_code", vitalType.get("loinc_code"));
        
        ObjectNode vitalValue = vital.putObject("vital_value");
        vitalValue.put("vital_numeric_value", value);
        vitalValue.putNull("vital_boolean_value");
        vitalValue.putNull("vital_string_value");
        
        vital.put("unit_display_name", unit);
        vital.put("unit_ucum_code", unit);
        
        ObjectNode measuredAtObj = vital.putObject("measured_at");
        measuredAtObj.put("DateTime", measuredAt);
        measuredAtObj.putNull("instant");
        
        ObjectNode period = measuredAtObj.putObject("period");
        period.putNull("startDateTime");
        period.putNull("endDateTime");
        
        return vital;
    }
    
    /**
     * Create a blood glucose vital record with default unit.
     *
     * @param value Blood glucose value
     * @param measuredAt Timestamp of measurement (ISO format)
     * @param glucoseType Type of glucose measurement (random, fasting, after_food)
     * @return Vital record ready for submission
     */
    public JsonNode createBloodGlucoseVital(float value, String measuredAt, String glucoseType) {
        return createBloodGlucoseVital(value, measuredAt, glucoseType, "mg/dL");
    }

    /**
     * Create a blood oxygen (SpO2) vital record.
     *
     * @param value Blood oxygen saturation value
     * @param measuredAt Timestamp of measurement (ISO format)
     * @param unit Unit of measurement
     * @return Vital record ready for submission
     * 
     * Example:
     * <pre>
     * EkaCareClient client = new EkaCareClient("your_id", "your_secret");
     * JsonNode oxygen = client.getVitals().createBloodOxygenVital(98, "2023-01-01T12:30:00");
     * </pre>
     */
    public JsonNode createBloodOxygenVital(float value, String measuredAt, String unit) {
        if (unit == null || unit.isEmpty()) {
            unit = "%";
        }
        
        ObjectNode vital = objectMapper.createObjectNode();
        vital.put("vital_display_name", "blood_oxygen");
        vital.put("vital_loinc_code", "2708-6");
        
        ObjectNode vitalValue = vital.putObject("vital_value");
        vitalValue.put("vital_numeric_value", value);
        vitalValue.putNull("vital_boolean_value");
        vitalValue.putNull("vital_string_value");
        
        vital.put("unit_display_name", unit);
        vital.put("unit_ucum_code", unit);
        
        ObjectNode measuredAtObj = vital.putObject("measured_at");
        measuredAtObj.put("DateTime", measuredAt);
        measuredAtObj.putNull("instant");
        
        ObjectNode period = measuredAtObj.putObject("period");
        period.putNull("startDateTime");
        period.putNull("endDateTime");
        
        return vital;
    }
    
    /**
     * Create a blood oxygen vital record with default unit.
     *
     * @param value Blood oxygen saturation value
     * @param measuredAt Timestamp of measurement (ISO format)
     * @return Vital record ready for submission
     */
    public JsonNode createBloodOxygenVital(float value, String measuredAt) {
        return createBloodOxygenVital(value, measuredAt, "%");
    }

    /**
     * Create blood pressure vital records (systolic and diastolic).
     *
     * @param systolic Systolic blood pressure value
     * @param diastolic Diastolic blood pressure value
     * @param measuredAt Timestamp of measurement (ISO format)
     * @param unit Unit of measurement
     * @return List of vital records ready for submission
     * 
     * Example:
     * <pre>
     * EkaCareClient client = new EkaCareClient("your_id", "your_secret");
     * List&lt;JsonNode&gt; bpVitals = client.getVitals().createBloodPressureVital(120, 80, "2023-01-01T14:45:00");
     * </pre>
     */
    public List<JsonNode> createBloodPressureVital(float systolic, float diastolic, String measuredAt, String unit) {
        if (unit == null || unit.isEmpty()) {
            unit = "mm Hg";
        }
        
        ObjectNode systolicVital = objectMapper.createObjectNode();
        systolicVital.put("vital_display_name", "systolic_bp");
        systolicVital.put("vital_loinc_code", "8480-6");
        
        ObjectNode systolicValue = systolicVital.putObject("vital_value");
        systolicValue.put("vital_numeric_value", systolic);
        systolicValue.putNull("vital_boolean_value");
        systolicValue.putNull("vital_string_value");
        
        systolicVital.put("unit_display_name", unit);
        systolicVital.put("unit_ucum_code", unit);
        
        ObjectNode systolicMeasuredAt = systolicVital.putObject("measured_at");
        systolicMeasuredAt.put("DateTime", measuredAt);
        systolicMeasuredAt.putNull("instant");
        
        ObjectNode systolicPeriod = systolicMeasuredAt.putObject("period");
        systolicPeriod.putNull("startDateTime");
        systolicPeriod.putNull("endDateTime");
        
        ObjectNode diastolicVital = objectMapper.createObjectNode();
        diastolicVital.put("vital_display_name", "diastolic_bp");
        diastolicVital.put("vital_loinc_code", "8462-4");
        
        ObjectNode diastolicValue = diastolicVital.putObject("vital_value");
        diastolicValue.put("vital_numeric_value", diastolic);
        diastolicValue.putNull("vital_boolean_value");
        diastolicValue.putNull("vital_string_value");
        
        diastolicVital.put("unit_display_name", unit);
        diastolicVital.put("unit_ucum_code", unit);
        
        ObjectNode diastolicMeasuredAt = diastolicVital.putObject("measured_at");
        diastolicMeasuredAt.put("DateTime", measuredAt);
        diastolicMeasuredAt.putNull("instant");
        
        ObjectNode diastolicPeriod = diastolicMeasuredAt.putObject("period");
        diastolicPeriod.putNull("startDateTime");
        diastolicPeriod.putNull("endDateTime");
        
        return List.of(systolicVital, diastolicVital);
    }
    
    /**
     * Create blood pressure vital records with default unit.
     *
     * @param systolic Systolic blood pressure value
     * @param diastolic Diastolic blood pressure value
     * @param measuredAt Timestamp of measurement (ISO format)
     * @return List of vital records ready for submission
     */
    public List<JsonNode> createBloodPressureVital(float systolic, float diastolic, String measuredAt) {
        return createBloodPressureVital(systolic, diastolic, measuredAt, "mm Hg");
    }
}
