package com.example.easycook.ui.image

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.easycook.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Champ « image » des formulaires de recette (facultatif).
 *
 * Sans image : un bouton propose la galerie ou l'appareil photo. L'image obtenue passe
 * toujours par l'écran de recadrage, d'où l'on peut aussi reprendre la photo.
 * Avec une image : un aperçu, et les boutons pour la changer ou la retirer.
 *
 * [imagePath] est le chemin du fichier enregistré, ou une chaîne vide.
 */
@Composable
fun RecipeImageField(
    recipeId: Int,
    imagePath: String,
    onImageChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageStore = remember { RecipeImageStore(context) }
    val coroutineScope = rememberCoroutineScope()

    // Tout est sauvegardé pour survivre à une rotation pendant le choix ou le recadrage
    var showSourceChooser by rememberSaveable { mutableStateOf(false) }
    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    var cropSourceUri by rememberSaveable { mutableStateOf<String?>(null) }
    var cropFromCamera by rememberSaveable { mutableStateOf(false) }
    var errorRes by rememberSaveable { mutableStateOf<Int?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            cropFromCamera = false
            cropSourceUri = uri.toString()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cropFromCamera = true
            cropSourceUri = pendingCameraUri
        }
    }

    val openGallery = {
        errorRes = null
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val openCamera = {
        errorRes = null
        val uri = imageStore.createCameraUri()
        pendingCameraUri = uri.toString()
        try {
            cameraLauncher.launch(uri)
        } catch (_: ActivityNotFoundException) {
            errorRes = R.string.image_error_no_camera
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (imagePath.isBlank()) {
            OutlinedButton(
                onClick = { showSourceChooser = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.image_add))
            }
        } else {
            RecipeImage(
                recipeId = recipeId,
                imagePath = imagePath,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(CropMath.ASPECT_RATIO)
                    .clip(MaterialTheme.shapes.large)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showSourceChooser = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.image_change))
                }
                TextButton(onClick = { onImageChange("") }) {
                    Text(
                        text = stringResource(R.string.image_remove),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        errorRes?.let { message ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(message),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (showSourceChooser) {
        ImageSourceDialog(
            onGallery = {
                showSourceChooser = false
                openGallery()
            },
            onCamera = {
                showSourceChooser = false
                openCamera()
            },
            onDismiss = { showSourceChooser = false }
        )
    }

    cropSourceUri?.let { source ->
        ImageCropDialog(
            sourceUri = source,
            retakeLabel = if (cropFromCamera) {
                stringResource(R.string.crop_retake)
            } else {
                stringResource(R.string.crop_choose_other)
            },
            onRetake = {
                cropSourceUri = null
                if (cropFromCamera) openCamera() else openGallery()
            },
            onCancel = { cropSourceUri = null },
            onConfirm = { cropped ->
                cropSourceUri = null
                coroutineScope.launch {
                    val savedPath = withContext(Dispatchers.IO) { imageStore.save(cropped) }
                    onImageChange(savedPath)
                }
            }
        )
    }
}

@Composable
private fun ImageSourceDialog(
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.image_source_title)) },
        text = {
            Column {
                ImageSourceOption(
                    icon = Icons.Default.PhotoLibrary,
                    label = stringResource(R.string.image_source_gallery),
                    onClick = onGallery
                )
                ImageSourceOption(
                    icon = Icons.Default.PhotoCamera,
                    label = stringResource(R.string.image_source_camera),
                    onClick = onCamera
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun ImageSourceOption(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, modifier = Modifier.weight(1f))
    }
}
