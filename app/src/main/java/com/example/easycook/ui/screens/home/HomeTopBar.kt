package com.example.easycook.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.easycook.R

/** Actions du menu « ⋮ » de l'accueil. */
class HomeMenuActions(
    val onShareQrCode: () -> Unit,
    val onScanQrCode: () -> Unit,
    val onExportFile: () -> Unit,
    val onImportFile: () -> Unit
)

/** Titre de l'accueil, avec la loupe de recherche et le menu « ⋮ ». */
@Composable
fun HomeTitleBar(
    onSearch: () -> Unit,
    menuActions: HomeMenuActions,
    // Null pour « Mon profil », qui ne peut pas être supprimé
    onDeleteProfile: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Row {
            IconButton(onClick = onSearch) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.home_search)
                )
            }

            HomeMenu(actions = menuActions, onDeleteProfile = onDeleteProfile)
        }
    }
}

@Composable
private fun HomeMenu(actions: HomeMenuActions, onDeleteProfile: (() -> Unit)?) {
    var expanded by remember { mutableStateOf(false) }

    // Ferme le menu avant de lancer l'action choisie
    val item: @Composable (Int, ImageVector, () -> Unit) -> Unit = { label, icon, action ->
        DropdownMenuItem(
            text = { Text(stringResource(label)) },
            leadingIcon = { Icon(icon, contentDescription = null) },
            onClick = {
                expanded = false
                action()
            }
        )
    }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.home_more_options)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            item(R.string.menu_share_qr, Icons.Default.QrCode, actions.onShareQrCode)
            item(R.string.menu_scan_qr, Icons.Default.QrCodeScanner, actions.onScanQrCode)
            HorizontalDivider()
            item(R.string.menu_export_recipes, Icons.Default.FileUpload, actions.onExportFile)
            item(R.string.menu_import_recipes, Icons.Default.FileDownload, actions.onImportFile)

            onDeleteProfile?.let { deleteProfile ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.menu_delete_profile),
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        expanded = false
                        deleteProfile()
                    }
                )
            }
        }
    }
}

/** Champ de recherche qui remplace le titre ; le clavier s'ouvre dès son affichage. */
@Composable
fun HomeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .focusRequester(focusRequester),
        placeholder = { Text(stringResource(R.string.home_search_placeholder)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.home_search_close)
                )
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
    )
}
