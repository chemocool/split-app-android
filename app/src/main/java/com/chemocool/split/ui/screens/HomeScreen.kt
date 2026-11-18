package com.chemocool.split.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chemocool.split.domain.Balances
import com.chemocool.split.domain.Money
import com.chemocool.split.ui.AppState
import com.chemocool.split.ui.components.Avatar
import com.chemocool.split.ui.components.EmptyState
import com.chemocool.split.ui.components.ExpenseRow
import com.chemocool.split.ui.components.FrCa
import com.chemocool.split.ui.components.SectionTitle
import com.chemocool.split.ui.components.SplitBar
import com.chemocool.split.ui.components.balanceHeadline
import com.chemocool.split.ui.theme.LocalSplitColors
import java.time.YearMonth
import java.time.format.TextStyle

/** Écran 02 : Accueil du couple. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(state: AppState.Ready, contentPadding: PaddingValues, onSettleUp: () -> Unit, onExpenseClick: (com.chemocool.split.domain.Expense) -> Unit) {
    val me = state.me
    val partner = state.partner
    val net = Balances.net(me.uid, state.expenses, state.settlements)
    val month = YearMonth.now()
    val monthExpenses = Balances.monthExpenses(state.expenses, month)
    val monthName = month.month.getDisplayName(TextStyle.FULL, FrCa)
    val recent = Balances.sinceLastSettlement(state.expenses, state.settlements)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item {
            TopAppBar(
                title = {
                    Column {
                        Text("Split", fontWeight = FontWeight.Medium)
                        Text(state.household.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    Row(Modifier.padding(end = 16.dp), horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                        Avatar(me.initial, isMe = true, size = 36.dp)
                        partner?.let { Avatar(it.initial, isMe = false, size = 36.dp) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        }

        if (partner == null) {
            item { InviteCard(state.household.inviteCode) }
        } else {
            item { BalanceCard(net, partner.firstName, onSettleUp) }
        }

        item {
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Dépenses de $monthName", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Money.format(monthExpenses.sumOf { it.amountCents }), style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(12.dp))
                    val mePaid = monthExpenses.filter { it.paidBy == me.uid }.sumOf { it.amountCents }
                    val partnerPaid = monthExpenses.filter { it.paidBy != me.uid }.sumOf { it.amountCents }
                    SplitBar(mePaid, partnerPaid, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Legend(MaterialTheme.colorScheme.primary, "Vous · ${Money.format(mePaid)}")
                        partner?.let { Legend(MaterialTheme.colorScheme.tertiary, "${it.firstName} · ${Money.format(partnerPaid)}") }
                    }
                }
            }
        }

        item { SectionTitle("Depuis le dernier remboursement", Modifier.padding(top = 8.dp)) }
        if (recent.isEmpty()) {
            item { EmptyState("Aucune dépense pour l'instant. Touchez « Nouvelle dépense » pour commencer.", Modifier.fillMaxWidth()) }
        }
        items(recent, key = { it.id }) { e ->
            ExpenseRow(e, me.uid, state.household, Modifier.tappable { onExpenseClick(e) })
        }
        item { Spacer(Modifier.height(88.dp)) }
    }
}

@Composable
private fun Legend(color: androidx.compose.ui.graphics.Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun BalanceCard(net: Long, partnerName: String, onSettleUp: () -> Unit) {
    val colors = LocalSplitColors.current
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(balanceHeadline(net, partnerName), style = MaterialTheme.typography.titleMedium)
            Text(
                Money.format(kotlin.math.abs(net)),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(16.dp))
            if (net != 0L) {
                Button(onClick = onSettleUp) { Text("Régler le solde") }
            } else {
                Text("Rien à régler pour le moment.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun InviteCard(code: String) {
    val context = LocalContext.current
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Invitez votre partenaire", style = MaterialTheme.typography.titleMedium)
            Text("Code d'invitation", style = MaterialTheme.typography.bodySmall)
            Text(code, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = {
                val text = "Rejoins notre foyer sur Split avec le code $code"
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null))
            }) {
                Icon(Icons.Outlined.Share, null)
                Spacer(Modifier.width(8.dp))
                Text("Envoyer le code")
            }
        }
    }
}
