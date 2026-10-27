package com.example.easycook.transfer

import com.example.easycook.data.TransferLink
import com.example.easycook.data.TransferProtocol
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL

/** Raisons pour lesquelles les recettes n'ont pas pu être reçues. */
enum class DownloadError {
    /** Le téléphone qui partage est injoignable (autre réseau, écran du QR code fermé…). */
    UNREACHABLE,
    /** Le téléphone répond mais refuse : QR code expiré ou remplacé par un nouveau. */
    REFUSED,
    /** Les données reçues dépassent la taille maximale acceptée. */
    TOO_LARGE,
    /** Les données reçues sont illisibles : clé incorrecte ou données modifiées en route. */
    INVALID_DATA
}

sealed interface DownloadResult {
    data class Success(val json: String) : DownloadResult
    data class Failure(val error: DownloadError) : DownloadResult
}

/**
 * Téléchargement des recettes depuis le téléphone qui affiche le QR code, puis déchiffrement
 * avec la clé du QR code. Appels bloquants.
 */
object TransferClient {

    /** Plafond de sécurité : une centaine de recettes avec photos tient largement dedans. */
    const val MAX_DOWNLOAD_BYTES = 64 * 1024 * 1024

    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 30_000

    fun download(link: TransferLink): DownloadResult {
        val connection = try {
            URL(link.downloadUrl()).openConnection() as HttpURLConnection
        } catch (_: IOException) {
            return DownloadResult.Failure(DownloadError.UNREACHABLE)
        }

        return try {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = false

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return DownloadResult.Failure(DownloadError.REFUSED)
            }
            if (connection.contentLengthLong > MAX_DOWNLOAD_BYTES) {
                return DownloadResult.Failure(DownloadError.TOO_LARGE)
            }

            val buffer = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val chunk = ByteArray(64 * 1024)
                while (true) {
                    val read = input.read(chunk)
                    if (read < 0) break
                    if (buffer.size() + read > MAX_DOWNLOAD_BYTES) {
                        return DownloadResult.Failure(DownloadError.TOO_LARGE)
                    }
                    buffer.write(chunk, 0, read)
                }
            }
            val plain = TransferCrypto.decrypt(buffer.toByteArray(), link.key)
                ?: return DownloadResult.Failure(DownloadError.INVALID_DATA)
            DownloadResult.Success(plain.toString(Charsets.UTF_8))
        } catch (_: IOException) {
            DownloadResult.Failure(DownloadError.UNREACHABLE)
        } finally {
            connection.disconnect()
        }
    }

    /** Adresse IPv4 de ce téléphone sur le réseau local (Wi-Fi ou partage de connexion), ou null. */
    fun localAddress(): String? {
        val candidates = runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { networkInterface ->
                    networkInterface.inetAddresses.toList()
                        .filterIsInstance<Inet4Address>()
                        .mapNotNull { address -> address.hostAddress?.let { networkInterface.name to it } }
                }
        }.getOrDefault(emptyList())

        return TransferProtocol.chooseLocalAddress(candidates)
    }
}
