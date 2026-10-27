package com.example.easycook.ui.composable

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.easycook.R
import com.example.easycook.data.OWN_PROFILE_ID
import com.example.easycook.data.Profile

private val TabsHorizontalPadding = 16.dp
private val TabsSpacing = 8.dp

/**
 * Onglets rectangulaires des profils, défilables horizontalement :
 * « Mon profil » puis un onglet par profil importé.
 *
 * Chaque onglet occupe un quart de la largeur en portrait, un huitième en paysage.
 */
@Composable
fun ProfileTabs(
    profiles: List<Profile>,
    selectedProfileId: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val visibleTabs = if (isLandscape) 8 else 4
    val tabs = listOf(Profile(id = OWN_PROFILE_ID, number = 0)) + profiles
    val listState = rememberLazyListState()

    // Ramène l'onglet sélectionné à l'écran (par exemple juste après un import)
    LaunchedEffect(selectedProfileId, tabs.size) {
        val index = tabs.indexOfFirst { it.id == selectedProfileId }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // Largeur choisie pour que pile [visibleTabs] onglets tiennent à l'écran
        val tabWidth: Dp =
            (maxWidth - TabsHorizontalPadding * 2 - TabsSpacing * (visibleTabs - 1)) / visibleTabs

        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = TabsHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(TabsSpacing)
        ) {
            items(tabs, key = { it.id }) { profile ->
                ProfileTab(
                    label = if (profile.isOwn) {
                        stringResource(R.string.profile_own)
                    } else {
                        stringResource(R.string.profile_imported, profile.number)
                    },
                    isSelected = profile.id == selectedProfileId,
                    onClick = { onSelect(profile.id) },
                    modifier = Modifier.width(tabWidth)
                )
            }
        }
    }
}

@Composable
private fun ProfileTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(40.dp)
            .semantics {
                role = Role.Tab
                selected = isSelected
            },
        shape = MaterialTheme.shapes.extraSmall,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
        contentColor = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
