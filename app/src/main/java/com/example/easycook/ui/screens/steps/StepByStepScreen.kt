package com.example.easycook.ui.screens.steps

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.easycook.R
import com.example.easycook.data.RecipeStep
import com.example.easycook.ui.composable.EasyCookFab

@Composable
fun StepByStepScreen(
    viewModel: StepByStepViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Texte en cours de saisie ; il repart de l'instruction de l'étape chaque fois qu'on en ouvre une
    var instructionDraft by rememberSaveable(state.editingStepId) {
        mutableStateOf(state.steps.find { it.id == state.editingStepId }?.instruction.orEmpty())
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back)
                )
            }

            Text(
                text = state.recipeTitle,
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.step_by_step_title),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                state.isLoading -> Unit
                state.steps.isEmpty() -> NoSteps()
                else -> {
                    StepsProgress(
                        doneCount = state.doneCount,
                        totalCount = state.steps.size,
                        onUncheckAll = viewModel::uncheckAll
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp)
                    ) {
                        items(state.steps, key = { it.id }) { step ->
                            StepCard(
                                step = step,
                                isEditing = step.id == state.editingStepId,
                                instructionDraft = instructionDraft,
                                showEmptyError = state.showEmptyInstructionError,
                                onInstructionDraftChange = { instructionDraft = it },
                                onStartEditing = { viewModel.startEditing(step.id) },
                                onConfirmEditing = { viewModel.confirmInstruction(instructionDraft) },
                                onCancelEditing = viewModel::cancelEditing,
                                onToggleDone = { viewModel.setDone(step.id, it) },
                                onDelete = { viewModel.deleteStep(step.id) }
                            )
                        }
                    }
                }
            }
        }

        EasyCookFab(
            onClick = viewModel::startAddingStep,
            icon = Icons.Default.Add,
            contentDescription = stringResource(R.string.step_add),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

/** Barre de progression, compteur d'étapes terminées et bouton « Tout décocher ». */
@Composable
private fun StepsProgress(doneCount: Int, totalCount: Int, onUncheckAll: () -> Unit) {
    LinearProgressIndicator(
        progress = { doneCount.toFloat() / totalCount },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = pluralStringResource(R.plurals.step_by_step_progress, totalCount, doneCount, totalCount),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )

        if (doneCount > 0) {
            TextButton(onClick = onUncheckAll) {
                Text(stringResource(R.string.step_by_step_uncheck_all))
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun NoSteps() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.step_by_step_empty_title),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.step_by_step_empty_hint),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun StepCard(
    step: RecipeStep,
    isEditing: Boolean,
    instructionDraft: String,
    showEmptyError: Boolean,
    onInstructionDraftChange: (String) -> Unit,
    onStartEditing: () -> Unit,
    onConfirmEditing: () -> Unit,
    onCancelEditing: () -> Unit,
    onToggleDone: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = step.isDone,
                    onCheckedChange = { onToggleDone(it) }
                )

                if (isEditing) {
                    OutlinedTextField(
                        value = instructionDraft,
                        onValueChange = { onInstructionDraftChange(it) },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.step_instruction)) },
                        placeholder = { Text(stringResource(R.string.step_instruction_placeholder)) },
                        singleLine = false,
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = { onConfirmEditing() }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.step_confirm_instruction),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Text(
                        text = step.instruction,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (step.isDone) {
                            TextDecoration.LineThrough
                        } else {
                            null
                        },
                        color = if (step.isDone) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    IconButton(onClick = { onStartEditing() }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.step_edit)
                        )
                    }
                }

                IconButton(onClick = { onDelete() }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.step_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (isEditing) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showEmptyError) {
                        Text(
                            text = stringResource(R.string.error_instruction_empty),
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    TextButton(onClick = { onCancelEditing() }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            }
        }
    }
}
