package com.chemocool.split.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chemocool.split.domain.Balances
import com.chemocool.split.domain.Money
import com.chemocool.split.ui.AppState
import com.chemocool.split.ui.components.Avatar
import com.chemocool.split.ui.components.EmptyState
import com.chemocool.split.ui.components.SectionTitle
import com.chemocool.split.ui.components.SettlementRow
import com.chemocool.split.ui.theme.LocalSplitColors
import kotlin.math.abs

/** Écran 06 : Soldes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalancesScreen(state: AppState.Ready, contentPadding: PaddingValues, onSettleUp: () -> Unit) {
    val me = state.me
    val partner = state.partner
    val net = Balances.net(me.uid, state.expenses, state.settlements)
    val colors = LocalSplitColors.current

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item { TopAppBar(title = { Text("Soldes") }) }
        if (partner == null) {
            item { EmptyState("Les soldes apparaîtront quand votre partenaire aura rejoint le foyer.", Modifier.fillMaxWidth()) }
            return@LazyColumn
        }
        item {
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val (debtor, creditor) = if (net < 0) me to partner else partner to me
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(debtor.initial, isMe = debtor.uid == me.uid, size = 48.dp)
                        Text("  →  ", style = MaterialTheme.typography.titleLarge)
                        Avatar(creditor.initial, isMe = creditor.uid == me.uid, size = 48.dp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        when {
                            net > 0 -> "${partner.firstName} vous doit"
                            net < 0 -> "Vous devez à ${partner.firstName}"
                            else -> "Vous êtes quittes"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        Money.format(abs(net)),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            net > 0 -> colors.positive
                            net < 0 -> colors.negative
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    )
                    if (net != 0L) {
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onSettleUp) { Text("Régler le solde") }
                    }
                }
            }
        }

        item { SectionTitle("Détail depuis le début") }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                TableRow("", "Payé", "Part", "Solde", header = true)
                HorizontalDivider()
                listOf(me, partner).forEach { m ->
                    val t = Balances.totals(m.uid, state.expenses)
                    TableRow(
                        if (m.uid == me.uid) "Vous" else m.firstName,
                        Money.format(t.paidCents),
                        Money.format(t.shareCents),
                        Money.format(t.balanceCents, withSign = true),
                    )
                }
                Text(
                    "Les virements déjà faits ne sont pas inclus dans ce tableau.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }

        item { SectionTitle("Remboursements passés") }
        if (state.settlements.isEmpty()) item { EmptyState("Aucun virement enregistré.", Modifier.fillMaxWidth()) }
        items(state.settlements, key = { it.id }) { SettlementRow(it, me.uid, state.household) }
    }
}

@Composable
private fun TableRow(name: String, paid: String, share: String, balance: String, header: Boolean = false) {
    val style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium
    val color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name, Modifier.weight(1f), style = style, color = color)
        Spacer(Modifier.width(4.dp))
        listOf(paid, share, balance).forEach {
            Text(it, Modifier.weight(1.2f), style = style, color = color, textAlign = TextAlign.End)
        }
    }
}
