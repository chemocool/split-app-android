package com.chemocool.split.data

import com.chemocool.split.domain.Category
import com.chemocool.split.domain.Expense
import com.chemocool.split.domain.Household
import com.chemocool.split.domain.Member
import com.chemocool.split.domain.Settlement
import com.chemocool.split.domain.SplitMode
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

/**
 * Données partagées du couple, synchronisées en temps réel par Firestore.
 *
 * users/{uid}                          → { householdId }
 * invites/{code}                       → { householdId }
 * households/{id}                      → nom, membres, partage par défaut, code d'invitation
 * households/{id}/expenses/{id}        → dépenses
 * households/{id}/settlements/{id}     → virements Interac
 */
class SplitRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private fun households() = db.collection("households")
    private fun expensesRef(hid: String) = households().document(hid).collection("expenses")
    private fun settlementsRef(hid: String) = households().document(hid).collection("settlements")

    fun householdIdOf(uid: String): Flow<String?> = listen(
        { cb -> db.collection("users").document(uid).addSnapshotListener { s, e -> cb(s, e) } },
    ) { it?.getString("householdId") }

    fun household(hid: String): Flow<Household?> = listen(
        { cb -> households().document(hid).addSnapshotListener { s, e -> cb(s, e) } },
    ) { it?.toHousehold() }

    fun expenses(hid: String): Flow<List<Expense>> = callbackFlow {
        val reg = expensesRef(hid).orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.documents.orEmpty().mapNotNull { it.toExpense() })
            }
        awaitClose { reg.remove() }
    }

    fun settlements(hid: String): Flow<List<Settlement>> = callbackFlow {
        val reg = settlementsRef(hid).orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.documents.orEmpty().mapNotNull { it.toSettlement() })
            }
        awaitClose { reg.remove() }
    }

    /** Crée le foyer et un code d'invitation à envoyer à l'autre personne. */
    suspend fun createHousehold(user: FirebaseUser, name: String): String {
        val doc = households().document()
        val code = newInviteCode()
        val batch = db.batch()
        batch.set(
            doc,
            mapOf(
                "name" to name,
                "memberIds" to listOf(user.uid),
                "members" to mapOf(user.uid to memberMap(user)),
                "inviteCode" to code,
                "defaultSplit" to SplitMode.EQUAL.name,
                "currency" to "CAD",
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        )
        batch.set(db.collection("invites").document(code), mapOf("householdId" to doc.id, "createdBy" to user.uid))
        batch.set(db.collection("users").document(user.uid), mapOf("householdId" to doc.id), SetOptions.merge())
        batch.commit().await()
        return doc.id
    }

    /** Rejoint le foyer de l'autre personne avec son code. */
    suspend fun joinHousehold(user: FirebaseUser, rawCode: String) {
        val code = rawCode.trim().uppercase()
        val invite = db.collection("invites").document(code).get().await()
        val hid = invite.getString("householdId") ?: throw IllegalArgumentException("Code d'invitation introuvable")
        val batch = db.batch()
        batch.update(
            households().document(hid),
            mapOf(
                "memberIds" to FieldValue.arrayUnion(user.uid),
                "members.${user.uid}" to memberMap(user),
            ),
        )
        batch.set(db.collection("users").document(user.uid), mapOf("householdId" to hid), SetOptions.merge())
        batch.commit().await()
    }

    suspend fun updateHousehold(hid: String, name: String? = null, defaultSplit: SplitMode? = null) {
        val changes = buildMap<String, Any> {
            name?.let { put("name", it) }
            defaultSplit?.let { put("defaultSplit", it.name) }
        }
        if (changes.isNotEmpty()) households().document(hid).update(changes).await()
    }

    suspend fun addExpense(hid: String, e: Expense) {
        expensesRef(hid).add(
            mapOf(
                "description" to e.description,
                "amountCents" to e.amountCents,
                "category" to e.category.name,
                "date" to e.date.toString(),
                "note" to e.note,
                "paidBy" to e.paidBy,
                "splitMode" to e.splitMode.name,
                "shares" to e.shares,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    suspend fun deleteExpense(hid: String, expenseId: String) {
        expensesRef(hid).document(expenseId).delete().await()
    }

    suspend fun addSettlement(hid: String, s: Settlement) {
        settlementsRef(hid).add(
            mapOf(
                "fromUid" to s.fromUid,
                "toUid" to s.toUid,
                "amountCents" to s.amountCents,
                "date" to s.date.toString(),
                "method" to "interac",
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    // --- Conversion ---

    private fun memberMap(user: FirebaseUser) = mapOf(
        "name" to (user.displayName ?: user.email ?: "Moi"),
        "email" to (user.email ?: ""),
        "photoUrl" to user.photoUrl?.toString(),
    )

    private fun newInviteCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // sans 0/O ni 1/I
        return (1..6).map { alphabet.random() }.joinToString("")
    }

    private fun createdAtMillis(d: DocumentSnapshot): Long =
        d.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
            ?: Long.MAX_VALUE

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toHousehold(): Household? {
        if (!exists()) return null
        val ids = get("memberIds") as? List<String> ?: emptyList()
        val members = get("members") as? Map<String, Map<String, Any?>> ?: emptyMap()
        return Household(
            id = id,
            name = getString("name").orEmpty(),
            members = ids.map { uid ->
                val m = members[uid].orEmpty()
                Member(uid, m["name"] as? String ?: "?", m["email"] as? String ?: "", m["photoUrl"] as? String)
            },
            inviteCode = getString("inviteCode").orEmpty(),
            defaultSplit = runCatching { SplitMode.valueOf(getString("defaultSplit")!!) }.getOrDefault(SplitMode.EQUAL),
            currency = getString("currency") ?: "CAD",
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toExpense(): Expense? = runCatching {
        Expense(
            id = id,
            description = getString("description").orEmpty(),
            amountCents = getLong("amountCents") ?: 0,
            category = Category.fromName(getString("category")),
            date = LocalDate.parse(getString("date")),
            note = getString("note").orEmpty(),
            paidBy = getString("paidBy").orEmpty(),
            splitMode = SplitMode.valueOf(getString("splitMode") ?: SplitMode.EQUAL.name),
            shares = (get("shares") as? Map<String, Number>).orEmpty().mapValues { it.value.toLong() },
            createdAtMillis = createdAtMillis(this),
        )
    }.getOrNull()

    private fun DocumentSnapshot.toSettlement(): Settlement? = runCatching {
        Settlement(
            id = id,
            fromUid = getString("fromUid").orEmpty(),
            toUid = getString("toUid").orEmpty(),
            amountCents = getLong("amountCents") ?: 0,
            date = LocalDate.parse(getString("date")),
            createdAtMillis = createdAtMillis(this),
        )
    }.getOrNull()

    private fun <T> listen(
        register: ((DocumentSnapshot?, Exception?) -> Unit) -> ListenerRegistration,
        map: (DocumentSnapshot?) -> T,
    ): Flow<T> = callbackFlow {
        val reg = register { snap, err ->
            if (err != null) close(err) else trySend(map(snap))
        }
        awaitClose { reg.remove() }
    }
}
