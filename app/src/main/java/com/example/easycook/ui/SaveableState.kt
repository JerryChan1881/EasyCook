package com.example.easycook.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList

/**
 * Liste de textes modifiable qui survit aux recréations de l'écran
 * (rotation, passage en mode multi-fenêtre…), contrairement à
 * `remember { mutableStateListOf() }`.
 */
@Composable
fun rememberSaveableStringList(initial: () -> List<String>): SnapshotStateList<String> =
    rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) {
        initial().toMutableStateList()
    }
