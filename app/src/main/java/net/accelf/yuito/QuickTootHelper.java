package net.accelf.yuito;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;

import com.keylesspalace.tusky.AccountActivity;
import com.keylesspalace.tusky.BottomSheetActivity;
import com.keylesspalace.tusky.PostLookupFallbackBehavior;
import com.keylesspalace.tusky.R;
import com.keylesspalace.tusky.ViewTagActivity;
import com.keylesspalace.tusky.appstore.DrawerFooterClickedEvent;
import com.keylesspalace.tusky.appstore.Event;
import com.keylesspalace.tusky.appstore.EventHub;
import com.keylesspalace.tusky.appstore.PreferenceChangedEvent;
import com.keylesspalace.tusky.appstore.QuickReplyEvent;
import com.keylesspalace.tusky.components.compose.ComposeActivity;
import com.keylesspalace.tusky.components.compose.view.TootButton;
import com.keylesspalace.tusky.db.AccountEntity;
import com.keylesspalace.tusky.db.AccountManager;
import com.keylesspalace.tusky.entity.Announcement;
import com.keylesspalace.tusky.entity.Status;
import com.keylesspalace.tusky.interfaces.LinkListener;
import com.keylesspalace.tusky.util.LinkHelper;
import com.keylesspalace.tusky.util.ListUtils;
import com.keylesspalace.tusky.util.ThemeUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.reactivex.android.schedulers.AndroidSchedulers;

import static com.keylesspalace.tusky.components.compose.ComposeActivity.CAN_USE_UNLEAKABLE;
import static com.keylesspalace.tusky.components.compose.ComposeActivity.PREF_DEFAULT_TAG;
import static com.keylesspalace.tusky.components.compose.ComposeActivity.PREF_USE_DEFAULT_TAG;
import static com.uber.autodispose.AutoDispose.autoDisposable;
import static com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider.from;

public class QuickTootHelper {

    private Context context;
    private TextView quickReplyInfo;
    private TextView defaultTagInfo;
    private ImageView visibilityButton;
    private EditText tootEditText;
    private ImageButton openAnnouncementsButton;
    private TextView announcementsText;
    private ImageButton prevButton;
    private ImageButton nextButton;
    private TextView announcementsCountText;
    private TootButton quickTootButton;

    private SharedPreferences defPrefs;
    private String domain;
    private String loggedInUsername;
    private EventHub eventHub;
    private LinkListener listener;

    private Status inReplyTo;
    private boolean open = false;
    private int index = 0;
    private List<Announcement> announcements;

    private static final String PREF_CURRENT_VISIBILITY = "current_visibility";

    public QuickTootHelper(BottomSheetActivity activity, ConstraintLayout root, AccountManager accountManager, EventHub eventHub) {
        context = root.getContext();
        quickReplyInfo = root.findViewById(R.id.quick_reply_info);
        defaultTagInfo = root.findViewById(R.id.default_tag_info);
        visibilityButton = root.findViewById(R.id.visibility_button);
        tootEditText = root.findViewById(R.id.toot_edit_text);
        openAnnouncementsButton = root.findViewById(R.id.button_open_announcements);
        announcementsText = root.findViewById(R.id.text_view_announcements);
        prevButton = root.findViewById(R.id.button_prev_announcements);
        nextButton = root.findViewById(R.id.button_next_announcements);
        announcementsCountText = root.findViewById(R.id.text_view_announcements_count);
        quickTootButton = root.findViewById(R.id.toot_button);

        context = root.getContext();
        this.defPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        AccountEntity account = accountManager.getActiveAccount();
        if (account != null) {
            domain = account.getDomain();
            loggedInUsername = account.getUsername();
        }

        this.eventHub = eventHub;

        updateVisibilityButton();
        updateDefaultTagInfo();
        visibilityButton.setOnClickListener(v -> setNextVisibility());
        quickTootButton.setOnClickListener(v -> quickToot());

        listener = new LinkListener() {
            @Override
            public void onViewTag(String tag) {
                context.startActivity(ViewTagActivity.getIntent(context, tag));
            }

            @Override
            public void onViewAccount(String id) {
                context.startActivity(AccountActivity.getIntent(context, id));
            }

            @Override
            public void onViewUrl(String url, String text) {
                activity.viewUrl(url, PostLookupFallbackBehavior.OPEN_IN_BROWSER, text);
            }
        };
        activity.mastodonApi.listAnnouncements()
                .observeOn(AndroidSchedulers.mainThread())
                .as(autoDisposable(from(activity, Lifecycle.Event.ON_DESTROY)))
                .subscribe(
                        a -> {
                            announcements = a;
                            updateAnnouncements();
                        },
                        Throwable::printStackTrace
                );
        updateAnnouncements();
        openAnnouncementsButton.setOnClickListener(v -> toggleOpenAnnouncements());
        announcementsText.setOnClickListener(v -> toggleOpenAnnouncements());
        prevButton.setOnClickListener(v -> prevAnnouncement());
        nextButton.setOnClickListener(v -> nextAnnouncement());
    }

