package com.chemocool.split.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.chemocool.split.domain.Expense
import com.chemocool.split.domain.Settlement
import com.chemocool.split.ui.AppState
import com.chemocool.split.ui.components.EmptyState
import com.chemocool.split.ui.components.ExpenseRow
import com.chemocool.split.ui.components.FrCa
import com.chemocool.split.ui.components.SectionTitle
import com.chemocool.split.ui.components.SettlementRow
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** Historique complet : dépenses et virements, groupés par mois. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(state: AppState.Ready, contentPadding: PaddingValues, onExpenseClick: (Expense) -> Unit) {
    val entries: List<Pair<YearMonth, Any>> =
        (state.expenses.map { YearMonth.from(it.date) to it as Any } + state.settlements.map { YearMonth.from(it.date) to it as Any })
            .sortedWith(compareByDescending<Pair<YearMonth, Any>> { dateOf(it.second) }.thenByDescending { createdOf(it.second) })
    val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", FrCa)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item { TopAppBar(title = { Text("Activité") }) }
        if (entries.isEmpty()) item { EmptyState("Rien pour l'instant.", Modifier.fillMaxWidth()) }
        entries.groupBy { it.first }.forEach { (month, list) ->
            item(key = "m$month") { SectionTitle(month.format(monthFormat).replaceFirstChar { it.uppercase() }) }
            items(list, key = { (it.second as? Expense)?.id?.let { id -> "e$id" } ?: "s${(it.second as Settlement).id}" }) { (_, item) ->
                when (item) {
                    is Expense -> ExpenseRow(item, state.me.uid, state.household, Modifier.tappable { onExpenseClick(item) })
                    is Settlement -> SettlementRow(item, state.me.uid, state.household)
                }
            }
        }
    }
}

private fun dateOf(o: Any) = when (o) {
    is Expense -> o.date
    is Settlement -> o.date
    else -> error("type inconnu")
}

private fun createdOf(o: Any) = when (o) {
    is Expense -> o.createdAtMillis
    is Settlement -> o.createdAtMillis
    else -> 0L
}
