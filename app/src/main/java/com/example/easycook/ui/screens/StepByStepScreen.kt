package com.example.easycook.ui.screens

import androidx.annotation.StringRes
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.easycook.R
import com.example.easycook.StateViewModel
import com.example.easycook.data.Recipe
import com.example.easycook.data.RecipeStep

@Composable
fun StepByStepScreen(
    recipe: Recipe,
    navController: NavHostController,
    stateViewModel: StateViewModel
) {
    val steps by stateViewModel.observeSteps(recipe.id)
        .collectAsState(initial = emptyList())
    val focusRequester = remember { FocusRequester() }

    var pendingStepId by remember { mutableStateOf<Long?>(null) }
    var instructionDraft by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<Int?>(null) }

    val startAddingStep = {
        errorMessage = null
        instructionDraft = ""
        stateViewModel.addStep(recipe.id, "") { newStepId ->
            pendingStepId = newStepId
        }
    }

    val confirmDraft: () -> Unit = {
        val trimmed = instructionDraft.trim()
        if (trimmed.isBlank()) {
            errorMessage = R.string.error_instruction_empty
        } else {
            pendingStepId?.let { stepId ->
                stateViewModel.updateStepInstruction(stepId, trimmed)
            }
            pendingStepId = null
            errorMessage = null
            instructionDraft = ""
        }
    }

    val cancelDraft: () -> Unit = {
        pendingStepId?.let { stepId -> stateViewModel.deleteStep(stepId) }
        pendingStepId = null
        errorMessage = null
        instructionDraft = ""
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            IconButton(
                onClick = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back)
                )
            }

            Text(
                text = recipe.title,
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.step_by_step_title),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (steps.isNotEmpty()) {
                val doneCount = steps.count { it.isDone }

                LinearProgressIndicator(
                    progress = { doneCount.toFloat() / steps.size },
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
                        text = pluralStringResource(R.plurals.step_by_step_progress, steps.size, doneCount, steps.size),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (doneCount > 0) {
                        TextButton(onClick = { stateViewModel.resetSteps(recipe.id) }) {
                            Text(stringResource(R.string.step_by_step_uncheck_all))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (steps.isEmpty()) {
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
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 96.dp
                    )
                ) {
                    items(steps, key = { it.id }) { step ->
                        StepCard(
                            step = step,
                            isEditing = step.id == pendingStepId,
                            instructionDraft = instructionDraft,
                            errorMessage = errorMessage,
                            onInstructionDraftChange = { instructionDraft = it },
                            onStartEditing = {
                                pendingStepId = step.id
                                instructionDraft = step.instruction
                                errorMessage = null
                            },
                            onConfirmEditing = confirmDraft,
                            onCancelEditing = cancelDraft,
                            onToggleDone = { isChecked ->
                                stateViewModel.toggleStep(step.id, isChecked)
                            },
                            onDelete = {
                                if (pendingStepId == step.id) {
                                    pendingStepId = null
                                    errorMessage = null
                                    instructionDraft = ""
                                }
                                stateViewModel.deleteStep(step.id)
                            },
                            focusRequester = focusRequester
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { startAddingStep() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.step_add)
            )
        }
    }
}

@Composable
private fun StepCard(
    step: RecipeStep,
    isEditing: Boolean,
    instructionDraft: String,
    @StringRes errorMessage: Int?,
    onInstructionDraftChange: (String) -> Unit,
    onStartEditing: () -> Unit,
    onConfirmEditing: () -> Unit,
    onCancelEditing: () -> Unit,
    onToggleDone: (Boolean) -> Unit,
    onDelete: () -> Unit,
    focusRequester: FocusRequester
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp)
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
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
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
                    if (errorMessage != null) {
                        Text(
                            text = stringResource(errorMessage),
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