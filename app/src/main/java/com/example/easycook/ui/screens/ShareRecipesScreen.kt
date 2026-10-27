package com.example.easycook.ui.screens

import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.easycook.R
import com.example.easycook.StateViewModel
import com.example.easycook.data.Recipe
import com.example.easycook.data.TransferLink
import com.example.easycook.transfer.TransferClient
import com.example.easycook.transfer.TransferCrypto
import com.example.easycook.transfer.TransferServer
import com.example.easycook.ui.composable.CookingTimeChip
import com.example.easycook.ui.composable.QrCode
import com.example.easycook.ui.composable.SectionCard
import com.example.easycook.ui.rememberLocalNetworkAccess
import com.example.easycook.ui.rememberSaveableIntSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/** Étapes du partage, une fois les recettes choisies. */
private sealed interface ShareState {
    data object Preparing : ShareState
    data object NoNetwork : ShareState
    data class Ready(val link: TransferLink) : ShareState
}

/**
 * Partage de recettes par QR code : on choisit les recettes (ou « Tout sélectionner »),
 * puis l'écran affiche un QR code que l'autre téléphone scanne pour les recevoir, photos comprises.
 */
@Composable
fun ShareRecipesScreen(
    profileId: Int,
    navController: NavHostController,
    stateViewModel: StateViewModel
) {
    val recipes by stateViewModel.recipes.collectAsState()
    val profileRecipes = remember(recipes, profileId) { recipes.filter { it.profileId == profileId } }
    val selectedIds = rememberSaveableIntSet()
    var isSharing by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val resources = LocalResources.current
    // Android 17+ : l'accès au réseau local doit être accordé avant d'ouvrir le serveur
    val requestLocalNetwork = rememberLocalNetworkAccess(
        onDenied = {
            Toast.makeText(context, resources.getString(R.string.local_network_denied_share), Toast.LENGTH_LONG).show()
        }
    )

    // Retour depuis le QR code : on revient au choix des recettes plutôt que de quitter
    BackHandler(enabled = isSharing) { isSharing = false }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    if (isSharing) isSharing = false else navController.popBackStack()
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back)
                )
            }
            Text(
                text = stringResource(R.string.share_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isSharing) {
            ShareQrCode(
                profileId = profileId,
                recipeIds = selectedIds.toSet(),
                stateViewModel = stateViewModel,
                onChangeSelection = { isSharing = false },
                onFinish = { navController.popBackStack() }
            )
        } else {
            RecipeSelection(
                recipes = profileRecipes,
                selectedIds = selectedIds,
                onShare = { requestLocalNetwork { isSharing = true } }
            )
        }
    }
}

@Composable
private fun RecipeSelection(
    recipes: List<Recipe>,
    selectedIds: MutableSet<Int>,
    onShare: () -> Unit
) {
    // On ignore les recettes sélectionnées puis supprimées entre-temps
    val selectedCount = recipes.count { it.id in selectedIds }
    val allSelected = recipes.isNotEmpty() && selectedCount == recipes.size

    Column(modifier = Modifier.fillMaxSize()) {
        if (recipes.isEmpty()) {
            Text(
                text = stringResource(R.string.share_no_recipes),
                modifier = Modifier.padding(vertical = 32.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        Text(
            text = stringResource(R.string.share_select_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        SelectableRow(
            checked = allSelected,
            onToggle = {
                if (allSelected) {
                    selectedIds.clear()
                } else {
                    selectedIds.addAll(recipes.map { it.id })
                }
            }
        ) {
            Text(
                text = stringResource(R.string.share_select_all),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.share_selected_count, selectedCount, recipes.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(recipes, key = { it.id }) { recipe ->
                SelectableRow(
                    checked = recipe.id in selectedIds,
                    onToggle = {
                        if (recipe.id in selectedIds) selectedIds.remove(recipe.id) else selectedIds.add(recipe.id)
                    }
                ) {
                    Text(
                        text = recipe.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (recipe.hasCookingTime) {
                        CookingTimeChip(minutes = recipe.cookingTime)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onShare,
            enabled = selectedCount > 0,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.QrCode, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text(pluralStringResource(R.plurals.share_generate_qr, selectedCount, selectedCount))
        }
    }
}

@Composable
private fun SelectableRow(
    checked: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // La ligne entière est cliquable ; la case n'a donc pas besoin de son propre clic
        Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(12.dp))
        content()
    }
}

@Composable
private fun ShareQrCode(
    profileId: Int,
    recipeIds: Set<Int>,
    stateViewModel: StateViewModel,
    onChangeSelection: () -> Unit,
    onFinish: () -> Unit
) {
    var state by remember { mutableStateOf<ShareState>(ShareState.Preparing) }
    var downloads by remember { mutableIntStateOf(0) }

    // Le serveur vit tant que cet écran est affiché ; il s'arrête dès qu'on le quitte
    LaunchedEffect(recipeIds) {
        state = ShareState.Preparing
        val address = withContext(Dispatchers.IO) { TransferClient.localAddress() }
        if (address == null) {
            state = ShareState.NoNetwork
            return@LaunchedEffect
        }

        val json = suspendCancellableCoroutine { continuation ->
            stateViewModel.exportRecipes(profileId = profileId, recipeIds = recipeIds) {
                continuation.resume(it)
            }
        }
        val token = TransferServer.newToken()
        val key = TransferCrypto.newKey()
        val server = withContext(Dispatchers.IO) {
            // Chiffré avec une clé qui ne voyage que dans le QR code ; serveur limité à l'adresse Wi-Fi
            val payload = TransferCrypto.encrypt(json.toByteArray(Charsets.UTF_8), key)
            TransferServer(payload, token, bindAddress = address) { downloads++ }
        }
        try {
            server.start(this)
            state = ShareState.Ready(TransferLink(host = address, port = server.port, token = token, key = key))
            awaitCancellation()
        } finally {
            server.stop()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (val current = state) {
            ShareState.Preparing -> {
                Spacer(modifier = Modifier.height(48.dp))
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.share_preparing))
            }

            ShareState.NoNetwork -> {
                Spacer(modifier = Modifier.height(48.dp))
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.share_no_network),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }

            is ShareState.Ready -> {
                Text(
                    text = pluralStringResource(R.plurals.share_scan_hint, recipeIds.size, recipeIds.size),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Fond blanc fixe : il sert de zone de silence au QR code, même en thème sombre
                Surface(
                    color = Color.White,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.widthIn(max = 320.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        QrCode(
                            content = current.link.encode(),
                            contentDescription = stringResource(R.string.share_qr_description),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SectionCard {
                    Text(
                        text = stringResource(R.string.share_same_network),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (downloads > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = pluralStringResource(R.plurals.share_sent_count, downloads, downloads),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onChangeSelection, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.share_change_selection))
            }
            Button(onClick = onFinish, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.share_finish))
            }
        }
    }
}
