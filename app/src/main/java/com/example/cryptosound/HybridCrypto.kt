package com.example.cryptosound

import android.util.Base64
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

object HybridCrypto {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    fun generateRSAKeyPair(): KeyPair {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        return keyGen.generateKeyPair()
    }

    fun generateAESKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(128)
        return keyGen.generateKey()
    }

    fun encryptMessage(message: String, aesKey: SecretKey): ByteArray {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, aesKey)
        return cipher.doFinal(message.toByteArray())
    }

    fun encryptAESKey(aesKey: SecretKey, publicKey: PublicKey): ByteArray {
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(aesKey.encoded)
    }

    fun decryptAESKey(encryptedKey: ByteArray, privateKey: PrivateKey): SecretKey {
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        val decryptedKey = cipher.doFinal(encryptedKey)
        return SecretKeySpec(decryptedKey, "AES")
    }

    fun decryptMessage(encryptedMessage: ByteArray, aesKey: SecretKey): String {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, aesKey)
        val decryptedBytes = cipher.doFinal(encryptedMessage)
        return String(decryptedBytes)
    }

    fun toBase64(data: ByteArray): String = Base64.encodeToString(data, Base64.DEFAULT)
    fun fromBase64(data: String): ByteArray = Base64.decode(data, Base64.DEFAULT)
}
