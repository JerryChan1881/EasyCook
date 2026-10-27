package com.example.easycook.ui.screens

import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.easycook.R
import com.example.easycook.Routes
import com.example.easycook.StateViewModel
import com.example.easycook.data.ImportResult
import com.example.easycook.data.OWN_PROFILE_ID
import com.example.easycook.data.RecipeSearch
import com.example.easycook.data.TransferLink
import com.example.easycook.transfer.DownloadResult
import com.example.easycook.transfer.TransferClient
import com.example.easycook.ui.composable.EasyCookFab
import com.example.easycook.ui.composable.ProfileTabs
import com.example.easycook.ui.composable.RecipeCard
import com.example.easycook.ui.messageRes
import com.example.easycook.ui.rememberLocalNetworkAccess
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    navController: NavHostController,
    stateViewModel: StateViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // LocalResources suit les changements de configuration (langue, rotation…), contrairement à context.resources
    val resources = LocalResources.current
    val coroutineScope = rememberCoroutineScope()
    val recipes by stateViewModel.recipes.collectAsState()
    val profiles by stateViewModel.profiles.collectAsState()

    // Profil affiché ; si le profil choisi a été supprimé, on revient sur « Mon profil »
    var selectedProfileId by rememberSaveable { mutableIntStateOf(OWN_PROFILE_ID) }
    val currentProfileId = selectedProfileId.takeIf { id ->
        id == OWN_PROFILE_ID || profiles.any { it.id == id }
    } ?: OWN_PROFILE_ID
    val currentProfile = profiles.find { it.id == currentProfileId }
    val profileRecipes = remember(recipes, currentProfileId) {
        recipes.filter { it.profileId == currentProfileId }
    }

    // On garde l'identifiant (et non la recette) pour que la confirmation survive à une rotation
    var recipeToDeleteId by rememberSaveable { mutableStateOf<Int?>(null) }
    val recipeToDelete = recipes.find { it.id == recipeToDeleteId }
    var showDeleteProfile by rememberSaveable { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    // rememberSaveable : la recherche est conservée au retour depuis le détail d'une recette
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val displayedRecipes = remember(profileRecipes, query) { RecipeSearch.search(profileRecipes, query) }

    val showMessage: (String) -> Unit = { message ->
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    // Import commun au fichier et au QR code : crée un profil et l'affiche aussitôt
    val importJson: (String) -> Unit = { json ->
        stateViewModel.importRecipes(json) { result ->
            when (result) {
                is ImportResult.Success -> {
                    selectedProfileId = result.profileId
                    showMessage(
                        resources.getQuantityString(R.plurals.import_success, result.recipeCount, result.recipeCount)
                    )
                }
                is ImportResult.Failure -> showMessage(resources.getString(result.error.messageRes()))
            }
        }
    }

    // Export : l'utilisateur choisit où enregistrer le fichier, puis on y écrit le JSON
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            stateViewModel.exportRecipes { json ->
                coroutineScope.launch {
                    val written = withContext(Dispatchers.IO) {
                        runCatching {
                            context.contentResolver.openOutputStream(uri)?.use {
                                it.write(json.toByteArray(Charsets.UTF_8))
                            } != null
                        }.getOrDefault(false)
                    }
                    showMessage(
                        resources.getString(if (written) R.string.export_success else R.string.export_error)
                    )
                }
            }
        }
    }

    // Import : on lit le fichier choisi puis on crée un nouveau profil avec ses recettes
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val json = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use {
                            it.readBytes().toString(Charsets.UTF_8)
                        }
                    }.getOrNull()
                }
                if (json == null) {
                    showMessage(resources.getString(R.string.import_error_read))
                    return@launch
                }
                importJson(json)
            }
        }
    }

    // Android 17+ : l'accès au réseau local doit être accordé avant de recevoir
    val requestLocalNetwork = rememberLocalNetworkAccess(
        onDenied = { showMessage(resources.getString(R.string.local_network_denied_receive)) }
    )

    // Réception par QR code : on scanne, on télécharge depuis l'autre téléphone, puis on importe
    var isReceiving by remember { mutableStateOf(false) }
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { scan ->
        val text = scan.contents ?: return@rememberLauncherForActivityResult // scan annulé
        val link = TransferLink.parse(text)
        if (link == null) {
            showMessage(resources.getString(R.string.receive_error_not_easycook))
            return@rememberLauncherForActivityResult
        }

        isReceiving = true
        coroutineScope.launch {
            val download = withContext(Dispatchers.IO) { TransferClient.download(link) }
            when (download) {
                is DownloadResult.Success -> importJson(download.json)
                is DownloadResult.Failure -> showMessage(resources.getString(download.error.messageRes()))
            }
            isReceiving = false
        }
    }

    val closeSearch = {
        isSearching = false
        query = ""
    }

    BackHandler(enabled = isSearching) { closeSearch() }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isSearching) {
                SearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    onClose = closeSearch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            } else {
                Row(
                    modifier = Modifier
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
                        IconButton(onClick = { isSearching = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.home_search)
                            )
                        }

                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.home_more_options)
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_share_qr)) },
                                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        navController.navigate(Routes.share(currentProfileId))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_scan_qr)) },
                                    leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        requestLocalNetwork {
                                            scanLauncher.launch(
                                                ScanOptions()
                                                    .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                                    .setPrompt(resources.getString(R.string.scan_prompt))
                                                    .setBeepEnabled(false)
                                                    .setOrientationLocked(false)
                                            )
                                        }
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_export_recipes)) },
                                    leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        exportLauncher.launch(resources.getString(R.string.export_file_name))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_import_recipes)) },
                                    leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        // Certains gestionnaires de fichiers ne reconnaissent pas le type JSON
                                        importLauncher.launch(
                                            arrayOf("application/json", "text/*", "application/octet-stream")
                                        )
                                    }
                                )
                                if (currentProfile != null) {
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
                                            showMenu = false
                                            showDeleteProfile = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            ProfileTabs(
                profiles = profiles,
                selectedProfileId = currentProfileId,
                onSelect = { selectedProfileId = it }
            )

            if (!isSearching) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.home_subtitle),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (profileRecipes.isEmpty()) {
                EmptyRecipes(modifier = Modifier.fillMaxSize())
            } else if (displayedRecipes.isEmpty()) {
                NoSearchResult(query = query, modifier = Modifier.fillMaxSize())
            } else {
                // 2 colonnes en portrait, 3 en paysage
                val isLandscape =
                    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
                val columns = if (isLandscape) 3 else 2

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayedRecipes, key = { it.id }) { recipe ->
                        RecipeCard(
                            recipe = recipe,
                            onClick = {
                                navController.navigate(Routes.recipeDetail(recipe.id))
                            },
                            onToggleFavorite = {
                                stateViewModel.setFavorite(recipe.id, !recipe.isFavorite)
                            },
                            onDelete = { recipeToDeleteId = recipe.id }
                        )
                    }
                }
            }
        }

        EasyCookFab(
            onClick = { navController.navigate(Routes.addRecipe(currentProfileId)) },
            icon = Icons.Default.Add,
            contentDescription = stringResource(R.string.home_add_recipe),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )

        if (isReceiving) {
            // Fenêtre bloquante le temps du téléchargement (quelques secondes avec des photos)
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(stringResource(R.string.receive_progress))
                    }
                }
            )
        }

        if (showDeleteProfile && currentProfile != null) {
            val profileName = stringResource(R.string.profile_imported, currentProfile.number)
            AlertDialog(
                onDismissRequest = { showDeleteProfile = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text(stringResource(R.string.delete_profile_title)) },
                text = { Text(stringResource(R.string.delete_profile_confirm, profileName)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            stateViewModel.deleteProfile(currentProfile.id)
                            selectedProfileId = OWN_PROFILE_ID
                            showDeleteProfile = false
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.action_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteProfile = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        recipeToDelete?.let { recipe ->
            AlertDialog(
                onDismissRequest = { recipeToDeleteId = null },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text(stringResource(R.string.home_delete_recipe)) },
                text = {
                    Text(stringResource(R.string.home_delete_recipe_confirm, recipe.title))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            stateViewModel.deleteRecipe(recipe.id)
                            recipeToDeleteId = null
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.action_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { recipeToDeleteId = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun EmptyRecipes(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.RestaurantMenu,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.home_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Ouvre le clavier dès l'affichage du champ de recherche
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.focusRequester(focusRequester),
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

@Composable
private fun NoSearchResult(query: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.home_search_no_result, query.trim()),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
    }
}
