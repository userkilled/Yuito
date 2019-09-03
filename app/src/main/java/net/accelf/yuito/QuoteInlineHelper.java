package net.accelf.yuito;

import android.content.Context;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.bumptech.glide.Glide;
import com.keylesspalace.tusky.R;
import com.keylesspalace.tusky.entity.Account;
import com.keylesspalace.tusky.entity.Emoji;
import com.keylesspalace.tusky.entity.Status;
import com.keylesspalace.tusky.interfaces.LinkListener;
import com.keylesspalace.tusky.util.CustomEmojiHelper;
import com.keylesspalace.tusky.util.LinkHelper;

import java.util.List;

public class QuoteInlineHelper {
    private Status quoteStatus;

    private View quoteContainer;
    private ImageView quoteAvatar;
    private TextView quoteDisplayName;
    private TextView quoteUsername;
    private TextView quoteContentWarningDescription;
    private ToggleButton quoteContentWarningButton;
    private TextView quoteContent;
    private TextView quoteMedia;

    private LinkListener listener;

    public QuoteInlineHelper(Status status, View container, LinkListener listener) {
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
    }

    private void setDisplayName(String name, List<Emoji> customEmojis) {
        CharSequence emojifiedName = CustomEmojiHelper.emojifyString(name, customEmojis, quoteDisplayName);
        quoteDisplayName.setText(emojifiedName);
    }

    private void setUsername(String name) {
        Context context = quoteUsername.getContext();
        String format = context.getString(R.string.status_username_format);
        String usernameText = String.format(format, name);
        quoteUsername.setText(usernameText);
    }

    private void setContent(Spanned content, Status.Mention[] mentions, List<Emoji> emojis,
                            LinkListener listener) {
        Spanned singleLineText = SpannedTextHelper.replaceSpanned(content);
        Spanned emojifiedText = CustomEmojiHelper.emojifyText(singleLineText, emojis, quoteContent);
        LinkHelper.setClickableText(quoteContent, emojifiedText, mentions, listener, false);
    }

    private void setAvatar(String url) {
        if (TextUtils.isEmpty(url)) {
            quoteAvatar.setImageResource(R.drawable.avatar_default);
        } else {
            Glide.with(quoteAvatar.getContext())
                    .load(url)
                    .placeholder(R.drawable.avatar_default)
                    .into(quoteAvatar);
        }
    }

    private void setSpoilerText(String spoilerText, List<Emoji> emojis) {
        CharSequence emojiSpoiler =
                CustomEmojiHelper.emojifyString(spoilerText, emojis, quoteContentWarningDescription);
        quoteContentWarningDescription.setText(emojiSpoiler);
        quoteContentWarningDescription.setVisibility(View.VISIBLE);
        quoteContentWarningButton.setVisibility(View.VISIBLE);
        quoteContentWarningButton.setChecked(false);
        quoteContentWarningButton.setOnCheckedChangeListener((buttonView, isChecked)
                -> quoteContent.setVisibility(isChecked ? View.VISIBLE : View.GONE));
        quoteContent.setVisibility(View.GONE);

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
        quoteContent.setOnClickListener(view -> listener.onViewUrl(statusUrl));
        quoteMedia.setOnClickListener(view -> listener.onViewUrl(statusUrl));
        quoteContainer.setOnClickListener(view -> listener.onViewUrl(statusUrl));
    }

    public void setupQuoteContainer() {
        Account account = quoteStatus.getAccount();
        setDisplayName(account.getDisplayName().equals("") ? account.getLocalUsername() : account.getDisplayName(), account.getEmojis());
        setUsername(account.getUsername());
        setContent(quoteStatus.getContent(), quoteStatus.getMentions(),
                quoteStatus.getEmojis(), listener);
        setAvatar(account.getAvatar());
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