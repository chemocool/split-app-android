package com.chemocool.split.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chemocool.split.data.LocalSettings
import com.chemocool.split.data.ThemeChoice
import com.chemocool.split.domain.Csv
import com.chemocool.split.domain.SplitMode
import com.chemocool.split.ui.AppState
import com.chemocool.split.ui.components.Avatar
import com.chemocool.split.ui.components.SectionTitle
import java.time.LocalDate

/** Écran 08 : Paramètres (onglet Profil). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: AppState.Ready,
    settings: LocalSettings,
    contentPadding: PaddingValues,
    onRenameHousehold: (String) -> Unit,
    onDefaultSplit: (SplitMode) -> Unit,
    onTheme: (ThemeChoice) -> Unit,
    onNotifyNewExpense: (Boolean) -> Unit,
    onNotifyMonthly: (Boolean) -> Unit,
    onMessage: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    val me = state.me
    val partner = state.partner
    var renaming by remember { mutableStateOf(false) }
    var splitMenu by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            val csv = Csv.export(state.household, state.expenses, state.settlements)
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())) // BOM pour Excel
                    out.write(csv.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess { onMessage("Export CSV enregistré") }.onFailure { onMessage("Export impossible") }
        }
    }

    Column(Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState())) {
        TopAppBar(title = { Text("Paramètres") })

        ListItem(
            leadingContent = { Avatar(me.initial, isMe = true, size = 48.dp) },
            headlineContent = { Text(me.name) },
            supportingContent = { Text(me.email.ifBlank { "Compte Google" }) },
        )
        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        SectionTitle("Foyer")
        ListItem(
            leadingContent = { Icon(Icons.Outlined.Favorite, null) },
            headlineContent = { Text("Partenaire") },
            supportingContent = { Text(partner?.let { "${it.name}${if (it.email.isNotBlank()) " · ${it.email}" else ""}" } ?: "En attente · code ${state.household.inviteCode}") },
        )
        ListItem(
            modifier = Modifier.tappable { renaming = true },
            leadingContent = { Icon(Icons.Outlined.Home, null) },
            headlineContent = { Text("Nom du foyer") },
            supportingContent = { Text(state.household.name) },
            trailingContent = { Icon(Icons.Outlined.Edit, "Modifier") },
        )
        ListItem(
            modifier = Modifier.tappable { splitMenu = true },
            leadingContent = { Icon(Icons.Outlined.CallSplit, null) },
            headlineContent = { Text("Partage par défaut") },
            supportingContent = {
                Text(state.household.defaultSplit.label)
                DropdownMenu(splitMenu, onDismissRequest = { splitMenu = false }) {
                    SplitMode.entries.forEach { m ->
                        DropdownMenuItem(text = { Text(m.label) }, onClick = { onDefaultSplit(m); splitMenu = false })
                    }
                }
            },
        )
        ListItem(
            leadingContent = { Icon(Icons.Outlined.AttachMoney, null) },
            headlineContent = { Text("Devise") },
            supportingContent = { Text("Dollar canadien (CAD)") },
        )
        if (partner == null) {
            ListItem(
                leadingContent = { Icon(Icons.Outlined.Key, null) },
                headlineContent = { Text("Code d'invitation") },
                supportingContent = { Text(state.household.inviteCode) },
            )
        }

        SectionTitle("Notifications")
        ListItem(
            headlineContent = { Text("Nouvelle dépense") },
            supportingContent = { Text("Quand votre partenaire ajoute une dépense") },
            trailingContent = { Switch(settings.notifyNewExpense, onNotifyNewExpense) },
        )
        ListItem(
            headlineContent = { Text("Rappel mensuel") },
            supportingContent = { Text("Un rappel pour régler le solde en fin de mois") },
            trailingContent = { Switch(settings.notifyMonthlyReminder, onNotifyMonthly) },
        )

        SectionTitle("Apparence")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            ThemeChoice.entries.forEachIndexed { i, t ->
                SegmentedButton(
                    selected = settings.theme == t,
                    onClick = { onTheme(t) },
                    shape = SegmentedButtonDefaults.itemShape(i, ThemeChoice.entries.size),
                ) { Text(t.label) }
            }
        }

        SectionTitle("Données", Modifier.padding(top = 8.dp))
        ListItem(
            modifier = Modifier.tappable { exportLauncher.launch("split-${LocalDate.now()}.csv") },
            leadingContent = { Icon(Icons.Outlined.Download, null) },
            headlineContent = { Text("Exporter en CSV") },
            supportingContent = { Text("Toutes les dépenses et tous les virements") },
        )
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        ListItem(
            modifier = Modifier.tappable { confirmSignOut = true },
            leadingContent = { Icon(Icons.AutoMirrored.Outlined.Logout, null, tint = MaterialTheme.colorScheme.error) },
            headlineContent = { Text("Se déconnecter", color = MaterialTheme.colorScheme.error) },
        )
    }

    if (renaming) {
        var name by remember { mutableStateOf(state.household.name) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Nom du foyer") },
            text = { OutlinedTextField(name, { name = it }, singleLine = true) },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = { onRenameHousehold(name.trim()); renaming = false }) { Text("Enregistrer") }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Annuler") } },
        )
    }
    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Se déconnecter ?") },
            text = { Text("Vos dépenses restent enregistrées. Vous les retrouverez en vous reconnectant.") },
            confirmButton = { TextButton(onClick = { confirmSignOut = false; onSignOut() }) { Text("Se déconnecter") } },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Annuler") } },
        )
    }
}
