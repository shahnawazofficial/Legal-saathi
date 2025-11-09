# 🇮🇳 Legal Saathi: Your AI-Powered Legal Companion

![Legal Saathi Logo Placeholder](https://via.placeholder.com/600x200?text=Legal+Saathi+-+AI+Powered+Legal+Assistance)

**Legal Saathi** (Legal Companion) is a modern Android application designed to democratize legal knowledge and government information for Indian citizens. Leveraging the power of Generative AI, the app provides instant, accurate, and actionable guidance right on your mobile device.

---

## 💡 About the Project

In a country as vast and diverse as India, navigating legal matters and government schemes can be complex. Legal Saathi aims to bridge this information gap by offering a simple, conversational interface for:

* Answering complex legal questions.
* Explaining key Indian laws and regulations.
* Providing up-to-date information on central and state government welfare schemes.

Our goal is to be a trustworthy **Legal Companion** available 24/7, making informed decisions accessible to everyone.

## ✨ Features

* **Instant Legal Queries:** Get quick and reliable answers to your questions across various domains of Indian law (e.g., property, marriage, consumer rights).
* **Government Scheme Guide:** Search and explore official details of various schemes, including eligibility criteria and application processes.
* **Actionable Steps:** Receive practical, step-by-step guidance on common legal or procedural requirements.
* **Generative AI Powered:** Built using **Google's Generative AI SDK** for state-of-the-art natural language understanding and information retrieval.
* **Modern Android UX:** A clean, intuitive, and high-performance user experience built using modern Android standards.

## 🛠️ Technology Stack

| Component | Technology | Description |
| :--- | :--- | :--- |
| **Platform** | Android | Target platform for the application. |
| **Language** | [Kotlin](https://kotlinlang.org/) | Preferred language for modern Android development. |
| **AI Backend** | [Google's Generative AI (e.g., Gemini API)](https://ai.google.dev/) | Powers the core legal Q&A and scheme information engine. |
| **Architecture** | Modern Android Architecture (e.g., MVVM) | Ensures maintainability and testability of the codebase. |
| **Build System** | Gradle Kotlin DSL | Used for configuration and dependency management. |

## 🚀 Getting Started

Follow these instructions to set up the project locally for development and testing.

### Prerequisites

1.  **Android Studio:** Latest stable version.
2.  **Android SDK:** Target API Level (as defined in `app/build.gradle.kts`).
3.  **Generative AI API Key:** You will need an API key from Google AI Studio.

### Installation

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/shahnawazofficial/Legal-saathi.git
    cd Legal-saathi
    ```

2.  **Configure API Key:**
    For security, you should not commit your API key directly.

    * Create a file named `local.properties` in the project's root directory.
    * Add your API key as a property:
        ```properties
        GEMINI_API_KEY="YOUR_API_KEY_HERE"
        ```
    * *The application's `build.gradle.kts` is configured to read this key safely during the build process.*

3.  **Open and Sync:**
    * Open the project in Android Studio.
    * Wait for Gradle to sync dependencies.

4.  **Run:**
    * Select an emulator or physical device.
    * Click the **Run** button to launch the application.

## 🤝 Contributing

We welcome contributions! Whether it's adding a feature, fixing a bug, or improving documentation, your help is appreciated.

1.  Fork the repository.
2.  Create your feature branch (`git checkout -b feature/AmazingFeature`).
3.  Commit your changes (`git commit -m 'feat: Add AmazingFeature'`).
4.  Push to the branch (`git push origin feature/AmazingFeature`).
5.  Open a Pull Request.

## 📜 License

Distributed under the **MIT License**. See the `LICENSE` file for more information.

## 📞 Contact

| Role | Contact |
| :--- | :--- |
| **Project Owner** | https://github.com/shahnawazofficial |
| **Repository Link** | https://github.com/shahnawazofficial/Legal-saathi |

---

## 📥 Download Latest Release

**Download APK:**  
https://github.com/shahnawazofficial/Legal-saathi/releases/download/v2.0.0/app-debug.apk

# Legal-saathi
Legal Saathi (Legal Companion) is a modern Android application built using Kotlin that leverages Google's Generative AI to provide Indian citizens with instant, accurate, and actionable legal assistance and government scheme information.
