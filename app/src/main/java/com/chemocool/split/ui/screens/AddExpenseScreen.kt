package com.chemocool.split.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chemocool.split.domain.Category
import com.chemocool.split.domain.Expense
import com.chemocool.split.domain.Money
import com.chemocool.split.domain.SplitMode
import com.chemocool.split.domain.Splits
import com.chemocool.split.ui.AppState
import com.chemocool.split.ui.components.Avatar
import com.chemocool.split.ui.components.icon
import com.chemocool.split.ui.components.short
import com.chemocool.split.ui.components.splitHint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Écrans 03 à 05 : Nouvelle dépense (partage Égal / En $ / Totalité). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseScreen(state: AppState.Ready, busy: Boolean, onClose: () -> Unit, onSave: (Expense) -> Unit) {
    val me = state.me
    val partner = state.partner ?: return
    var description by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(Category.GROCERIES) }
    var date by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var note by rememberSaveable { mutableStateOf("") }
    var showNote by rememberSaveable { mutableStateOf(false) }
    var payerUid by rememberSaveable { mutableStateOf(me.uid) }
    var mode by rememberSaveable { mutableStateOf(state.household.defaultSplit) }
    var otherAmountText by rememberSaveable { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    val amount = Money.parse(amountText)
    val otherUid = if (payerUid == me.uid) partner.uid else me.uid
    val otherName = if (otherUid == me.uid) "Vous" else partner.firstName
    val payerName = if (payerUid == me.uid) "vous" else partner.firstName
    val shares = amount?.let { Splits.compute(it, mode, payerUid, otherUid, Money.parse(otherAmountText)) }
    val canSave = description.isNotBlank() && shares != null && !busy

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouvelle dépense") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "Fermer") } },
                actions = {
                    TextButton(enabled = canSave, onClick = {
                        onSave(
                            Expense(
                                description = description.trim(),
                                amountCents = amount!!,
                                category = category,
                                date = date,
                                note = note.trim(),
                                paidBy = payerUid,
                                splitMode = mode,
                                shares = shares!!,
                            ),
                        )
                    }) { Text("Enregistrer") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                description, { description = it },
                label = { Text("Description") },
                placeholder = { Text("Épicerie Metro") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            TextField(
                value = amountText,
                onValueChange = { v -> amountText = v.filter { it.isDigit() || it == ',' || it == '.' } },
                placeholder = { Text("0,00", style = bigAmount(), modifier = Modifier.fillMaxWidth()) },
                suffix = { Text(" $", style = bigAmount()) },
                textStyle = bigAmount(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.entries.filter { it != Category.OTHER }.forEach { c ->
                    FilterChip(
                        selected = category == c,
                        onClick = { category = c },
                        label = { Text(c.label) },
                        leadingIcon = { Icon(c.icon(), null, Modifier.size(18.dp)) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { showDatePicker = true },
                    label = { Text(date.short()) },
                    leadingIcon = { Icon(Icons.Outlined.CalendarToday, null, Modifier.size(18.dp)) },
                )
                AssistChip(
                    onClick = { showNote = !showNote },
                    label = { Text(if (note.isBlank()) "Ajouter une note" else "Note") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Notes, null, Modifier.size(18.dp)) },
                )
            }
            if (showNote) {
                OutlinedTextField(note, { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(16.dp))
            Text("Payé par", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(me.uid to "Vous", partner.uid to partner.firstName).forEachIndexed { i, (uid, label) ->
                    SegmentedButton(
                        selected = payerUid == uid,
                        onClick = { payerUid = uid },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                        icon = { Avatar(label.take(1).uppercase(), isMe = uid == me.uid, size = 24.dp) },
                    ) { Text(label) }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Partage", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SplitMode.entries.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = mode == m,
                        onClick = { mode = m },
                        shape = SegmentedButtonDefaults.itemShape(i, SplitMode.entries.size),
                    ) { Text(m.label) }
                }
            }
            Text(
                splitHint(mode, otherName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (mode == SplitMode.AMOUNT) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    otherAmountText,
                    { v -> otherAmountText = v.filter { it.isDigit() || it == ',' || it == '.' } },
                    label = { Text(if (otherUid == me.uid) "Votre part" else "Part de ${partner.firstName}") },
                    suffix = { Text("$") },
                    isError = amount != null && otherAmountText.isNotBlank() && shares == null,
                    supportingText = { if (amount != null) Text("Maximum ${Money.format(amount)}") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(16.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    if (shares == null) {
                        Text("Entrez un montant pour voir la répartition.")
                    } else {
                        val owed = shares[otherUid] ?: 0
                        val sentence = when {
                            owed == 0L -> "Personne ne doit rien sur cette dépense."
                            otherUid == me.uid -> "Vous devrez ${Money.format(owed)} à ${partner.firstName}"
                            else -> "${partner.firstName} vous devra ${Money.format(owed)}"
                        }
                        Text(sentence, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Part de $payerName : ${Money.format(shares[payerUid] ?: 0)} · Part de ${otherName.replaceFirstChar { it.lowercase() }.takeIf { otherUid == me.uid } ?: otherName} : ${Money.format(owed)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Annuler") } },
        ) { DatePicker(pickerState) }
    }
}

@Composable
private fun bigAmount() = TextStyle(
    fontSize = 44.sp,
    fontWeight = FontWeight.Medium,
    textAlign = TextAlign.Center,
    color = MaterialTheme.colorScheme.onSurface,
)
