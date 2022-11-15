package net.accelf.yuito

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.entity.Status.Visibility

class VisibilityToggleButton @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    AppCompatImageView(context, attrs) {

    var visibility: Visibility = Visibility.UNKNOWN
        set(value) {
            field = value
            setImageResource(
                when (visibility) {
                    Visibility.PUBLIC -> R.drawable.ic_public_24dp
                    Visibility.UNLISTED -> R.drawable.ic_lock_open_24dp
                    Visibility.PRIVATE -> R.drawable.ic_lock_outline_24dp
                    Visibility.DIRECT -> R.drawable.ic_email_24dp
                    Visibility.UNLEAKABLE -> R.drawable.ic_low_vision_24dp
                    else -> R.drawable.ic_lock_open_24dp
                }
            )

            contentDescription = context.getString(
                when (visibility) {
                    Visibility.UNKNOWN -> R.string.visibility_unknown
                    Visibility.PUBLIC -> R.string.visibility_public
                    Visibility.UNLISTED -> R.string.visibility_unlisted
                    Visibility.PRIVATE -> R.string.visibility_private
                    Visibility.DIRECT -> R.string.visibility_direct
                    Visibility.UNLEAKABLE -> R.string.visibility_unleakable
                }
            )
        }

    init {
        isClickable = true
        isFocusable = true
    }
}
