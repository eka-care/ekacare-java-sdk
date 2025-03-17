# EkaCare Java SDK

This document outlines the structure of the Java SDK for EkaCare API.

## Package Structure

```
care.eka
├── EkaCareClient.java             # Main client class
├── auth
│   └── Auth.java                  # Authentication module
├── abdm
│   └── Profile.java               # ABDM Profile module
├── records
│   └── Records.java               # Records management
├── tools
│   └── EkaFileUploader.java       # File upload utilities
├── vitals
│   └── Vitals.java                # Vitals management
└── utils
    ├── Constants.java             # SDK constants
    └── exceptions
        ├── EkaCareError.java      # Base exception
        ├── EkaCareAPIError.java   # API errors
        ├── EkaCareAuthError.java  # Authentication errors
        ├── EkaCareValidationError.java  # Validation errors
        └── EkaCareResourceNotFoundError.java  # Not found errors
```

## Dependencies

The SDK will use the following dependencies:
- OkHttp for HTTP requests
- Jackson for JSON processing
- AWS SDK for S3 (for multipart uploads)
- SLF4J for logging
