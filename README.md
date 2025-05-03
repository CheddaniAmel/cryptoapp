
#  CryptoSound – Hybrid Voice Encryption App

**CryptoSound** is a modern Android application that allows users to **record their voice**, **transcribe it**, **encrypt the text using hybrid encryption (AES + RSA)**, and **play back the encrypted and decrypted messages** with text-to-speech. It also includes **live waveform animations**, a **futuristic video background**, and **graphical representations of original and encrypted audio**.

---

##  Key Features

- **Voice Recording** via Android's SpeechRecognizer
- **Hybrid Encryption** (AES for content, RSA for key)
- **Decryption** of encrypted text for verification
- **Text-to-Speech Playback** of encrypted and decrypted messages
- **Live Waveform Animation** during recording
- **Futuristic UI** with video background and glowing buttons
- **Audio Waveform Visualization** for both original and simulated encrypted signals

---

## Tech Stack

- **Kotlin** & **Jetpack Compose**
- Android SpeechRecognizer API
- Android TextToSpeech API
- RSA and AES encryption (custom HybridCrypto class)
- ExoPlayer (for background video playback)
- Custom Composables for waveform and UI animations

---

## How It Works

1. **Record** your voice with a tap.
2. **SpeechRecognizer** transcribes the audio to text.
3. **AES** generates a session key to encrypt the message.
4. The **AES key is encrypted** using **RSA public key**.
5. The encrypted text is shown in Base64 format.
6. **Decryption** restores the original message using the RSA private key.
7. Text-to-Speech reads the encrypted and decrypted results aloud.
8. Original and encrypted audio (simulated) **waveforms are displayed** in the result screen.

---
