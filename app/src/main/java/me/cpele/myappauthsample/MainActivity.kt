package me.cpele.myappauthsample

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import net.openid.appauth.*
import java.io.InputStreamReader
import java.net.URL

class MainActivity : AppCompatActivity() {

    private companion object {
        private const val CLIENT_ID = "511828570984-fuprh0cm7665emlne3rnf9pk34kkn86s.apps.googleusercontent.com"
    }

    private lateinit var authorizationService: AuthorizationService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mainButtonGo.setOnClickListener {
            auth()
        }
        authorizationService = AuthorizationService(applicationContext)
    }

    override fun onStart() {
        super.onStart()
        val response = AuthorizationResponse.fromIntent(intent)
        val error = AuthorizationException.fromIntent(intent)

        response?.apply {
            Log.d(localClassName, jsonSerializeString())
            val authState = AuthState(response, error)
            val request = createTokenExchangeRequest()
            authorizationService.performTokenRequest(request) { response, tokenException ->
                authState.apply {
                    update(response, tokenException)
                }.apply {
                    performActionWithFreshTokens(authorizationService) { accessToken, idToken, _ ->

                        Thread {
                            val url = "https://www.googleapis.com/oauth2/v3/userinfo"
                            val connection = URL(url).openConnection()
                            connection.setRequestProperty("Authorization", "Bearer $accessToken")
                            val inputStream = connection.getInputStream()
                            val inputStreamReader = InputStreamReader(inputStream)
                            val strResponse = inputStreamReader.readText()
                            inputStreamReader.close()

                            AlertDialog.Builder(this@MainActivity)
                                    .setMessage("Here is your response: $strResponse")
                                    .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                                    .show()
                        }.start()
                    }
                }
            }
        }
    }

    private fun auth() {

        val redirectUri = Uri.parse("com.googleusercontent.apps.511828570984-fuprh0cm7665emlne3rnf9pk34kkn86s:/oauth2redirect")

        val configuration = AuthorizationServiceConfiguration(
                Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"),
                Uri.parse("https://www.googleapis.com/oauth2/v4/token")
        )

        val request = AuthorizationRequest.Builder(
                configuration,
                CLIENT_ID,
                ResponseTypeValues.CODE,
                redirectUri
        ).setScope("profile").build()

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, 0)

        authorizationService.performAuthorizationRequest(request, pendingIntent)
    }
}
