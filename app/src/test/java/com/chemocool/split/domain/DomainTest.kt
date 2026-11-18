package com.chemocool.split.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class MoneyTest {
    @Test fun formatsCanadianFrench() {
        assertEquals("46,25 $", Money.format(4625))
        assertEquals("1 234,05 $", Money.format(123405))
        assertEquals("0,00 $", Money.format(0))
        assertEquals("−12,50 $", Money.format(-1250))
        assertEquals("+12,50 $", Money.format(1250, withSign = true))
    }

    @Test fun parsesUserInput() {
        assertEquals(4625L, Money.parse("46,25"))
        assertEquals(4625L, Money.parse("46.25"))
        assertEquals(4600L, Money.parse("46"))
        assertEquals(4650L, Money.parse("46,5"))
        assertEquals(123405L, Money.parse("1 234,05 $"))
        assertNull(Money.parse(""))
        assertNull(Money.parse("abc"))
        assertNull(Money.parse("1,234"))
    }
}

class SplitsTest {
    @Test fun equalSplitGivesOddCentToPayer() {
        assertEquals(mapOf("g" to 2313L, "a" to 2312L), Splits.compute(4625, SplitMode.EQUAL, "g", "a"))
    }

    @Test fun amountSplit() {
        assertEquals(mapOf("g" to 3000L, "a" to 1625L), Splits.compute(4625, SplitMode.AMOUNT, "g", "a", 1625))
        assertNull(Splits.compute(4625, SplitMode.AMOUNT, "g", "a", 5000))
        assertNull(Splits.compute(4625, SplitMode.AMOUNT, "g", "a", null))
    }

    @Test fun fullSplitOtherPaysEverything() {
        assertEquals(mapOf("g" to 0L, "a" to 4625L), Splits.compute(4625, SplitMode.FULL, "g", "a"))
    }
}

class BalancesTest {
    private val day = LocalDate.of(2026, 11, 18)
    private fun expense(paidBy: String, amount: Long, mode: SplitMode, created: Long, other: Long? = null): Expense {
        val otherUid = if (paidBy == "g") "a" else "g"
        return Expense(
            id = "$created", description = "x", amountCents = amount, category = Category.GROCERIES, date = day,
            paidBy = paidBy, splitMode = mode, shares = Splits.compute(amount, mode, paidBy, otherUid, other)!!,
            createdAtMillis = created,
        )
    }

    @Test fun netBalanceFromEachSide() {
        val expenses = listOf(
            expense("g", 10000, SplitMode.EQUAL, 1),   // Alexandra doit 50 $
            expense("a", 3000, SplitMode.FULL, 2),     // Guillaume doit 30 $
        )
        assertEquals(2000L, Balances.net("g", expenses, emptyList()))
        assertEquals(-2000L, Balances.net("a", expenses, emptyList()))
    }

    @Test fun settlementClearsBalance() {
        val expenses = listOf(expense("g", 10000, SplitMode.EQUAL, 1))
        val settlements = listOf(Settlement("s", fromUid = "a", toUid = "g", amountCents = 5000, date = day, createdAtMillis = 2))
        assertEquals(0L, Balances.net("g", expenses, settlements))
        assertEquals(0L, Balances.net("a", expenses, settlements))
    }

    @Test fun totalsAndSinceLastSettlement() {
        val e1 = expense("g", 10000, SplitMode.EQUAL, 1)
        val e2 = expense("a", 4000, SplitMode.EQUAL, 3)
        val s = Settlement("s", "a", "g", 5000, day, createdAtMillis = 2)
        assertEquals(MemberTotals(10000, 7000), Balances.totals("g", listOf(e1, e2)))
        assertEquals(listOf(e2), Balances.sinceLastSettlement(listOf(e1, e2), listOf(s)))
    }

    @Test fun csvHasHeaderAndRows() {
        val h = Household("h", "Foyer", listOf(Member("g", "Guillaume"), Member("a", "Alexandra")), "ABC123")
        val csv = Csv.export(h, listOf(expense("g", 4625, SplitMode.EQUAL, 1)), emptyList())
        val lines = csv.trim().lines()
        assertEquals("Type;Date;Description;Catégorie;Montant;Payé par;Partage;Part Guillaume;Part Alexandra;Note", lines[0])
        assertEquals("Dépense;2026-11-18;x;Épicerie;46,25;Guillaume;Égal;23,13;23,12;", lines[1])
    }
}
