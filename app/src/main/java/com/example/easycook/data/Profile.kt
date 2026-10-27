package com.example.easycook.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Profil importé depuis un autre téléphone. Ses recettes portent son [id] dans `profileId`.
 *
 * « Mon profil » n'a pas de ligne dans cette table : ses recettes utilisent [OWN_PROFILE_ID].
 */
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    /** Numéro affiché dans l'onglet : « Profil 1 », « Profil 2 »… */
    val number: Int,
    /** Date de l'import, en millisecondes depuis l'époque Unix. */
    val importedAt: Long
)

/** Profil tel que l'affiche l'interface. */
data class Profile(
    val id: Int,
    val number: Int
) {
    val isOwn: Boolean
        get() = id == OWN_PROFILE_ID
}

/** Identifiant des recettes de « Mon profil ». */
const val OWN_PROFILE_ID = 0

object ProfileNumbering {

    /**
     * Numéro du prochain profil importé : le plus petit numéro libre à partir de 1.
     * Après la suppression de « Profil 1 », le prochain import reprend donc ce numéro.
     */
    fun next(usedNumbers: Collection<Int>): Int {
        var number = 1
        while (number in usedNumbers) {
            number++
        }
        return number
    }
}