    public void composeButton() {
        if (tootEditText.getText().length() == 0 && inReplyTo == null) {
            context.startActivity(getComposeIntent(context, true, false));
        } else {
            startComposeWithQuickComposeData();
        }
    }

    public void handleEvent(Event event) {
        if (event instanceof QuickReplyEvent) {
            reply(((QuickReplyEvent) event).getStatus());
        } else if (event instanceof PreferenceChangedEvent) {
            switch (((PreferenceChangedEvent) event).getPreferenceKey()) {
                case PREF_CURRENT_VISIBILITY: {
                    updateVisibilityButton();
                    break;
                }
                case PREF_DEFAULT_TAG:
                case PREF_USE_DEFAULT_TAG: {
                    updateDefaultTagInfo();
                    break;
                }
            }
        } else if (event instanceof DrawerFooterClickedEvent) {
            tootEditText.setText("にゃーん");
        }
    }

    private void reply(Status status) {
        inReplyTo = status;
        updateQuickReplyInfo();
    }

    private void startComposeWithQuickComposeData() {
        Intent intent = getComposeIntent(context, false, false);
        resetQuickCompose();
        context.startActivity(intent);
    }

    private void quickToot() {
        if (tootEditText.getText().toString().length() > 0) {
            Intent intent = getComposeIntent(context, false, true);
            resetQuickCompose();
            context.startActivity(intent);
        }
    }

    private Intent getComposeIntent(Context context, boolean onlyVisibility, boolean tootRightNow) {
        ComposeActivity.ComposeOptions options = new ComposeActivity.ComposeOptions();
        options.setVisibility(getCurrentVisibility());
        if (onlyVisibility) {
            return ComposeActivity.startIntent(context, options);
        }
        options.setTootText(tootEditText.getText().toString());
        options.setTootRightNow(tootRightNow);

        if (inReplyTo != null) {
            Status.Mention[] mentions = inReplyTo.getMentions();
            Set<String> mentionedUsernames = new LinkedHashSet<>();
            mentionedUsernames.add(inReplyTo.getAccount().getUsername());
            for (Status.Mention mention : mentions) {
                mentionedUsernames.add(mention.getUsername());
            }
            mentionedUsernames.remove(loggedInUsername);

            options.setInReplyToId(inReplyTo.getId());
            options.setContentWarning(inReplyTo.getSpoilerText());
            options.setMentionedUsernames(mentionedUsernames);
            options.setReplyingStatusAuthor(inReplyTo.getAccount().getLocalUsername());
            options.setReplyingStatusContent(inReplyTo.getContent().toString());
        }

        return ComposeActivity.startIntent(context, options);
    }

    private void resetQuickCompose() {
        tootEditText.getText().clear();
        inReplyTo = null;
        updateQuickReplyInfo();
    }

    private void updateQuickReplyInfo() {
        if (inReplyTo != null) {
            quickReplyInfo.setText(String.format("Reply to : %s", inReplyTo.getAccount().getUsername()));
        } else {
            quickReplyInfo.setText("");
        }
    }

    private void updateDefaultTagInfo() {
        boolean useDefaultTag = defPrefs.getBoolean(PREF_USE_DEFAULT_TAG, false);
        String defaultText = defPrefs.getString(PREF_DEFAULT_TAG, "");
        if (useDefaultTag) {
            defaultTagInfo.setText(String.format("%s : %s", context.getString(R.string.hint_default_text), defaultText));
            defaultTagInfo.setTextColor(ThemeUtils.getColor(context, R.attr.colorInfo));
        } else {
            defaultTagInfo.setText(String.format("%s inactive", context.getString(R.string.hint_default_text)));
            defaultTagInfo.setTextColor(ThemeUtils.getColor(context, android.R.attr.textColorTertiary));
        }
    }

