package com.example.easycook.transfer

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferCryptoTest {

    private val message = """{"recipes":[{"title":"Crème brûlée"}]}""".toByteArray(Charsets.UTF_8)

    @Test
    fun `des donnees chiffrees puis dechiffrees avec la meme cle sont identiques`() {
        val key = TransferCrypto.newKey()

        assertArrayEquals(message, TransferCrypto.decrypt(TransferCrypto.encrypt(message, key), key))
    }

    @Test
    fun `les donnees chiffrees ne laissent pas voir le contenu`() {
        val encrypted = TransferCrypto.encrypt(message, TransferCrypto.newKey())

        assertFalse(String(encrypted, Charsets.ISO_8859_1).contains("recipes"))
    }

    @Test
    fun `deux chiffrements du meme contenu different grace au nonce aleatoire`() {
        val key = TransferCrypto.newKey()

        assertFalse(TransferCrypto.encrypt(message, key).contentEquals(TransferCrypto.encrypt(message, key)))
    }

    @Test
    fun `une mauvaise cle ne dechiffre rien`() {
        val encrypted = TransferCrypto.encrypt(message, TransferCrypto.newKey())

        assertNull(TransferCrypto.decrypt(encrypted, TransferCrypto.newKey()))
    }

    @Test
    fun `des donnees modifiees en route sont detectees`() {
        val key = TransferCrypto.newKey()
        val tampered = TransferCrypto.encrypt(message, key).also { it[it.size / 2] = (it[it.size / 2] + 1).toByte() }

        assertNull(TransferCrypto.decrypt(tampered, key))
    }

    @Test
    fun `des donnees trop courtes ou une cle invalide sont refusees sans planter`() {
        val key = TransferCrypto.newKey()

        assertNull(TransferCrypto.decrypt(ByteArray(5), key))
        assertNull(TransferCrypto.decrypt(TransferCrypto.encrypt(message, key), "pas-une-cle"))
        assertNull(TransferCrypto.decrypt(TransferCrypto.encrypt(message, key), "abcd"))
    }

    @Test
    fun `les cles generees font 256 bits et sont toutes differentes`() {
        val keys = (1..100).map { TransferCrypto.newKey() }

        assertTrue(keys.all { it.length == 64 && it.all { char -> char in '0'..'9' || char in 'a'..'f' } })
        assertEquals(100, keys.toSet().size)
    }
}
