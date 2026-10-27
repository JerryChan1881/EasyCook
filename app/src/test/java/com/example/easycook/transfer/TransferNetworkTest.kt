package com.example.easycook.transfer

import com.example.easycook.data.TransferLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

/**
 * Vrai transfert serveur -> client, en local sur la machine de test (127.0.0.1), chiffrement compris.
 * Verifie le protocole de bout en bout sans emulateur.
 */
class TransferNetworkTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val servers = mutableListOf<TransferServer>()
    private val key = TransferCrypto.newKey()

    @After
    fun tearDown() {
        servers.forEach { it.stop() }
        scope.cancel()
    }

    /** Demarre un serveur comme le fait l'ecran de partage : donnees chiffrees, ecoute sur une seule adresse. */
    private fun startServer(json: String, token: String, onDownloaded: () -> Unit = {}): TransferServer =
        TransferServer(
            payload = TransferCrypto.encrypt(json.toByteArray(Charsets.UTF_8), key),
            token = token,
            bindAddress = LOOPBACK,
            onDownloaded = onDownloaded
        )
            .also { servers += it }
            .also { it.start(scope) }

    private fun linkTo(server: TransferServer, token: String = "secret") =
        TransferLink(host = LOOPBACK, port = server.port, token = token, key = key)

    @Test
    fun `le client recoit et dechiffre exactement les donnees servies`() {
        val json = """{"format":"easycook-recipes","recipes":[{"title":"Crème brûlée"}]}"""
        val server = startServer(json, token = "secret")

        assertEquals(DownloadResult.Success(json), TransferClient.download(linkTo(server)))
    }

    @Test
    fun `les donnees qui circulent sur le reseau sont illisibles`() {
        val server = startServer("""{"title":"Recette secrete"}""", token = "secret")

        // Ce que verrait un appareil qui intercepte la reponse HTTP
        val connection = URL(linkTo(server).downloadUrl()).openConnection() as HttpURLConnection
        val rawBody = connection.inputStream.use { String(it.readBytes(), Charsets.ISO_8859_1) }
        connection.disconnect()

        assertFalse(rawBody.contains("Recette secrete"))
    }

    @Test
    fun `sans la bonne cle les donnees recues sont refusees`() {
        val server = startServer("{}", token = "secret")

        val result = TransferClient.download(linkTo(server).copy(key = TransferCrypto.newKey()))

        assertEquals(DownloadResult.Failure(DownloadError.INVALID_DATA), result)
    }

    @Test
    fun `un mauvais code est refuse`() {
        val server = startServer("{}", token = "secret")

        assertEquals(
            DownloadResult.Failure(DownloadError.REFUSED),
            TransferClient.download(linkTo(server, token = "devine"))
        )
    }

    @Test
    fun `une requete vers un autre chemin recoit une erreur 404`() {
        val server = startServer("{}", token = "secret")

        val connection = URL("http://$LOOPBACK:${server.port}/autre").openConnection() as HttpURLConnection

        assertEquals(404, connection.responseCode)
        connection.disconnect()
    }

    @Test
    fun `chaque telechargement reussi est signale`() {
        val downloads = AtomicInteger(0)
        val server = startServer("{}", token = "secret") { downloads.incrementAndGet() }

        TransferClient.download(linkTo(server))
        TransferClient.download(linkTo(server))
        TransferClient.download(linkTo(server, token = "faux"))

        assertEquals(2, downloads.get())
    }

    @Test
    fun `un serveur arrete est injoignable`() {
        val server = startServer("{}", token = "secret")
        val link = linkTo(server)
        server.stop()

        assertEquals(DownloadResult.Failure(DownloadError.UNREACHABLE), TransferClient.download(link))
    }

    @Test
    fun `de grosses donnees passent sans perte`() {
        // Environ 3 Mo, l'ordre de grandeur de quelques recettes avec photos
        val big = "x".repeat(3 * 1024 * 1024)
        val server = startServer(big, token = "secret")

        val result = TransferClient.download(linkTo(server))

        assertEquals(big.length, (result as DownloadResult.Success).json.length)
    }

    @Test
    fun `les codes generes sont longs et tous differents`() {
        val tokens = (1..100).map { TransferServer.newToken() }

        assertTrue(tokens.all { it.length == 32 })
        assertEquals(100, tokens.toSet().size)
    }

    private companion object {
        const val LOOPBACK = "127.0.0.1"
    }
}
