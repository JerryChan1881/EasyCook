package com.example.easycook.transfer

import com.example.easycook.data.TransferProtocol
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Mini serveur HTTP ouvert sur le Wi-Fi local par le téléphone qui partage ses recettes,
 * le temps que l'écran du QR code reste affiché.
 *
 * Il ne sert qu'une ressource ([TransferProtocol.PATH]) et seulement à qui présente le code
 * secret du QR code. Les données servies sont déjà chiffrées ([TransferCrypto]) avec une clé
 * qui ne figure que dans le QR code.
 */
class TransferServer(
    private val payload: ByteArray,
    private val token: String,
    /** Adresse locale sur laquelle écouter (le Wi-Fi) ; null pour toutes les interfaces. */
    bindAddress: String? = null,
    /** Appelé (hors du thread principal) chaque fois qu'un téléphone a téléchargé les recettes. */
    private val onDownloaded: () -> Unit = {}
) {
    // Port 0 : le système choisit un port libre. Lié à l'adresse Wi-Fi, le serveur n'est pas
    // joignable par les autres interfaces du téléphone (réseau mobile…)
    private val serverSocket = ServerSocket(0, BACKLOG, bindAddress?.let { InetAddress.getByName(it) })

    val port: Int
        get() = serverSocket.localPort

    fun start(scope: CoroutineScope, dispatcher: CoroutineDispatcher = Dispatchers.IO): Job =
        scope.launch(dispatcher) {
            while (isActive) {
                val socket = try {
                    serverSocket.accept()
                } catch (_: SocketException) {
                    break // serveur arrêté
                }
                launch { handle(socket) }
            }
        }

    fun stop() {
        runCatching { serverSocket.close() }
    }

    private fun handle(socket: Socket) {
        socket.use { client ->
            client.soTimeout = SOCKET_TIMEOUT_MS
            val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.ISO_8859_1))
            val requestLine = runCatching { reader.readLine() }.getOrNull() ?: return
            // Les en-têtes de la requête ne servent pas : on les lit jusqu'à la ligne vide
            while (true) {
                val header = runCatching { reader.readLine() }.getOrNull()
                if (header.isNullOrEmpty()) break
            }

            val output = BufferedOutputStream(client.getOutputStream())
            val requestedToken = TransferProtocol.requestedToken(requestLine)
            when {
                requestedToken == null -> respond(output, 404, "Not Found")
                !sameToken(requestedToken) -> respond(output, 403, "Forbidden")
                else -> {
                    respond(output, 200, "OK", payload, "application/json; charset=utf-8")
                    onDownloaded()
                }
            }
        }
    }

    // Comparaison en temps constant, pour ne pas laisser deviner le code par mesure du temps de réponse
    private fun sameToken(candidate: String): Boolean =
        MessageDigest.isEqual(candidate.toByteArray(), token.toByteArray())

    private fun respond(
        output: OutputStream,
        status: Int,
        reason: String,
        body: ByteArray = ByteArray(0),
        contentType: String = "text/plain; charset=utf-8"
    ) {
        val headers = "HTTP/1.1 $status $reason\r\n" +
            "Content-Type: $contentType\r\n" +
            "Content-Length: ${body.size}\r\n" +
            "Connection: close\r\n\r\n"
        runCatching {
            output.write(headers.toByteArray(Charsets.ISO_8859_1))
            output.write(body)
            output.flush()
        }
    }

    companion object {
        private const val SOCKET_TIMEOUT_MS = 15_000
        private const val BACKLOG = 10

        /** Code secret aléatoire à mettre dans le QR code. */
        fun newToken(): String {
            val bytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
