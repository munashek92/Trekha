package com.app.trekha.user.model;

public enum KycDocumentType {
    NATIONAL_ID,        // Passenger, Driver, Owner
    PASSPORT,           // Passenger, Driver, Owner
    SELFIE,             // Passenger, Driver, Owner
    DRIVER_LICENSE,     // Driver
    VEHICLE_PHOTO,      // Driver (for their vehicle), Owner (for their vehicle)
    VEHICLE_REG_BOOK,   // Driver, Owner
    PROOF_OF_ADDRESS,   // Owner
    CR14_COMPANY_DOCS   // Owner (if company)
}
