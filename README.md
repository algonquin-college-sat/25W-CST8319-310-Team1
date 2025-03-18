# Label Xtract

**Team ENIGMatic**  
*(Algonquin College – CST8319 Software Development Project)*

---

## Table of Contents
1. [Quickstart](#quickstart)  
2. [Project Overview](#project-overview)  
3. [Features](#features)  
4. [Project Requirements Summary](#project-requirements-summary)  
5. [Project Structure](#project-structure)  
6. [Technologies Used](#technologies-used)  
7. [Installation & Setup](#installation--setup)  
8. [Usage](#usage)  
9. [Sample Output](#sample-output)  
10. [License](#license)  

---

## Quickstart
1. Clone the repo
2. Open in Android Studio
3. Run on an Android 11+ device
4. Scan a shipping label

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
```plaintext
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
```

### Key Classes

---

#### ImageAnalyzer
- Manages the camera frames fed into ML Kit’s Text and Barcode Scanners.
- Pauses scanning once a postal code is detected.
- Uses `recognizer.process(...)` for OCR and `barcodeScanner.process(...)` for barcodes.

#### FieldExtractor
- Extracts text blocks from the recognized OCR result.
- Identifies addresses, postal codes, and additional shipping label data.

#### LabelJSON
- A `@Serializable` Kotlin class that defines the structure of the JSON output.

#### MainActivity
- Sets up the CameraX preview and binds an instance of `ImageAnalyzer`.
- Listens for callbacks indicating text/barcode detection or final JSON generation.
- Launches the GMS Document Scanner for enhanced image capture.
- Displays output to the user in a bottom sheet.

#### MainActivityViewModel
- Encapsulates UI state using `LiveData` (e.g., controlling the preview or results container visibility).

---

## Technologies Used

Below is a comprehensive list of the core technologies and libraries utilized by Label Xtract:

- **![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)**  
  Primary programming language for implementing the application logic, leveraging modern Kotlin features such as data classes, and extension functions.

- **Gradle**  
  Build automation tool used to manage project configurations, dependencies, and tasks. The Gradle wrapper ensures consistent build environments across different systems.

- **Android Studio**  
  Recommended integrated development environment (IDE) for Android app development, offering code editing, debugging, and built-in Gradle support.

- **Android SDK / AndroidX Libraries**  
  - **CameraX**: Provides a simple and consistent API to access the device camera.  
  - **Lifecycle** / **ViewModel**: Architecture components managing UI-related data in a lifecycle-conscious way.  
  - **Activity KTX** / **Fragment KTX** / **Core KTX**: Kotlin extension libraries that simplify common Android tasks.  
  - **ConstraintLayout**: Flexible layout manager for UI design.  
  - **Material Components**: Implements Material Design widgets and behaviors (e.g., Bottom Sheets).

- **Google ML Kit**  
  - **ML Kit Text Recognition**: Automatically detects and extracts text from images.  
  - **ML Kit Barcode Scanning**: Identifies and reads barcodes (including partially damaged ones).  
  - **Play Services Document Scanner**: Enables higher-fidelity image capture flows via `GmsDocumentScanning`.

- **Kotlinx Serialization (Json)**  
  Used for converting recognized label data into structured JSON output. Annotated classes (e.g., `@Serializable`) allow easy parsing and generation of JSON strings.

- **Material Design Components**  
  Offers ready-made UI components (buttons, text fields, bottom sheets, etc.) and ensures a modern, consistent user experience aligned with Google’s Material Design guidelines.

These technologies work together to deliver a user-friendly experience. All build dependencies are specified in the app’s `build.gradle` file, making it straightforward to manage and update the libraries as needed.

---

## Installation & Setup

1. **Clone the Repository**
   ```bash
   git clone https://github.com/algonquin-college-sat/25W-CST8319-310-Team1.git

2. **Open in Android Studio**
   - Open **Android Studio**.
   - Select **Open an Existing Project**.
   - Navigate to the cloned folder and open it.

3. **Sync and Install Dependencies**
   - Allow **Gradle** to sync automatically, or go to **File > Sync Project with Gradle Files**.
   - Required **ML Kit** libraries and **AndroidX** components will be installed automatically via Gradle.

4. **Run on a Device or Emulator**
   - Connect your Android device via USB with **Developer Mode** enabled.
   - Press the **Run** button in Android Studio to build and deploy the app.

5. **Permissions**
   - On the first launch, the app will request **Camera** permissions. Grant these permissions so the application can access the camera for live scanning.

---

## Usage

### Launch the App
- Open **Label Xtract** on your Android device.  
- The camera feed will be active, continuously scanning for shipping labels.  
- Ensure the label is well lit and within view.

### Label Detection
- The OCR automatically searches for textual patterns (especially a Canadian postal code) to identify a shipping label.  
- Once a label is detected, the scanning process pauses to avoid duplicates.

### Scanning the Document
- The app launches the **GmsDocumentScanning** flow for higher-fidelity images.  
- A short pause or “debounce” is built in to allow text recognitions and barcode scanning process to complete.

### Viewing the Results
- After detection, the recognized text blocks are processed into a `LabelJSON`.
- A **bottom sheet** appears, displaying the JSON output (e.g., postal code, addresses, product details, barcodes, etc.).
- **An image of the captured label** is also displayed on the screen above the bottom sheet for visual reference.

### Close or Proceed
- If the user taps **Close**, the bottom sheet collapses, and the camera feed resumes.  
- The user can then repeat the process for new packages.

---

## Sample Output

Below is a sample JSON output captured by the application:

```json
{
  "productType": "Priority",
  "toAddress": "Julie Tester, 4811 Churchill Place, Laval, QC, H7W 4H4",
  "destPostalCode": "H7W 4H4",
  "trackPin": "7023 2102 3528 2700",
  "barCode": "PHWH7447023210235282270000200",
  "fromAddress": "IIQA CUST DO NOT USE, 2 SAINTE-CATHERINE ST East, MONTREAL QC H2X",
  "productDimension": "33x33x33cm",
  "productWeight": "7.190kg",
  "productInstruction": "MANIFESTREQ",
  "reference": "QC-DJ002"
}
```

---

## Contribution Guidelines

We welcome contributions to improve **Label Xtract**! Whether you find a bug, have an idea for a new feature, or want to enhance the existing code, here’s how you can get involved:

- **Reporting Issues or Suggestions**:  
  If you encounter any problems using **Label Xtract** or have recommendations for enhancements, please open an issue on the project's issue tracker (GitHub Issues). Provide details about the problem or idea, including steps to reproduce bugs or reasoning behind feature requests. This helps maintainers and contributors understand and prioritize the work.

- **Contributing Code**:  
  We appreciate pull requests from the community. To contribute code or documentation:

  1. **Fork the Repository**:  
     Click the **"Fork"** button on GitHub to create your own copy of the **Label Xtract** repository.
  2. **Create a Branch**:  
     In your forked repository, create a new branch for your fix or feature. Use a descriptive branch name, for example: `fix/image-loading-bug` or `feature/add-json-export`.
  3. **Make Changes**:  
     Develop your feature or bug fix on that branch. Follow the coding style of the project (consistent naming, formatting, and adequate comments).
  4. **Test Your Changes**:  
     Ensure that the project still builds and all tests pass after your changes.
  5. **Commit and Push**:  
     Commit your changes with a clear and concise commit message. Push the branch to your GitHub fork.
  6. **Open a Pull Request**:  
     Go to the original repository and open a PR from your forked branch. In the pull request description, clearly explain the changes you’ve made and **why** (link to the issue it fixes, if applicable). Include any relevant screenshots or logs if the changes affect the UI or performance.
  7. **Code Review**:  
     The ENIGMatic team will review your pull request as soon as we can. Please be open to feedback or requests for adjustments. We aim to collaborate to maintain code quality and project vision.

- **Code Style and Guidelines**:  
  Try to adhere to any style guidelines used in the project. Write clear comments for any functions you introduce. This makes it easier for others to understand and maintain the code in the future.

By following these guidelines, you help us ensure that the project remains stable and useful. All contributors will be acknowledged for their work. **Thank you** for helping improve **Label Xtract**!

---

## Contributors

The **Enigmatic OCR** project was developed by the following team members as part of a software development course project:

| **Name**       | **Role**            | **GitHub**                                             |
|----------------|---------------------|-------------------------------------------------------|
| Ekene Ndubueze  | Developer, QA, Tester     | [GitHub Profile](https://github.com/Ozi-Tech)     |
| Naomi Bell     | Developer, UI/UX, Tester         | [GitHub Profile](https://github.com/bell0418)     |
| Ian Philips  | Developer, Business Analyst, Tester         | [GitHub Profile](https://github.com/phil0440)     |
| Gulnur Ospanova  | Developer, Architect/Researcher, Tester      | [GitHub Profile](https://github.com/gulnurkaztai)     |
| Milan Neven  | Developer, Architect, Tester | [GitHub Profile](https://github.com/MilanNeven)     |

---

## License

This project is licensed under the [Apache License 2.0](LICENSE).  
For detailed terms and conditions, please see the `LICENSE` file in the root directory of this repository.



