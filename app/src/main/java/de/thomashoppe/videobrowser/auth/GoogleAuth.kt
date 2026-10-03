package de.thomashoppe.videobrowser.auth

import android.content.Context
import android.accounts.Account
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val YOUTUBE_READONLY_SCOPE = "https://www.googleapis.com/auth/youtube.readonly"

object GoogleAuth {
    fun client(context: Context): GoogleSignInClient = GoogleSignIn.getClient(context, signInOptions())

    fun signedInEmail(context: Context): String? = GoogleSignIn.getLastSignedInAccount(context)?.email

    suspend fun accessToken(context: Context): String = withContext(Dispatchers.IO) {
        val email = GoogleSignIn.getLastSignedInAccount(context)?.email
            ?: throw IllegalStateException("Bitte zuerst mit einem Google-Konto anmelden.")
        GoogleAuthUtil.getToken(
            context,
            Account(email, GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE),
            "oauth2:$YOUTUBE_READONLY_SCOPE",
        )
    }

    private fun signInOptions() = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(YOUTUBE_READONLY_SCOPE))
        .build()
}

