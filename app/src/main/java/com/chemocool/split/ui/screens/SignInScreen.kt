package com.chemocool.split.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Logo : un cercle coupé en deux moitiés. */
@Composable
fun SplitLogo(size: Int = 96) {
    val left = MaterialTheme.colorScheme.primary
    val right = MaterialTheme.colorScheme.tertiary
    Canvas(Modifier.size(size.dp)) {
        val gap = this.size.width * 0.04f
        val s = Size(this.size.width - gap, this.size.height)
        drawArc(left, 90f, 180f, true, topLeft = Offset(0f, 0f), size = s)
        drawArc(right, -90f, 180f, true, topLeft = Offset(gap, 0f), size = s)
    }
}

/** Écran 01 : Connexion. */
@Composable
fun SignInScreen(busy: Boolean, onGoogle: () -> Unit) {
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.weight(1f))
        SplitLogo()
        Spacer(Modifier.height(24.dp))
        Text("Split", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Partagez vos dépenses à deux, sans calculs ni malaise.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        Button(onClick = onGoogle, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Continuer avec Google")
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Vos données sont partagées uniquement avec votre partenaire.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Après la première connexion : créer le foyer, ou rejoindre celui de l'autre avec son code. */
@Composable
fun OnboardingScreen(busy: Boolean, onCreate: (String) -> Unit, onJoin: (String) -> Unit, onSignOut: () -> Unit) {
    var name by remember { mutableStateOf("Notre foyer") }
    var code by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { SplitLogo(64) }
        Spacer(Modifier.height(24.dp))
        Text("Créez votre foyer", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Une seule personne crée le foyer, puis envoie le code d'invitation à l'autre.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(name, { name = it }, label = { Text("Nom du foyer") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Button(onClick = { onCreate(name) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Créer le foyer") }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        Text("Vous avez reçu un code ?", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            code,
            { code = it.uppercase().take(6) },
            label = { Text("Code d'invitation") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { onJoin(code) }, enabled = !busy && code.length == 6, modifier = Modifier.fillMaxWidth()) {
            Text("Rejoindre le foyer")
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onSignOut, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Se déconnecter") }
    }
}

@Composable
fun MessageScreen(title: String, body: String, onRetry: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SplitLogo(64)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (onRetry != null) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry) { Text("Réessayer") }
        }
    }
}
