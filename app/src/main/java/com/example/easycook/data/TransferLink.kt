package com.example.easycook.data

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Contenu du QR code de partage : où joindre le téléphone qui partage, avec quel code d'accès,
 * et la clé qui déchiffre les recettes reçues (elle ne circule jamais sur le réseau).
 *
 * Le QR code ne transporte pas les recettes (un QR code tient environ 3 Ko, une photo bien plus) :
 * il indique au téléphone qui scanne l'adresse du petit serveur ouvert sur le Wi-Fi local,
 * et le code secret à présenter pour télécharger les recettes.
 *
 * Forme : `easycook://share?h=192.168.1.20&p=53211&t=CODE&k=CLE&v=1`
 */
data class TransferLink(
    val host: String,
    val port: Int,
    val token: String,
    /** Clé AES en hexadécimal (voir [com.example.easycook.transfer.TransferCrypto]). */
    val key: String
) {
    /** Texte à encoder dans le QR code. */
    fun encode(): String =
        "$SCHEME://$HOST_PART?h=${host.urlEncode()}&p=$port&t=${token.urlEncode()}&k=${key.urlEncode()}&v=$VERSION"

    /** Adresse HTTP où télécharger les recettes. */
    fun downloadUrl(): String = "http://$host:$port${TransferProtocol.PATH}?token=${token.urlEncode()}"

    companion object {
        const val SCHEME = "easycook"
        private const val HOST_PART = "share"
        private const val VERSION = 1

        /**
         * Lit le texte d'un QR code ; null si ce n'est pas un QR code de partage EasyCook valide.
         * L'adresse doit être celle d'un réseau local : un QR code piégé ne peut pas faire
         * contacter une adresse sur Internet.
         */
        fun parse(text: String): TransferLink? {
            val prefix = "$SCHEME://$HOST_PART?"
            if (!text.trim().startsWith(prefix)) {
                return null
            }

            val parameters = text.trim()
                .removePrefix(prefix)
                .split("&")
                .mapNotNull { pair ->
                    val separator = pair.indexOf('=')
                    if (separator <= 0) null else pair.substring(0, separator) to pair.substring(separator + 1).urlDecode()
                }
                .toMap()

            val version = parameters["v"]?.toIntOrNull() ?: return null
            val host = parameters["h"]?.takeIf { TransferProtocol.isPrivateIpv4(it) } ?: return null
            val port = parameters["p"]?.toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
            val token = parameters["t"]?.takeIf { it.isNotBlank() } ?: return null
            val key = parameters["k"]?.takeIf { it.isNotBlank() } ?: return null

            return if (version == VERSION) TransferLink(host, port, token, key) else null
        }

        private fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")

        private fun String.urlDecode(): String = runCatching { URLDecoder.decode(this, "UTF-8") }.getOrDefault(this)
    }
}

/** Règles du mini protocole HTTP entre les deux téléphones. */
object TransferProtocol {

    /** Chemin unique servi par le téléphone qui partage. */
    const val PATH = "/recipes"

    /**
     * Code présenté dans la première ligne d'une requête HTTP (`GET /recipes?token=X HTTP/1.1`),
     * ou null si la requête ne vise pas les recettes.
     */
    fun requestedToken(requestLine: String): String? {
        val parts = requestLine.trim().split(" ")
        if (parts.size < 2 || parts[0] != "GET") {
            return null
        }

        val target = parts[1]
        val path = target.substringBefore('?')
        if (path != PATH) {
            return null
        }

        return target.substringAfter('?', missingDelimiterValue = "")
            .split("&")
            .firstOrNull { it.startsWith("token=") }
            ?.removePrefix("token=")
            ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrNull() }
    }

    /**
     * Choisit l'adresse IPv4 locale à annoncer dans le QR code parmi celles du téléphone
     * (nom de l'interface réseau, adresse). Le Wi-Fi (`wlan`) passe avant les autres, et
     * seules les adresses de réseau privé sont retenues : le serveur ne doit pas être
     * annoncé sur une adresse publique ou de réseau mobile.
     */
    fun chooseLocalAddress(candidates: List<Pair<String, String>>): String? =
        candidates
            .filter { (_, address) -> isPrivateIpv4(address) }
            .sortedByDescending { (interfaceName, _) -> interfaceName.startsWith("wlan") }
            .firstOrNull()
            ?.second

    fun isPrivateIpv4(address: String): Boolean {
        val bytes = address.split(".").mapNotNull { it.toIntOrNull() }
        if (bytes.size != 4 || bytes.any { it !in 0..255 }) {
            return false
        }
        return bytes[0] == 10 ||
            (bytes[0] == 172 && bytes[1] in 16..31) ||
            (bytes[0] == 192 && bytes[1] == 168)
    }
}
