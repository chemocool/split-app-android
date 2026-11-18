package com.chemocool.split.ui

import android.app.Activity
import android.app.Application
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chemocool.split.data.AuthRepository
import com.chemocool.split.data.LocalSettings
import com.chemocool.split.data.SplitRepository
import com.chemocool.split.data.SettingsStore
import com.chemocool.split.data.ThemeChoice
import com.chemocool.split.domain.Expense
import com.chemocool.split.domain.Household
import com.chemocool.split.domain.Member
import com.chemocool.split.domain.Settlement
import com.chemocool.split.domain.SplitMode
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface AppState {
    data object Loading : AppState

    /** google-services.json absent : Firebase n'est pas configuré. */
    data object NotConfigured : AppState
    data object SignedOut : AppState
    data class NoHousehold(val user: FirebaseUser) : AppState
    data class Ready(
        val me: Member,
        val household: Household,
        val expenses: List<Expense>,
        val settlements: List<Settlement>,
    ) : AppState {
        val partner: Member? get() = household.partnerOf(me.uid)
    }
    data class Failed(val message: String) : AppState
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val configured = FirebaseApp.getApps(app).isNotEmpty()
    private val auth by lazy { AuthRepository() }
    private val repo by lazy { SplitRepository() }
    private val settingsStore = SettingsStore(app)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    fun messageShown() { _message.value = null }

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    val settings: StateFlow<LocalSettings> =
        settingsStore.settings.stateIn(viewModelScope, SharingStarted.Eagerly, LocalSettings())

    private val retryTrigger = MutableStateFlow(0)

    /** Relance la synchronisation après une erreur. */
    fun retry() { retryTrigger.value += 1 }

    private fun stateFlow(): Flow<AppState> =
        (if (!configured) flowOf(AppState.NotConfigured) else auth.authState.flatMapLatest { user ->
            if (user == null) flowOf(AppState.SignedOut)
            else repo.householdIdOf(user.uid).flatMapLatest { hid ->
                if (hid == null) flowOf(AppState.NoHousehold(user))
                else combine(repo.household(hid), repo.expenses(hid), repo.settlements(hid)) { h, ex, st ->
                    if (h == null) AppState.NoHousehold(user)
                    else AppState.Ready(
                        me = h.member(user.uid) ?: Member(user.uid, user.displayName ?: "Moi"),
                        household = h,
                        expenses = ex,
                        settlements = st,
                    )
                }
            }
        }).catch { emit(AppState.Failed(it.localizedMessage ?: "Erreur de synchronisation")) }

    val state: StateFlow<AppState> =
        retryTrigger.flatMapLatest { stateFlow() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppState.Loading)

    private fun ready(): AppState.Ready? = state.value as? AppState.Ready

    private fun run(success: String? = null, block: suspend () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                block()
                success?.let { _message.value = it }
            } catch (_: GetCredentialCancellationException) {
                // L'utilisateur a fermé le sélecteur de compte : rien à signaler.
            } catch (e: Exception) {
                _message.value = e.localizedMessage ?: "Une erreur est survenue"
            } finally {
                _busy.value = false
            }
        }
    }

    // --- Compte ---
    fun signIn(activity: Activity) = run { auth.signInWithGoogle(activity) }
    fun signOut() = run { auth.signOut(getApplication()) }

    // --- Foyer ---
    fun createHousehold(name: String) = run {
        val user = auth.currentUser ?: return@run
        repo.createHousehold(user, name.ifBlank { "Notre foyer" })
    }

    fun joinHousehold(code: String) = run("Bienvenue dans le foyer !") {
        val user = auth.currentUser ?: return@run
        repo.joinHousehold(user, code)
    }

    fun setDefaultSplit(mode: SplitMode) = run {
        ready()?.let { repo.updateHousehold(it.household.id, defaultSplit = mode) }
    }

    fun addTestPartner() = run("Partenaire de test ajouté") {
        ready()?.let { repo.enableTestPartner(it.household.id) }
    }

    fun removeTestPartner() = run("Partenaire de test retiré") {
        ready()?.let { repo.disableTestPartner(it.household.id) }
    }

    fun renameHousehold(name: String) = run {
        ready()?.let { repo.updateHousehold(it.household.id, name = name) }
    }

    // --- Dépenses ---
    fun addExpense(expense: Expense, onDone: () -> Unit) = run("Dépense ajoutée") {
        val r = ready() ?: return@run
        repo.addExpense(r.household.id, expense)
        onDone()
    }

    fun deleteExpense(expense: Expense) = run("Dépense supprimée") {
        val r = ready() ?: return@run
        repo.deleteExpense(r.household.id, expense.id)
    }

    // --- Remboursement (toujours par virement Interac) ---
    fun recordSettlement(fromUid: String, toUid: String, amountCents: Long, date: LocalDate, onDone: () -> Unit) =
        run("Virement enregistré") {
            val r = ready() ?: return@run
            repo.addSettlement(r.household.id, Settlement(fromUid = fromUid, toUid = toUid, amountCents = amountCents, date = date))
            onDone()
        }

    // --- Préférences locales ---
    fun setTheme(t: ThemeChoice) = viewModelScope.launch { settingsStore.setTheme(t) }
    fun setNotifyNewExpense(v: Boolean) = viewModelScope.launch { settingsStore.setNotifyNewExpense(v) }
    fun setNotifyMonthly(v: Boolean) = viewModelScope.launch { settingsStore.setNotifyMonthly(v) }
    fun showMessage(m: String) { _message.value = m }
}
