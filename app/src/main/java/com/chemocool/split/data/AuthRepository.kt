package com.chemocool.split.data

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Connexion avec le compte Google, via Credential Manager + Firebase Auth. */
class AuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {

    val currentUser: FirebaseUser? get() = auth.currentUser

    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Ouvre le sélecteur de compte Google. [activityContext] doit être une Activity.
     * L'identifiant client web vient de google-services.json (ressource default_web_client_id).
     */
    suspend fun signInWithGoogle(activityContext: Context): FirebaseUser {
        val webClientId = webClientId(activityContext)
            ?: error("google-services.json manquant : voir le README pour configurer Firebase.")
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = CredentialManager.create(activityContext).getCredential(activityContext, request)
        val credential = result.credential
        require(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Type d'identifiant inattendu"
        }
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authResult = auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        return authResult.user ?: error("Connexion impossible")
    }

    suspend fun signOut(context: Context) {
        auth.signOut()
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
    }

    private fun webClientId(context: Context): String? {
        // Recherche dynamique : la ressource n'existe que si google-services.json est présent.
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (id != 0) context.getString(id) else null
    }
}
