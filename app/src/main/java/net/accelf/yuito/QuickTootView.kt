package net.accelf.yuito

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.preference.PreferenceManager
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.appstore.Event
import com.keylesspalace.tusky.appstore.PreferenceChangedEvent
import com.keylesspalace.tusky.appstore.QuickReplyEvent
import com.keylesspalace.tusky.components.compose.ComposeActivity
import com.keylesspalace.tusky.components.compose.ComposeActivity.Companion.PREF_DEFAULT_TAG
import com.keylesspalace.tusky.components.compose.ComposeActivity.Companion.PREF_USE_DEFAULT_TAG
import com.keylesspalace.tusky.databinding.ViewQuickTootBinding
import com.keylesspalace.tusky.util.ThemeUtils

class QuickTootView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewQuickTootBinding.inflate(LayoutInflater.from(context), this, true)

    private val preference by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    private lateinit var viewModel: QuickTootViewModel

    fun attachViewModel(viewModel: QuickTootViewModel, owner: LifecycleOwner) {
        this.viewModel = viewModel

        binding.buttonVisibility.attachViewModel(viewModel, owner)

        viewModel.content.observe(owner) {
            if (binding.editTextContent.text.toString() != it) {
                binding.editTextContent.setText(it)
            }
        }
        binding.editTextContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.content.value = s.toString()
            }
        })

        viewModel.inReplyTo.observe(owner) {
            binding.textQuickReply.text = it?.let { "Reply to ${it.account.username}" } ?: ""
        }

        viewModel.defaultTag.observe(owner) {
            binding.textDefaultTag.text = it?.let { "${context.getString(R.string.hint_default_text)} : $it" }
                    ?: "${context.getString(R.string.hint_default_text)} inactive"
            binding.textDefaultTag.setTextColor(ThemeUtils.getColor(context, it?.let { R.attr.colorInfo }
                    ?: android.R.attr.textColorTertiary))
        }
        syncDefaultTag()

        viewModel.visibility.observe(owner) {
            binding.buttonToot.setStatusVisibility(it)
        }
        binding.buttonToot.setOnClickListener {
            val intent = ComposeActivity.startIntent(it.context, viewModel.composeOptions(true))
            viewModel.reset()
            it.context.startActivity(intent)
        }
    }

    private fun syncDefaultTag() {
        viewModel.defaultTag.value = if (preference.getBoolean(PREF_USE_DEFAULT_TAG, false)) {
            preference.getString(PREF_DEFAULT_TAG, null)
        } else {
            null
        }
    }

    fun onFABClicked(view: View) {
        val intent = ComposeActivity.startIntent(view.context, viewModel.composeOptions(false))
        viewModel.reset()
        view.context.startActivity(intent)
    }

    fun handleEvent(event: Event?) {
        when (event) {
            is QuickReplyEvent -> viewModel.reply(event)
            is PreferenceChangedEvent -> {
                if (event.preferenceKey in arrayOf(PREF_DEFAULT_TAG, PREF_USE_DEFAULT_TAG)) {
                    syncDefaultTag()
                }
            }
        }
    }
}
