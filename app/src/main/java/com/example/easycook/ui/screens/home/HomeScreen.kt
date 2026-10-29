package com.example.easycook.ui.screens.home

import android.content.res.Resources
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.easycook.R
import com.example.easycook.data.Recipe
import com.example.easycook.data.RecipeSearch
import com.example.easycook.ui.composable.EasyCookFab
import com.example.easycook.ui.composable.ProfileTabs
import com.example.easycook.ui.messageRes
import com.example.easycook.ui.rememberLocalNetworkAccess
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenRecipe: (recipeId: Int) -> Unit,
    onAddRecipe: (profileId: Int) -> Unit,
    onShare: (profileId: Int) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // LocalResources suit les changements de configuration (langue, rotation…), contrairement à context.resources
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            Toast.makeText(context, event.message(resources), Toast.LENGTH_LONG).show()
        }
    }

    // Export : l'utilisateur choisit où enregistrer le fichier
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportToFile(it.toString()) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importFromFile(it.toString()) } }

    // contents est null si le scan a été annulé
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { scan ->
        scan.contents?.let(viewModel::receive)
    }

    // Android 17+ : l'accès au réseau local doit être accordé avant de recevoir
    val requestLocalNetwork = rememberLocalNetworkAccess(
        onDenied = {
            Toast.makeText(context, resources.getString(R.string.local_network_denied_receive), Toast.LENGTH_LONG).show()
        }
    )

    val menuActions = HomeMenuActions(
        onShareQrCode = { onShare(state.selectedProfileId) },
        onScanQrCode = {
            requestLocalNetwork {
                scanLauncher.launch(
                    ScanOptions()
                        .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        .setPrompt(resources.getString(R.string.scan_prompt))
                        .setBeepEnabled(false)
                        .setOrientationLocked(false)
                )
            }
        },
        onExportFile = { exportLauncher.launch(resources.getString(R.string.export_file_name)) },
        // Certains gestionnaires de fichiers ne reconnaissent pas le type JSON
        onImportFile = { importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream")) }
    )

    HomeContent(
        state = state,
        menuActions = menuActions,
        onSelectProfile = viewModel::selectProfile,
        onOpenRecipe = onOpenRecipe,
        onAddRecipe = { onAddRecipe(state.selectedProfileId) },
        onToggleFavorite = viewModel::toggleFavorite,
        onDeleteRecipe = viewModel::deleteRecipe,
        onDeleteProfile = viewModel::deleteProfile
    )
}

/** Contenu de l'accueil, sans ViewModel : il ne fait qu'afficher [state] et remonter les actions. */
@Composable
fun HomeContent(
    state: HomeUiState,
    menuActions: HomeMenuActions,
    onSelectProfile: (Int) -> Unit,
    onOpenRecipe: (Int) -> Unit,
    onAddRecipe: () -> Unit,
    onToggleFavorite: (Recipe) -> Unit,
    onDeleteRecipe: (Int) -> Unit,
    onDeleteProfile: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // rememberSaveable : la recherche est conservée au retour depuis le détail d'une recette
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    // On garde l'identifiant (et non la recette) pour que la confirmation survive à une rotation
    var recipeToDeleteId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showDeleteProfile by rememberSaveable { mutableStateOf(false) }

    val displayedRecipes = remember(state.recipes, query) { RecipeSearch.search(state.recipes, query) }
    val selectedProfile = state.selectedProfile

    val closeSearch = {
        isSearching = false
        query = ""
    }
    BackHandler(enabled = isSearching) { closeSearch() }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isSearching) {
                HomeSearchBar(query = query, onQueryChange = { query = it }, onClose = closeSearch)
            } else {
                HomeTitleBar(
                    onSearch = { isSearching = true },
                    menuActions = menuActions,
                    onDeleteProfile = selectedProfile?.let { { showDeleteProfile = true } }
                )
            }

            ProfileTabs(
                profiles = state.profiles,
                selectedProfileId = state.selectedProfileId,
                onSelect = onSelectProfile
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

            when {
                // Rien tant que la base n'a pas répondu, pour ne pas faire clignoter « Aucune recette »
                state.isLoading -> Unit
                state.recipes.isEmpty() -> EmptyRecipes()
                displayedRecipes.isEmpty() -> NoSearchResult(query = query)
                else -> RecipeGrid(
                    recipes = displayedRecipes,
                    onOpen = { onOpenRecipe(it.id) },
                    onToggleFavorite = onToggleFavorite,
                    onDelete = { recipeToDeleteId = it.id }
                )
            }
        }

        EasyCookFab(
            onClick = onAddRecipe,
            icon = Icons.Default.Add,
            contentDescription = stringResource(R.string.home_add_recipe),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )

        if (state.isReceiving) {
            ReceivingDialog()
        }

        if (showDeleteProfile && selectedProfile != null) {
            val profileName = stringResource(R.string.profile_imported, selectedProfile.number)
            ConfirmDeleteDialog(
                title = stringResource(R.string.delete_profile_title),
                message = stringResource(R.string.delete_profile_confirm, profileName),
                onConfirm = {
                    onDeleteProfile(selectedProfile.id)
                    showDeleteProfile = false
                },
                onDismiss = { showDeleteProfile = false }
            )
        }

        state.recipes.find { it.id == recipeToDeleteId }?.let { recipe ->
            ConfirmDeleteDialog(
                title = stringResource(R.string.home_delete_recipe),
                message = stringResource(R.string.home_delete_recipe_confirm, recipe.title),
                onConfirm = {
                    onDeleteRecipe(recipe.id)
                    recipeToDeleteId = null
                },
                onDismiss = { recipeToDeleteId = null }
            )
        }
    }
}

private fun HomeEvent.message(resources: Resources): String = when (this) {
    is HomeEvent.Imported -> resources.getQuantityString(R.plurals.import_success, recipeCount, recipeCount)
    is HomeEvent.ImportFailed -> resources.getString(error.messageRes())
    HomeEvent.FileUnreadable -> resources.getString(R.string.import_error_read)
    is HomeEvent.Exported -> resources.getString(if (success) R.string.export_success else R.string.export_error)
    HomeEvent.NotAnEasyCookQrCode -> resources.getString(R.string.receive_error_not_easycook)
    is HomeEvent.ReceiveFailed -> resources.getString(error.messageRes())
}
