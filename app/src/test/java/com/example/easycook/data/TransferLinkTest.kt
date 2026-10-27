package com.example.easycook.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferLinkTest {

    private val link = TransferLink(host = "192.168.1.20", port = 53211, token = "abc-123", key = "0f1e2d")

    // --- QR code ---

    @Test
    fun `un lien encode puis relu redonne le meme lien`() {
        assertEquals(link, TransferLink.parse(link.encode()))
    }

    @Test
    fun `le lien a la forme attendue`() {
        assertEquals("easycook://share?h=192.168.1.20&p=53211&t=abc-123&k=0f1e2d&v=1", link.encode())
    }

    @Test
    fun `un code contenant des caracteres speciaux survit a l encodage`() {
        val special = link.copy(token = "a b&c=d/é")

        assertEquals(special, TransferLink.parse(special.encode()))
    }

    @Test
    fun `un QR code d une autre application est ignore`() {
        assertNull(TransferLink.parse("https://example.com"))
        assertNull(TransferLink.parse("bonjour"))
    }

    @Test
    fun `un lien incomplet ou invalide est ignore`() {
        assertNull(TransferLink.parse("easycook://share?h=192.168.1.20&p=53211&v=1"))
        assertNull(TransferLink.parse("easycook://share?h=192.168.1.20&p=abc&t=x&k=ab&v=1"))
        assertNull(TransferLink.parse("easycook://share?h=192.168.1.20&p=70000&t=x&k=ab&v=1"))
        assertNull(TransferLink.parse("easycook://share?p=53211&t=x&v=1"))
        // Sans cle de dechiffrement
        assertNull(TransferLink.parse("easycook://share?h=192.168.1.20&p=53211&t=x&v=1"))
    }

    @Test
    fun `un lien d une version inconnue est ignore`() {
        assertNull(TransferLink.parse("easycook://share?h=192.168.1.20&p=53211&t=x&k=ab&v=2"))
    }

    @Test
    fun `un QR code pointant vers une adresse Internet est refuse`() {
        assertNull(TransferLink.parse("easycook://share?h=8.8.8.8&p=53211&t=x&k=ab&v=1"))
        assertNull(TransferLink.parse("easycook://share?h=exemple.com&p=53211&t=x&k=ab&v=1"))
        assertNull(TransferLink.parse("easycook://share?h=127.0.0.1&p=53211&t=x&k=ab&v=1"))
    }

    @Test
    fun `la cle de dechiffrement n apparait pas dans l adresse de telechargement`() {
        assertFalse(link.downloadUrl().contains("0f1e2d"))
    }

    @Test
    fun `l adresse de telechargement vise le chemin des recettes avec le code`() {
        assertEquals("http://192.168.1.20:53211/recipes?token=abc-123", link.downloadUrl())
    }

    // --- Requetes HTTP ---

    @Test
    fun `le code est extrait d une requete vers les recettes`() {
        assertEquals("abc-123", TransferProtocol.requestedToken("GET /recipes?token=abc-123 HTTP/1.1"))
    }

    @Test
    fun `le code encode dans l adresse est decode`() {
        val url = link.copy(token = "a b&c").downloadUrl()
        val target = url.substringAfter(":53211")

        assertEquals("a b&c", TransferProtocol.requestedToken("GET $target HTTP/1.1"))
    }

    @Test
    fun `une requete vers un autre chemin ou sans code est refusee`() {
        assertNull(TransferProtocol.requestedToken("GET /autre?token=abc HTTP/1.1"))
        assertNull(TransferProtocol.requestedToken("GET /recipes HTTP/1.1"))
        assertNull(TransferProtocol.requestedToken("POST /recipes?token=abc HTTP/1.1"))
        assertNull(TransferProtocol.requestedToken(""))
    }

    // --- Adresse locale ---

    @Test
    fun `le wifi est prefere aux autres interfaces`() {
        val candidates = listOf("rndis0" to "192.168.42.129", "wlan0" to "192.168.1.20")

        assertEquals("192.168.1.20", TransferProtocol.chooseLocalAddress(candidates))
    }

    @Test
    fun `les adresses publiques et de bouclage sont ignorees`() {
        val candidates = listOf("rmnet0" to "100.64.12.3", "lo" to "127.0.0.1", "wlan0" to "8.8.8.8")

        assertNull(TransferProtocol.chooseLocalAddress(candidates))
    }

    @Test
    fun `seules les plages de reseau prive sont acceptees`() {
        assertTrue(TransferProtocol.isPrivateIpv4("10.0.0.5"))
        assertTrue(TransferProtocol.isPrivateIpv4("172.20.1.1"))
        assertTrue(TransferProtocol.isPrivateIpv4("192.168.0.10"))
        assertFalse(TransferProtocol.isPrivateIpv4("172.32.0.1"))
        assertFalse(TransferProtocol.isPrivateIpv4("192.169.0.1"))
        assertFalse(TransferProtocol.isPrivateIpv4("pas une adresse"))
    }
}
