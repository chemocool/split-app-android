package com.chemocool.split.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chemocool.split.domain.Category
import com.chemocool.split.domain.Expense
import com.chemocool.split.domain.Household
import com.chemocool.split.domain.Money
import com.chemocool.split.domain.Settlement
import com.chemocool.split.domain.SplitMode
import com.chemocool.split.ui.theme.LocalSplitColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val FrCa: Locale = Locale.CANADA_FRENCH
private val shortDate = DateTimeFormatter.ofPattern("d MMM", FrCa)
private val longDate = DateTimeFormatter.ofPattern("d MMMM yyyy", FrCa)

fun LocalDate.short(): String = if (this == LocalDate.now()) "Aujourd'hui" else format(shortDate)
fun LocalDate.long(): String = format(longDate)

fun Category.icon(): ImageVector = when (this) {
    Category.GROCERIES -> Icons.Outlined.ShoppingCart
    Category.RESTAURANTS -> Icons.Outlined.Restaurant
    Category.HOME -> Icons.Outlined.Home
    Category.BILLS -> Icons.Outlined.ReceiptLong
    Category.LEISURE -> Icons.Outlined.Celebration
    Category.OTHER -> Icons.Outlined.Receipt
}

/** Pastille avec l'initiale : sarcelle pour moi, rose pour l'autre personne. */
@Composable
fun Avatar(initial: String, isMe: Boolean, size: Dp = 40.dp) {
    val bg = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
    val fg = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
    Box(
        Modifier.size(size).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initial,
            color = fg,
            style = if (size >= 48.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun CategoryBadge(category: Category) {
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(category.icon(), contentDescription = category.label, tint = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

/** Ligne de dépense : « Payé par Alexandra · Égal » et ce que ça change pour moi. */
@Composable
fun ExpenseRow(expense: Expense, meUid: String, household: Household, modifier: Modifier = Modifier) {
    val payer = household.member(expense.paidBy)
    val payerName = if (expense.paidBy == meUid) "vous" else payer?.firstName ?: "?"
    val colors = LocalSplitColors.current
    val impact: Long = if (expense.paidBy == meUid) expense.amountCents - (expense.shares[meUid] ?: 0)
    else -(expense.shares[meUid] ?: 0)
    ListItem(
        modifier = modifier,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { CategoryBadge(expense.category) },
        headlineContent = { Text(expense.description, maxLines = 1) },
        supportingContent = {
            Text("Payé par $payerName · ${expense.splitMode.label} · ${expense.date.short()}", maxLines = 1)
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(Money.format(expense.amountCents), style = MaterialTheme.typography.titleSmall)
                if (impact != 0L) {
                    Text(
                        if (impact > 0) "on vous doit ${Money.format(impact)}" else "vous devez ${Money.format(-impact)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (impact > 0) colors.positive else colors.negative,
                    )
                }
            }
        },
    )
}

@Composable
fun SettlementRow(s: Settlement, meUid: String, household: Household, modifier: Modifier = Modifier) {
    val from = if (s.fromUid == meUid) "Vous" else household.member(s.fromUid)?.firstName ?: "?"
    val to = if (s.toUid == meUid) "vous" else household.member(s.toUid)?.firstName ?: "?"
    ListItem(
        modifier = modifier,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.SyncAlt, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
        },
        headlineContent = { Text("$from → $to") },
        supportingContent = { Text("Virement Interac · ${s.date.short()}") },
        trailingContent = { Text(Money.format(s.amountCents), style = MaterialTheme.typography.titleSmall) },
    )
}

/** Barre de répartition « qui a payé quoi » du mois. */
@Composable
fun SplitBar(meCents: Long, partnerCents: Long, modifier: Modifier = Modifier) {
    val total = (meCents + partnerCents).coerceAtLeast(1)
    val meFraction = meCents.toFloat() / total
    Row(modifier.clip(RoundedCornerShape(6.dp))) {
        if (meCents > 0) Box(Modifier.weight(meFraction.coerceAtLeast(0.01f)).height(12.dp).background(MaterialTheme.colorScheme.primary))
        if (meCents > 0 && partnerCents > 0) Spacer(Modifier.width(3.dp))
        if (partnerCents > 0) Box(Modifier.weight((1 - meFraction).coerceAtLeast(0.01f)).height(12.dp).background(MaterialTheme.colorScheme.tertiary))
        if (meCents == 0L && partnerCents == 0L) Box(Modifier.weight(1f).height(12.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest))
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(24.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/** Libellé du solde du point de vue de l'utilisateur. */
fun balanceHeadline(net: Long, partnerName: String): String = when {
    net > 0 -> "$partnerName vous doit"
    net < 0 -> "Vous devez à $partnerName"
    else -> "Vous êtes quittes"
}

fun splitHint(mode: SplitMode, partnerName: String): String = when (mode) {
    SplitMode.EQUAL -> "Chacun paie la moitié"
    SplitMode.AMOUNT -> "Indiquez ce que $partnerName doit"
    SplitMode.FULL -> "$partnerName rembourse la totalité"
}

@Composable
fun Labeled(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}
