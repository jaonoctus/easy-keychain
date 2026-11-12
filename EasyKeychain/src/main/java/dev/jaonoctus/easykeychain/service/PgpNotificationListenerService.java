package dev.jaonoctus.easykeychain.service;


import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import dev.jaonoctus.easykeychain.Constants;
import dev.jaonoctus.easykeychain.NotificationChannelManager;
import dev.jaonoctus.easykeychain.R;
import dev.jaonoctus.easykeychain.pgp.PgpHelper;
import dev.jaonoctus.easykeychain.pgp.PgpMarkerObfuscator;
import dev.jaonoctus.easykeychain.ui.DecryptActivity;
import timber.log.Timber;


/**
 * Watches the notifications of other apps for PGP messages, and offers to decrypt the ones it
 * finds with a notification of its own.
 * <p>
 * This component is disabled in the manifest. It only exists for the system once the user turns
 * the feature on in the settings, and it only receives anything once they also grant notification
 * access. Notification contents are inspected in memory and never stored or logged.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class PgpNotificationListenerService extends NotificationListenerService {
    public static final int MIN_SDK = Build.VERSION_CODES.LOLLIPOP;

    /** Messaging apps repost their notification on every update, don't nag about the same message. */
    private static final int MAX_REMEMBERED_MESSAGES = 200;

    private final Set<Integer> recentlyNotified = new LinkedHashSet<>();

    public static boolean isSupported() {
        return Build.VERSION.SDK_INT >= MIN_SDK;
    }

    public static void setComponentEnabled(Context context, boolean enabled) {
        ComponentName componentName = new ComponentName(context, PgpNotificationListenerService.class);
        int state = enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        context.getPackageManager().setComponentEnabledSetting(componentName, state, PackageManager.DONT_KILL_APP);
    }

    public static boolean isComponentEnabled(Context context) {
        ComponentName componentName = new ComponentName(context, PgpNotificationListenerService.class);
        return context.getPackageManager().getComponentEnabledSetting(componentName)
                == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    public static boolean hasNotificationAccess(Context context) {
        return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.getPackageName());
    }

    /** True if the feature is switched on and the system actually delivers notifications to it. */
    public static boolean isActive(Context context) {
        return isSupported() && isComponentEnabled(context) && hasNotificationAccess(context);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || getPackageName().equals(sbn.getPackageName())) {
            return;
        }

        String message = findPgpMessage(extractTexts(sbn.getNotification()));
        if (message == null) {
            return;
        }

        int notificationId = notificationIdFor(message);
        if (!rememberMessage(notificationId)) {
            return;
        }

        Timber.d("Detected a PGP message in a notification from %s", sbn.getPackageName());
        showDetectedNotification(message, notificationId);
    }

    /**
     * Returns the first complete PGP message among the candidates, in armored form.
     * A message that the notification cut short cannot be decrypted, so it is not reported.
     */
    @Nullable
    @VisibleForTesting
    static String findPgpMessage(List<CharSequence> candidates) {
        for (CharSequence candidate : candidates) {
            // skip the regular expressions for the vast majority of notifications
            String text = candidate.toString();
            if (!text.contains("-----BEGIN PGP ") && !text.contains(PgpMarkerObfuscator.MARKER)) {
                continue;
            }
            String message = PgpHelper.getPgpMessageContent(text);
            if (message != null) {
                return message;
            }
        }
        return null;
    }

    /**
     * A notification id for a message, stable for the same text, so an app that reposts its
     * notification replaces our entry instead of stacking a duplicate.
     */
    @VisibleForTesting
    static int notificationIdFor(String message) {
        // mask instead of Math.abs, which stays negative for Integer.MIN_VALUE
        return Constants.NotificationIds.PGP_DETECTED_BASE
                + (message.hashCode() & Integer.MAX_VALUE) % Constants.NotificationIds.PGP_DETECTED_RANGE;
    }

    /** @return false if this message was already reported recently */
    private boolean rememberMessage(int notificationId) {
        if (!recentlyNotified.add(notificationId)) {
            return false;
        }
        if (recentlyNotified.size() > MAX_REMEMBERED_MESSAGES) {
            recentlyNotified.remove(recentlyNotified.iterator().next());
        }
        return true;
    }

    private static List<CharSequence> extractTexts(@Nullable Notification notification) {
        List<CharSequence> texts = new ArrayList<>();
        if (notification == null || notification.extras == null) {
            return texts;
        }
        Bundle extras = notification.extras;

        // newest message of a conversation first, that's the one this notification is about
        NotificationCompat.MessagingStyle messagingStyle =
                NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification);
        if (messagingStyle != null) {
            List<NotificationCompat.MessagingStyle.Message> messages = messagingStyle.getMessages();
            for (int i = messages.size() - 1; i >= 0; i--) {
                addIfPresent(texts, messages.get(i).getText());
            }
        }

        addIfPresent(texts, extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT));
        addIfPresent(texts, extras.getCharSequence(NotificationCompat.EXTRA_TEXT));
        CharSequence[] lines = extras.getCharSequenceArray(NotificationCompat.EXTRA_TEXT_LINES);
        if (lines != null) {
            for (int i = lines.length - 1; i >= 0; i--) {
                addIfPresent(texts, lines[i]);
            }
        }

        return texts;
    }

    private static void addIfPresent(List<CharSequence> texts, @Nullable CharSequence text) {
        if (text != null && text.length() > 0) {
            texts.add(text);
        }
    }

    private void showDetectedNotification(String message, int notificationId) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }
        NotificationChannelManager.getInstance(this).createNotificationChannelsIfNecessary();

        Intent intent = new Intent(this, DecryptActivity.class);
        intent.setAction(Intent.ACTION_SEND);
        intent.setType(Constants.MIME_TYPE_TEXT);
        intent.putExtra(Intent.EXTRA_TEXT, message);
        intent.putExtra(DecryptActivity.EXTRA_DETECTED_NOTIFICATION_ID, notificationId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // The request code must differ per message. Extras are not part of a PendingIntent's
        // identity, so with a fixed one every notification would open the latest message.
        PendingIntent pendingIntent = PendingIntent.getActivity(this, notificationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | Constants.FLAG_IMMUTABLE_COMPAT);

        Notification notification = new NotificationCompat.Builder(this, NotificationChannelManager.PGP_DETECTED)
                .setSmallIcon(R.drawable.ic_stat_notify_24dp)
                .setContentTitle(getString(R.string.notify_pgp_detected_title))
                .setContentText(getString(R.string.notify_pgp_detected_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                // Not auto-cancelled on tap: DecryptActivity dismisses it once it has taken over
                // the message, so a tap that leads nowhere doesn't lose the way back to it.
                .setAutoCancel(false)
                .build();

        notificationManager.notify(notificationId, notification);
    }
}
