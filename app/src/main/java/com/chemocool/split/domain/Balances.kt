package com.chemocool.split.domain

import java.time.YearMonth

data class MemberTotals(
    val paidCents: Long,
    val shareCents: Long,
) {
    val balanceCents: Long get() = paidCents - shareCents
}

object Balances {
    /**
     * Solde du point de vue de [meUid] :
     * positif = l'autre me doit de l'argent, négatif = je dois de l'argent à l'autre.
     */
    fun net(meUid: String, expenses: List<Expense>, settlements: List<Settlement>): Long {
        var net = 0L
        for (e in expenses) {
            if (e.paidBy == meUid) {
                net += e.shares.filterKeys { it != meUid }.values.sum()
            } else {
                net -= e.shares[meUid] ?: 0
            }
        }
        for (s in settlements) {
            if (s.fromUid == meUid) net += s.amountCents
            if (s.toUid == meUid) net -= s.amountCents
        }
        return net
    }

    /** Total payé et part de chacun, pour le tableau de l'écran Soldes. */
    fun totals(uid: String, expenses: List<Expense>): MemberTotals = MemberTotals(
        paidCents = expenses.filter { it.paidBy == uid }.sumOf { it.amountCents },
        shareCents = expenses.sumOf { it.shares[uid] ?: 0 },
    )

    fun monthExpenses(expenses: List<Expense>, month: YearMonth): List<Expense> =
        expenses.filter { YearMonth.from(it.date) == month }

    /** Dépenses ajoutées depuis le dernier remboursement. */
    fun sinceLastSettlement(expenses: List<Expense>, settlements: List<Settlement>): List<Expense> {
        val last = settlements.maxByOrNull { it.createdAtMillis } ?: return expenses
        return expenses.filter { it.createdAtMillis > last.createdAtMillis }
    }
}
