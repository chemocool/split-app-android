package com.chemocool.split.domain

object Csv {
    private fun esc(v: String): String =
        if (v.any { it == ';' || it == '"' || it == '\n' }) "\"" + v.replace("\"", "\"\"") + "\"" else v

    /** Export CSV (séparateur « ; » pour Excel en français). */
    fun export(household: Household, expenses: List<Expense>, settlements: List<Settlement>): String {
        val name = { uid: String -> household.member(uid)?.firstName ?: "?" }
        val sb = StringBuilder()
        sb.append("Type;Date;Description;Catégorie;Montant;Payé par;Partage")
        household.members.forEach { sb.append(";Part ").append(esc(it.firstName)) }
        sb.append(";Note\n")
        expenses.sortedBy { it.date }.forEach { e ->
            sb.append("Dépense;").append(e.date).append(';').append(esc(e.description)).append(';')
                .append(e.category.label).append(';').append(Money.formatInput(e.amountCents)).append(';')
                .append(esc(name(e.paidBy))).append(';').append(e.splitMode.label)
            household.members.forEach { sb.append(';').append(Money.formatInput(e.shares[it.uid] ?: 0)) }
            sb.append(';').append(esc(e.note)).append('\n')
        }
        settlements.sortedBy { it.date }.forEach { s ->
            sb.append("Virement Interac;").append(s.date).append(';')
                .append(esc("${name(s.fromUid)} → ${name(s.toUid)}")).append(";;")
                .append(Money.formatInput(s.amountCents)).append(';').append(esc(name(s.fromUid))).append(';')
            household.members.forEach { _ -> sb.append(';') }
            sb.append(";\n")
        }
        return sb.toString()
    }
}
