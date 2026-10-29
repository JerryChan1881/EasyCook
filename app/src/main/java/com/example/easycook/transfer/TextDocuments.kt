package com.example.easycook.transfer

import android.content.Context
import androidx.core.net.toUri

/**
 * Lecture et écriture des fichiers d'export choisis par l'utilisateur (sélecteur de fichiers Android).
 * Les documents sont désignés par leur adresse (`content://…`) sous forme de texte.
 *
 * Interface séparée d'Android pour que le ViewModel de l'accueil reste testable sur la JVM.
 * Appels bloquants : à faire hors du thread principal.
 */
interface TextDocuments {

    /** Contenu du document, ou null s'il est illisible. */
    fun read(uri: String): String?

    /** Écrit [text] dans le document ; false en cas d'échec. */
    fun write(uri: String, text: String): Boolean
}

class ContentResolverDocuments(context: Context) : TextDocuments {

    private val resolver = context.applicationContext.contentResolver

    override fun read(uri: String): String? = runCatching {
        resolver.openInputStream(uri.toUri())?.use { it.readBytes().toString(Charsets.UTF_8) }
    }.getOrNull()

    override fun write(uri: String, text: String): Boolean = runCatching {
        resolver.openOutputStream(uri.toUri())?.use { it.write(text.toByteArray(Charsets.UTF_8)) } != null
    }.getOrDefault(false)
}
