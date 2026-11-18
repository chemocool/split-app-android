package com.chemocool.split.domain

object Splits {
    /**
     * Calcule la part de chacun.
     * @param otherAmountCents pour [SplitMode.AMOUNT] : ce que l'autre personne doit.
     * @return null si le partage est impossible (montant de l'autre supérieur au total, etc.).
     */
    fun compute(
        amountCents: Long,
        mode: SplitMode,
        payerUid: String,
        otherUid: String,
        otherAmountCents: Long? = null,
    ): Map<String, Long>? {
        if (amountCents <= 0) return null
        val otherShare = when (mode) {
            // Le cent impair reste à la charge de la personne qui paie.
            SplitMode.EQUAL -> amountCents / 2
            SplitMode.AMOUNT -> otherAmountCents?.takeIf { it in 0..amountCents } ?: return null
            SplitMode.FULL -> amountCents
        }
        return mapOf(payerUid to amountCents - otherShare, otherUid to otherShare)
    }
}
