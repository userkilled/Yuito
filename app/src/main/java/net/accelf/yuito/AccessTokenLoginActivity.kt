package net.accelf.yuito

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import at.connyduck.calladapter.networkresult.onFailure
import at.connyduck.calladapter.networkresult.onSuccess
import com.keylesspalace.tusky.MainActivity
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.databinding.ActivityAccessTokenLoginBinding
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.di.Injectable
import com.keylesspalace.tusky.network.MastodonApi
import com.keylesspalace.tusky.util.viewBinding
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class AccessTokenLoginActivity : AppCompatActivity(), Injectable {

    @Inject
    lateinit var accountManager: AccountManager

    @Inject
    lateinit var mastodonApi: MastodonApi

    private val binding by viewBinding(ActivityAccessTokenLoginBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.authorizeButton.setOnClickListener {
            it.isEnabled = false
            runBlocking { authorize() }
            it.isEnabled = true
        }

        log("Input domain and access token to login.")
    }

    private fun log(text: String) {
        runOnUiThread {
            binding.logTextView.text = String.format("%s\n%s", binding.logTextView.text.toString(), text)
        }
    }

    private suspend fun authorize() {
        if (binding.domainEditText.text.isNullOrBlank()) {
            return
        }

        val domain = binding.domainEditText.text.toString()
        val accessToken = binding.accessTokenEditText.text.toString()
        log("Starting login test. [domain: $domain, accessToken: $accessToken]")
        mastodonApi.accountVerifyCredentials(domain, auth = "Bearer $accessToken")
            .onSuccess { account ->
                log("Login successful. Moving to account registration phase.")
                accountManager.addAccount(accessToken, domain, "", "", "", account)
                log("Completed. Enjoy!")
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                overridePendingTransition(R.anim.explode, R.anim.explode)
            }
            .onFailure { e ->
                log("Login failed. ${e.message}")
                log("Aborting.")
            }
    }
}
