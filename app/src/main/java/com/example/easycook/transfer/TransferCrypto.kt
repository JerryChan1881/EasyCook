package com.example.easycook.transfer

import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Chiffrement des recettes pendant le transfert (AES-256-GCM).
 *
 * La clé est générée à chaque partage et ne voyage que dans le QR code, jamais sur le réseau :
 * un appareil du même Wi-Fi qui intercepterait le trafic ne verrait que des données illisibles.
 * GCM authentifie aussi les données : une modification en route est détectée au déchiffrement.
 */
object TransferCrypto {

    private const val KEY_BYTES = 32
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private val random = SecureRandom()

    /** Nouvelle clé aléatoire, en hexadécimal pour tenir dans le QR code. */
    fun newKey(): String = randomBytes(KEY_BYTES).toHex()

    /** Chiffre [plain] ; le résultat contient le nonce suivi des données chiffrées. */
    fun encrypt(plain: ByteArray, hexKey: String): ByteArray {
        val nonce = randomBytes(NONCE_BYTES)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(hexKey.fromHex(), "AES"), GCMParameterSpec(TAG_BITS, nonce))
        }
        return nonce + cipher.doFinal(plain)
    }

    /** Déchiffre des données produites par [encrypt] ; null si la clé est fausse ou les données altérées. */
    fun decrypt(data: ByteArray, hexKey: String): ByteArray? {
        if (data.size <= NONCE_BYTES) {
            return null
        }
        val key = runCatching { hexKey.fromHex() }.getOrNull()?.takeIf { it.size == KEY_BYTES } ?: return null
        return try {
            Cipher.getInstance(TRANSFORMATION).run {
                init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, data, 0, NONCE_BYTES))
                doFinal(data, NONCE_BYTES, data.size - NONCE_BYTES)
            }
        } catch (_: GeneralSecurityException) {
            null
        }
    }

    private fun randomBytes(size: Int): ByteArray = ByteArray(size).also { random.nextBytes(it) }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.fromHex(): ByteArray {
        require(length % 2 == 0) { "Clé hexadécimale de longueur impaire" }
        return ByteArray(length / 2) { index -> substring(index * 2, index * 2 + 2).toInt(16).toByte() }
    }
}
