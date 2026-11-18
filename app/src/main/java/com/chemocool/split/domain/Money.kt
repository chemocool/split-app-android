package com.chemocool.split.domain

import kotlin.math.abs

/**
 * Les montants sont stockés en cents (Long) pour éviter les erreurs d'arrondi.
 * Affichage au format canadien-français : « 1 234,56 $ ».
 */
object Money {
    private const val NBSP = ' '

    fun format(cents: Long, withSign: Boolean = false): String {
        val sign = when {
            cents < 0 -> "−"
            withSign && cents > 0 -> "+"
            else -> ""
        }
        val absolute = abs(cents)
        val dollars = (absolute / 100).toString()
        val grouped = dollars.reversed().chunked(3).joinToString(NBSP.toString()).reversed()
        val decimals = (absolute % 100).toString().padStart(2, '0')
        return "$sign$grouped,$decimals$NBSP$"
    }

    /** Version sans le symbole $, pour les champs de saisie : « 46,25 ». */
    fun formatInput(cents: Long): String {
        val absolute = abs(cents)
        return "${absolute / 100},${(absolute % 100).toString().padStart(2, '0')}"
    }

    /**
     * Lit une saisie utilisateur (« 46,25 », « 46.25 », « 46 », « 1 234,5 $ ») et la convertit en cents.
     * Retourne null si la saisie n'est pas un montant valide.
     */
    fun parse(input: String): Long? {
        val cleaned = input.trim()
            .replace("$", "")
            .filterNot { it.isWhitespace() || it == '\u00A0' || it == '\u202F' }
            .replace(',', '.')
        if (cleaned.isEmpty()) return null
        if (!Regex("""^\d+(\.\d{0,2})?$""").matches(cleaned)) return null
        val parts = cleaned.split('.')
        val dollars = parts[0].toLongOrNull() ?: return null
        val centsPart = parts.getOrNull(1).orEmpty().padEnd(2, '0')
        return dollars * 100 + centsPart.toLong()
    }
}
