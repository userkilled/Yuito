package net.accelf.yuito;

import android.content.Context;
import android.text.Spanned;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Px;

import com.google.android.material.button.MaterialButton;
import com.keylesspalace.tusky.R;
import com.keylesspalace.tusky.entity.Account;
import com.keylesspalace.tusky.entity.Emoji;
import com.keylesspalace.tusky.entity.HashTag;
import com.keylesspalace.tusky.entity.Status;
import com.keylesspalace.tusky.interfaces.LinkListener;
import com.keylesspalace.tusky.util.CustomEmojiHelper;
import com.keylesspalace.tusky.util.ImageLoadingHelper;
import com.keylesspalace.tusky.util.LinkHelper;
import com.keylesspalace.tusky.util.StatusDisplayOptions;

import java.util.List;

public class QuoteInlineHelper {
    private Status quoteStatus;

    private View quoteContainer;
    private ImageView quoteAvatar;
    private TextView quoteDisplayName;
    private TextView quoteUsername;
    private TextView quoteContentWarningDescription;
    private MaterialButton quoteContentWarningButton;
    private TextView quoteContent;
    private TextView quoteMedia;

    private LinkListener listener;
    @Px
    private int avatarRadius24dp;
    private StatusDisplayOptions statusDisplayOptions;

    public QuoteInlineHelper(Status status, View container, LinkListener listener,
                             @Px int avatarRadius24dp, StatusDisplayOptions statusDisplayOptions) {
        quoteStatus = status;
        quoteContainer = container;
        quoteAvatar = container.findViewById(R.id.status_quote_inline_avatar);
        quoteDisplayName = container.findViewById(R.id.status_quote_inline_display_name);
        quoteUsername = container.findViewById(R.id.status_quote_inline_username);
        quoteContentWarningDescription = container.findViewById(R.id.status_quote_inline_content_warning_description);
        quoteContentWarningButton = container.findViewById(R.id.status_quote_inline_content_warning_button);
        quoteContent = container.findViewById(R.id.status_quote_inline_content);
        quoteMedia = container.findViewById(R.id.status_quote_inline_media);
        this.listener = listener;
        this.avatarRadius24dp = avatarRadius24dp;
        this.statusDisplayOptions = statusDisplayOptions;
    }

    private void setDisplayName(String name, List<Emoji> customEmojis) {
        CharSequence emojifiedName = CustomEmojiHelper.emojify(name, customEmojis, quoteDisplayName, statusDisplayOptions.animateEmojis());
        quoteDisplayName.setText(emojifiedName);
    }

    private void setUsername(String name) {
        Context context = quoteUsername.getContext();
        String format = context.getString(R.string.status_username_format);
        String usernameText = String.format(format, name);
        quoteUsername.setText(usernameText);
    }

    private void setContent(
            Spanned content,
            List<Status.Mention> mentions,
            List<HashTag> tags,
            List<Emoji> emojis,
            LinkListener listener
    ) {
        Spanned singleLineText = SpannedTextHelper.replaceSpanned(content);
        CharSequence emojifiedText = CustomEmojiHelper.emojify(singleLineText, emojis, quoteContent, statusDisplayOptions.animateEmojis());
        LinkHelper.setClickableText(quoteContent, emojifiedText, mentions, tags, listener);
    }

    private void setAvatar(String url, @Px int avatarRadius24dp, StatusDisplayOptions statusDisplayOptions) {
        ImageLoadingHelper.loadAvatar(url, quoteAvatar, avatarRadius24dp, statusDisplayOptions.animateAvatars());
    }

    private void setSpoilerText(String spoilerText, List<Emoji> emojis) {
        CharSequence emojiSpoiler =
                CustomEmojiHelper.emojify(spoilerText, emojis, quoteContentWarningDescription, statusDisplayOptions.animateEmojis());
        quoteContentWarningDescription.setText(emojiSpoiler);
        quoteContentWarningDescription.setVisibility(View.VISIBLE);
        quoteContentWarningButton.setVisibility(View.VISIBLE);
        quoteContentWarningButton.setOnClickListener(v
                -> setContentVisibility(!(quoteContent.getVisibility() == View.VISIBLE)));
        setContentVisibility(false);
    }

    private void setContentVisibility(boolean show) {
        if (show) {
            quoteContent.setVisibility(View.VISIBLE);
            quoteContentWarningButton.setText(R.string.status_content_warning_show_less);
        } else {
            quoteContent.setVisibility(View.GONE);
            quoteContentWarningButton.setText(R.string.status_content_warning_show_more);
        }
    }

    private void hideSpoilerText() {
        quoteContentWarningDescription.setVisibility(View.GONE);
        quoteContentWarningButton.setVisibility(View.GONE);
        quoteContent.setVisibility(View.VISIBLE);
    }

    private void setOnClickListener(String accountId, String statusUrl) {
        quoteAvatar.setOnClickListener(view -> listener.onViewAccount(accountId));
        quoteDisplayName.setOnClickListener(view -> listener.onViewAccount(accountId));
        quoteUsername.setOnClickListener(view -> listener.onViewAccount(accountId));
        quoteContent.setOnClickListener(view -> listener.onViewUrl(statusUrl, statusUrl));
        quoteMedia.setOnClickListener(view -> listener.onViewUrl(statusUrl, statusUrl));
        quoteContainer.setOnClickListener(view -> listener.onViewUrl(statusUrl, statusUrl));
    }

    public void setupQuoteContainer() {
        Account account = quoteStatus.getAccount();
        setDisplayName(account.getName(), account.getEmojis());
        setUsername(account.getUsername());
        setContent(
                quoteStatus.getContent(),
                quoteStatus.getMentions(),
                quoteStatus.getTags(),
                quoteStatus.getEmojis(),
                listener
        );
        setAvatar(account.getAvatar(), avatarRadius24dp, statusDisplayOptions);
        setOnClickListener(account.getId(), quoteStatus.getUrl());

        if (quoteStatus.getSpoilerText().isEmpty()) {
            hideSpoilerText();
        } else {
            setSpoilerText(quoteStatus.getSpoilerText(), quoteStatus.getEmojis());
        }

        if (quoteStatus.getAttachments().size() == 0) {
            quoteMedia.setVisibility(View.GONE);
        } else {
            quoteMedia.setVisibility(View.VISIBLE);
            quoteMedia.setText(quoteContainer.getContext().getString(R.string.status_quote_media,
                    quoteStatus.getAttachments().size()));
        }
    }
}
