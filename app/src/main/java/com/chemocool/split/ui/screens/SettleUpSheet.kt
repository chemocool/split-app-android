package com.chemocool.split.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chemocool.split.domain.Balances
import com.chemocool.split.domain.Money
import com.chemocool.split.ui.AppState
import com.chemocool.split.ui.components.Avatar
import com.chemocool.split.ui.components.short
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs

/** Écran 07 : Régler le solde. Toujours par virement Interac, sans choix de mode de paiement. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleUpSheet(
    state: AppState.Ready,
    busy: Boolean,
    onDismiss: () -> Unit,
    onRecord: (fromUid: String, toUid: String, amountCents: Long, date: LocalDate) -> Unit,
) {
    val me = state.me
    val partner = state.partner ?: return
    val net = Balances.net(me.uid, state.expenses, state.settlements)
    val fromUid = if (net < 0) me.uid else partner.uid
    val toUid = if (net < 0) partner.uid else me.uid
    var amountText by remember { mutableStateOf(Money.formatInput(abs(net))) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val amount = Money.parse(amountText)?.takeIf { it > 0 }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding()) {
            Text("Régler le solde", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(if (fromUid == me.uid) me.initial else partner.initial, isMe = fromUid == me.uid)
                Text("  →  ")
                Avatar(if (toUid == me.uid) me.initial else partner.initial, isMe = toUid == me.uid)
                Spacer(Modifier.width(12.dp))
                Text(
                    if (fromUid == me.uid) "Vous remboursez ${partner.firstName}" else "${partner.firstName} vous rembourse",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                amountText,
                { v -> amountText = v.filter { it.isDigit() || it == ',' || it == '.' } },
                label = { Text("Montant") },
                suffix = { Text("$") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            AssistChip(
                onClick = { showDatePicker = true },
                label = { Text(date.short()) },
                leadingIcon = { Icon(Icons.Outlined.CalendarToday, null, Modifier.size(18.dp)) },
            )
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AccountBalance, null)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Virement Interac", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Faites le virement depuis votre app bancaire, puis enregistrez-le ici.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { amount?.let { onRecord(fromUid, toUid, it, date) } },
                enabled = amount != null && !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Enregistrer le virement de ${amount?.let { Money.format(it) } ?: "—"}") }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Annuler") } },
        ) { DatePicker(pickerState) }
    }
}
