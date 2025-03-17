package care.eka.utils;

/**
 * Constants used throughout the Eka Care SDK.
 */
public class Constants {
    /**
     * API URLs.
     */
    public static final class ApiUrls {
        public static final String PRODUCTION = "https://api.eka.care";
        public static final String SANDBOX = "https://api.dev.eka.care";
    }
    
    /**
     * Document types.
     */
    public static final class DocumentTypes {
        public static final String LAB_REPORT = "lr";
        public static final String PRESCRIPTION = "ps";
        public static final String DISCHARGE_SUMMARY = "ds";
        public static final String VACCINE_CERTIFICATE = "vc";
        public static final String INSURANCE = "in";
        public static final String INVOICE = "iv";
        public static final String SCAN = "sc";
        public static final String OTHER = "ot";
    }
    
    /**
     * Webhook event types.
     */
    public static final class WebhookEvents {
        public static final String APPOINTMENT_CREATED = "appointment.created";
        public static final String APPOINTMENT_UPDATED = "appointment.updated";
        public static final String PRESCRIPTION_CREATED = "prescription.created";
        public static final String PRESCRIPTION_UPDATED = "prescription.updated";
    }
    
    /**
     * ABDM consent status.
     */
    public static final class ConsentStatus {
        public static final String REQUESTED = "requested";
        public static final String GRANTED = "granted";
        public static final String EXPIRED = "expired";
        public static final String DENIED = "denied";
        public static final String REVOKED = "revoked";
    }
    
    /**
     * ABDM health information types.
     */
    public static final class HealthInformationTypes {
        public static final String OP_CONSULTATION = "OPConsultation";
        public static final String PRESCRIPTION = "Prescription";
        public static final String DISCHARGE_SUMMARY = "DischargeSummary";
        public static final String DIAGNOSTIC_REPORT = "DiagnosticReport";
        public static final String IMMUNIZATION_RECORD = "ImmunizationRecord";
        public static final String HEALTH_DOCUMENT_RECORD = "HealthDocumentRecord";
        public static final String WELLNESS_RECORD = "WellnessRecord";
    }
    
    /**
     * Vital LOINC codes.
     */
    public static final class LoincCodes {
        public static final String HEART_RATE = "8867-4";
        public static final String BLOOD_GLUCOSE_RANDOM = "74774-1";
        public static final String BLOOD_GLUCOSE_FASTING = "1558-6";
        public static final String BLOOD_GLUCOSE_AFTER_FOOD = "1521-4";
        public static final String BLOOD_OXYGEN = "2708-6";
        public static final String SYSTOLIC_BP = "8480-6";
        public static final String DIASTOLIC_BP = "8462-4";
        public static final String HEIGHT = "8302-2";
        public static final String BMI = "39156-5";
    }
}
