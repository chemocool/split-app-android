package com.chemocool.split.ui

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chemocool.split.domain.Expense
import com.chemocool.split.domain.Money
import com.chemocool.split.ui.components.long
import com.chemocool.split.ui.screens.ActivityScreen
import com.chemocool.split.ui.screens.AddExpenseScreen
import com.chemocool.split.ui.screens.BalancesScreen
import com.chemocool.split.ui.screens.HomeScreen
import com.chemocool.split.ui.screens.MessageScreen
import com.chemocool.split.ui.screens.OnboardingScreen
import com.chemocool.split.ui.screens.SettingsScreen
import com.chemocool.split.ui.screens.SettleUpSheet
import com.chemocool.split.ui.screens.SignInScreen

private enum class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    HOME("accueil", "Accueil", Icons.Outlined.Home, Icons.Filled.Home),
    ACTIVITY("activite", "Activité", Icons.Outlined.History, Icons.Filled.History),
    BALANCES("soldes", "Soldes", Icons.Outlined.Balance, Icons.Filled.Balance),
    PROFILE("profil", "Profil", Icons.Outlined.Person, Icons.Filled.Person),
}

private const val ADD_ROUTE = "nouvelle-depense"

@Composable
fun SplitApp(vm: AppViewModel, activity: Activity) {
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.messageShown()
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        when (val s = state) {
            AppState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            AppState.NotConfigured -> MessageScreen(
                "Firebase n'est pas configuré",
                "Ajoutez le fichier google-services.json dans le dossier app/ puis recompilez. Voir le README.",
            )
            AppState.SignedOut -> Scaffold(snackbarHost = { SnackbarHost(snackbar) }) {
                Box(Modifier.fillMaxSize()) { SignInScreen(busy) { vm.signIn(activity) } }
            }
            is AppState.NoHousehold -> Scaffold(snackbarHost = { SnackbarHost(snackbar) }) {
                Box(Modifier.fillMaxSize()) {
                    OnboardingScreen(busy, vm::createHousehold, vm::joinHousehold, vm::signOut)
                }
            }
            is AppState.Failed -> MessageScreen("Synchronisation impossible", s.message, onRetry = vm::retry)
            is AppState.Ready -> ReadyApp(s, vm, busy, snackbar)
        }
    }
}

@Composable
private fun ReadyApp(state: AppState.Ready, vm: AppViewModel, busy: Boolean, snackbar: SnackbarHostState) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val settings by vm.settings.collectAsStateWithLifecycle()
    var showSettle by remember { mutableStateOf(false) }
    var selectedExpense by remember { mutableStateOf<Expense?>(null) }
    val inTabs = route != ADD_ROUTE

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            AnimatedVisibility(inTabs) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        val selected = route == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(if (selected) tab.selectedIcon else tab.icon, null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (inTabs && (route == Tab.HOME.route || route == Tab.ACTIVITY.route) && state.partner != null) {
                ExtendedFloatingActionButton(
                    onClick = { nav.navigate(ADD_ROUTE) },
                    icon = { Icon(Icons.Outlined.Add, null) },
                    text = { Text("Nouvelle dépense") },
                )
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Tab.HOME.route) {
            composable(Tab.HOME.route) {
                HomeScreen(state, padding, onSettleUp = { showSettle = true }, onExpenseClick = { selectedExpense = it })
            }
            composable(Tab.ACTIVITY.route) {
                ActivityScreen(state, padding, onExpenseClick = { selectedExpense = it })
            }
            composable(Tab.BALANCES.route) {
                BalancesScreen(state, padding, onSettleUp = { showSettle = true })
            }
            composable(Tab.PROFILE.route) {
                SettingsScreen(
                    state, settings, padding,
                    onRenameHousehold = vm::renameHousehold,
                    onDefaultSplit = vm::setDefaultSplit,
                    onTheme = { vm.setTheme(it) },
                    onNotifyNewExpense = { vm.setNotifyNewExpense(it) },
                    onNotifyMonthly = { vm.setNotifyMonthly(it) },
                    onMessage = vm::showMessage,
                    onAddTestPartner = vm::addTestPartner,
                    onRemoveTestPartner = vm::removeTestPartner,
                    onSignOut = vm::signOut,
                )
            }
            composable(ADD_ROUTE) {
                AddExpenseScreen(state, busy, onClose = { nav.popBackStack() }) { expense ->
                    vm.addExpense(expense) { nav.popBackStack() }
                }
            }
        }
    }

    if (showSettle) {
        SettleUpSheet(state, busy, onDismiss = { showSettle = false }) { from, to, amount, date ->
            vm.recordSettlement(from, to, amount, date) { showSettle = false }
        }
    }

    selectedExpense?.let { e ->
        val payer = if (e.paidBy == state.me.uid) "Vous" else state.household.member(e.paidBy)?.firstName ?: "?"
        AlertDialog(
            onDismissRequest = { selectedExpense = null },
            title = { Text(e.description) },
            text = {
                Column {
                    Text("${Money.format(e.amountCents)} · ${e.category.label}")
                    Text("Payé par $payer le ${e.date.long()}")
                    Text("Partage : ${e.splitMode.label}")
                    state.household.members.forEach { m ->
                        Text("Part de ${if (m.uid == state.me.uid) "vous" else m.firstName} : ${Money.format(e.shares[m.uid] ?: 0)}")
                    }
                    if (e.note.isNotBlank()) Text("Note : ${e.note}")
                }
            },
            confirmButton = { TextButton(onClick = { selectedExpense = null }) { Text("Fermer") } },
            dismissButton = {
                TextButton(onClick = { vm.deleteExpense(e); selectedExpense = null }) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }
}
