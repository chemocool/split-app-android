package com.chemocool.split.domain

import java.time.LocalDate

/** Une personne du foyer (Guillaume ou Alexandra). */
data class Member(
    val uid: String,
    val name: String,
    val email: String = "",
    val photoUrl: String? = null,
) {
    val firstName: String get() = name.trim().split(" ").firstOrNull().orEmpty().ifEmpty { "?" }
    val initial: String get() = firstName.take(1).uppercase()
}

/** Comment une dépense est partagée. Pas de partage en pourcentage. */
enum class SplitMode(val label: String) {
    /** Moitié-moitié. */
    EQUAL("Égal"),

    /** Montant précis dû par l'autre personne. */
    AMOUNT("En $"),

    /** L'autre personne rembourse la totalité. */
    FULL("Totalité");
}

enum class Category(val label: String) {
    GROCERIES("Épicerie"),
    RESTAURANTS("Restos"),
    HOME("Maison"),
    BILLS("Factures"),
    LEISURE("Loisirs"),
    OTHER("Autre");

    companion object {
        fun fromName(name: String?): Category = entries.firstOrNull { it.name == name } ?: OTHER
    }
}

data class Expense(
    val id: String = "",
    val description: String,
    val amountCents: Long,
    val category: Category,
    val date: LocalDate,
    val note: String = "",
    val paidBy: String,
    val splitMode: SplitMode,
    /** Part de chaque personne, en cents. La somme vaut amountCents. */
    val shares: Map<String, Long>,
    val createdAtMillis: Long = 0,
) {
    /** Ce que la personne qui n'a pas payé doit à celle qui a payé. */
    fun owedBy(uid: String): Long = if (uid == paidBy) 0 else shares[uid] ?: 0
}

/** Un remboursement par virement Interac. */
data class Settlement(
    val id: String = "",
    val fromUid: String,
    val toUid: String,
    val amountCents: Long,
    val date: LocalDate,
    val createdAtMillis: Long = 0,
)

data class Household(
    val id: String,
    val name: String,
    val members: List<Member>,
    val inviteCode: String,
    val defaultSplit: SplitMode = SplitMode.EQUAL,
    val currency: String = "CAD",
) {
    fun partnerOf(uid: String): Member? = members.firstOrNull { it.uid != uid }
    fun member(uid: String): Member? = members.firstOrNull { it.uid == uid }
}
