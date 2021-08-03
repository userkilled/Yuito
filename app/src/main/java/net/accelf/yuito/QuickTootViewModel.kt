package net.accelf.yuito

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.keylesspalace.tusky.components.compose.ComposeActivity
import com.keylesspalace.tusky.components.compose.ComposeActivity.Companion.CAN_USE_UNLEAKABLE
import com.keylesspalace.tusky.components.compose.mutableLiveData
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.entity.Status
import com.keylesspalace.tusky.entity.Status.Visibility
import javax.inject.Inject

class QuickTootViewModel @Inject constructor(
        accountManager: AccountManager
): ViewModel() {

    private val account = accountManager.activeAccount!!

    private val unleakableAllowed by lazy { CAN_USE_UNLEAKABLE.contains(account.domain) }

    val content = mutableLiveData("")

    private val visibilityMutable = mutableLiveData(Visibility.PUBLIC)
    val visibility: LiveData<Visibility> = visibilityMutable
    private var stashedVisibility: Visibility? = null

    private val inReplyToMutable: MutableLiveData<Status?> = mutableLiveData(null)
    val inReplyTo: LiveData<Status?> = inReplyToMutable

    val defaultTag: MutableLiveData<String?> = mutableLiveData(null)

    fun setInitialVisibility(num: Int) {
        visibilityMutable.value = (Visibility.byNum(num)
                .takeUnless { it == Visibility.UNKNOWN }
                ?: account.defaultPostPrivacy)
                .takeUnless { it == Visibility.UNLEAKABLE && unleakableAllowed }
                ?: Visibility.PRIVATE
    }

    fun stepVisibility() {
        visibilityMutable.value = when (visibility.value) {
            Visibility.PUBLIC -> Visibility.UNLISTED
            Visibility.UNLISTED -> Visibility.PRIVATE
            Visibility.PRIVATE -> when (unleakableAllowed) {
                true -> Visibility.UNLEAKABLE
                false -> Visibility.PUBLIC
            }
            Visibility.UNLEAKABLE -> Visibility.PUBLIC
            else -> Visibility.PUBLIC
        }
    }

    private fun overrideVisibility(overrideTo: Visibility) {
        stashedVisibility = visibility.value
        visibilityMutable.value = overrideTo
    }

    fun reply(status: Status) {
        inReplyToMutable.value = status
        overrideVisibility(status.visibility)
    }

    fun composeOptions(tootRightNow: Boolean): ComposeActivity.ComposeOptions {
        return ComposeActivity.ComposeOptions(
                tootText = content.value,
                mentionedUsernames = inReplyTo.value
                        ?.let {
                            linkedSetOf(it.account.username, *(it.mentions.map { mention -> mention.username }.toTypedArray()))
                                    .apply { remove(account.username) }
                        },
                inReplyToId = inReplyTo.value?.id,
                visibility = visibility.value,
                contentWarning = inReplyTo.value?.spoilerText,
                replyingStatusAuthor = inReplyTo.value?.account?.name,
                replyingStatusContent = inReplyTo.value?.content?.toString(),
                tootRightNow = tootRightNow
        )
    }

    fun reset() {
        content.value = ""
        inReplyToMutable.value = null
        stashedVisibility?.let {
            visibilityMutable.value = stashedVisibility
            stashedVisibility = null
        }
    }
}
