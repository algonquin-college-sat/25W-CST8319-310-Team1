# Label Xtract

**Team ENIGMatic**  
*(Algonquin College – CST8319 Software Development Project)*

---

## Table of Contents
1. [Project Overview](#project-overview)  
2. [Features](#features)  
3. [Project Requirements Summary](#project-requirements-summary)  
4. [Project Structure](#project-structure)  
5. [Installation & Setup](#installation--setup)  
6. [Usage](#usage)  
7. [Sample Output](#sample-output)  
8. [Testing](#testing)  
9. [License & Credits](#license--credits)  
10. [References](#references)

---

## Project Overview
**Label Xtract** is an Android application designed to read shipping labels using the device’s rear-facing camera and automatically extract key information (addresses, postal codes, barcodes, and more) via Optical Character Recognition (OCR). The data is then displayed in JSON format. This project was developed by **Team ENIGMatic** to address inefficiencies in handling non-conformant packages at Canada Post, where a small percentage of parcel labels fail to be read by traditional barcode scanners.

**Core Goals:**
- Provide depot clerks with a tool to quickly extract all relevant shipping label info.
- Offer an intuitive and automated way to handle partially damaged or otherwise unscannable labels.
- Produce a structured JSON output containing the recognized fields.

---

## Features
- **Continuous Camera Feed** using the Android **CameraX** library to capture frames in real time.
- **Automatic Text Recognition** using [Google ML Kit’s TextRecognition](https://developers.google.com/ml-kit/vision/text-recognition).
- **Barcode Scanning** using [Google ML Kit’s BarcodeScanning](https://developers.google.com/ml-kit/vision/barcode-scanning).
- **Postal Code Detection** via regex pattern matching for Canadian postal codes.
- **JSON Output** that displays recognized text, barcodes, addresses, and other relevant fields.
- **Document Scanner Integration** (via GmsDocumentScanning) to capture higher-fidelity images if needed.

---

## Project Requirements Summary
This application aligns with the core requirements stated in the **Project Requirements Specification (PRS)**:

1. **Functional Requirements**  
   - OCR scanning of shipping labels  
   - Barcode detection and retrieval of the barcode text  
   - JSON output for recognized fields  
   - Handling of partially damaged labels  
   - Intuitive UI and user flow

2. **Non-Functional Requirements**  
   - Compatible with Android 11 or higher  
   - Sub-second processing time for scans  
   - Developed using Agile methodology  
   - Hosted on a public GitHub repository under a permissive license

---

## Project Structure
.
└── algonquin.cst8319.enigmatic
    ├── MainActivity.kt                # Entry point of the app
    ├── MainActivityViewModel.kt      # ViewModel for LiveData (UI state)
    ├── ImageAnalyzer.kt              # Core class using ML Kit to detect text/barcodes
    ├── ImageAnalyzerListener.kt      # Listener interface for analyzer callbacks
    ├── LabelJSON.kt                  # Data class for storing label fields (Kotlinx Serialization)
    ├── PersistentBottomSheet.kt      # (Optional) Implementation for a Bottom Sheet Fragment
    ├── data
    │   └── FieldExtractor.kt         # Helper class for extracting fields from OCR text blocks
    ├── xml layouts, resources, etc.
    └── ...


- **`ImageAnalyzer`**  
  Handles camera frames fed into ML Kit’s Text and Barcode Scanners, pausing scanning once a postal code is detected, and processing the results for JSON output.

- **`FieldExtractor`**  
  Extracts text blocks from OCR output (addresses, postal codes, reference numbers, etc.).

- **`LabelJSON`**  
  A `@Serializable` Kotlin class defining the structure of the JSON output (product type, addresses, postal code, barcode, etc.).

- **`MainActivity`**  
  Sets up camera preview (using CameraX) and binds the `ImageAnalyzer`. Launches GmsDocumentScanning if needed and displays final output in a bottom sheet.

- **`MainActivityViewModel`**  
  Holds UI state in `LiveData`, controlling preview/results container visibility and text.

---

## Installation & Setup

1. **Clone the Repository**
   ```bash
   git clone https://github.com/algonquin-college-sat/25W-CST8319-310-Team1.git