    private Status.Visibility getCurrentVisibility() {
        Status.Visibility visibility = Status.Visibility.byNum(defPrefs.getInt(PREF_CURRENT_VISIBILITY, Status.Visibility.PUBLIC.getNum()));
        if (!Arrays.asList(CAN_USE_UNLEAKABLE)
                .contains(domain) && visibility == Status.Visibility.UNLEAKABLE) {
            defPrefs.edit()
                    .putInt(PREF_CURRENT_VISIBILITY, Status.Visibility.PUBLIC.getNum())
                    .apply();
            eventHub.dispatch(new PreferenceChangedEvent(PREF_CURRENT_VISIBILITY));
            return Status.Visibility.PUBLIC;
        }
        return visibility;
    }

    private void updateVisibilityButton() {
        Status.Visibility visibility = getCurrentVisibility();
        quickTootButton.setStatusVisibility(visibility);
        switch (visibility) {
            case PUBLIC:
                visibilityButton.setImageResource(R.drawable.ic_public_24dp);
                break;
            case UNLISTED:
                visibilityButton.setImageResource(R.drawable.ic_lock_open_24dp);
                break;
            case PRIVATE:
                visibilityButton.setImageResource(R.drawable.ic_lock_outline_24dp);
                break;
            case UNLEAKABLE:
                visibilityButton.setImageResource(R.drawable.ic_low_vision_24dp);
                break;
        }
    }

    private void setNextVisibility() {
        Status.Visibility visibility = getCurrentVisibility();
        switch (visibility) {
            case PUBLIC:
                visibility = Status.Visibility.UNLISTED;
                break;
            case UNLISTED:
                visibility = Status.Visibility.PRIVATE;
                break;
            case PRIVATE:
                if (Arrays.asList(CAN_USE_UNLEAKABLE).contains(domain)) {
                    visibility = Status.Visibility.UNLEAKABLE;
                } else {
                    visibility = Status.Visibility.PUBLIC;
                }
                break;
            case UNLEAKABLE:
            case UNKNOWN:
                visibility = Status.Visibility.PUBLIC;
                break;
        }
        defPrefs.edit()
                .putInt(PREF_CURRENT_VISIBILITY, visibility.getNum())
                .apply();
        eventHub.dispatch(new PreferenceChangedEvent(PREF_CURRENT_VISIBILITY));
        updateVisibilityButton();
    }

    private void updateAnnouncements() {
        if (ListUtils.isEmpty(announcements)) {
            openAnnouncementsButton.setVisibility(View.GONE);
            announcementsText.setVisibility(View.GONE);
            announcementsCountText.setVisibility(View.GONE);
            prevButton.setVisibility(View.GONE);
            nextButton.setVisibility(View.GONE);
        } else {
            openAnnouncementsButton.setVisibility(View.VISIBLE);
            announcementsText.setVisibility(View.VISIBLE);
            announcementsCountText.setVisibility(View.VISIBLE);
            if (open) {
                prevButton.setVisibility(View.VISIBLE);
                nextButton.setVisibility(View.VISIBLE);
            } else {
                prevButton.setVisibility(View.GONE);
                nextButton.setVisibility(View.GONE);
            }

            openAnnouncementsButton.setImageDrawable(ContextCompat.getDrawable(context, open ? R.drawable.ic_arrow_drop_down : R.drawable.ic_arrow_drop_up));
            announcementsText.setSingleLine(!open);
            announcementsCountText.setText(String.format(Locale.getDefault(), "(%d/%d)", index + 1, announcements.size()));
            Announcement announcement = announcements.get(index);
            LinkHelper.setClickableText(announcementsText, announcement.getContent(), announcement.getMentions(), listener, false);
        }
    }

    private void toggleOpenAnnouncements() {
        open = !open;
        updateAnnouncements();
    }

    private void prevAnnouncement() {
        if (index > 0) {
            index--;
            updateAnnouncements();
        }
    }

    private void nextAnnouncement() {
        if (index < announcements.size() - 1) {
            index++;
            updateAnnouncements();
        }
    }
}
