package dev.jaonoctus.easykeychain;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;


public class NotificationChannelManager {
    public static final String PERMISSION_REQUESTS = "permissionRequests";
    public static final String PASSPHRASE_CACHE = "passphraseCache";
    public static final String ORBOT = "orbot";
    public static final String PGP_DETECTED = "pgpDetected";

    private final Context context;
    private final NotificationManager notificationManager;

    public static NotificationChannelManager getInstance(Context context) {
        NotificationManager notifyMan = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        return new NotificationChannelManager(context.getApplicationContext(), notifyMan);
    }

    private NotificationChannelManager(Context context, NotificationManager notificationManager) {
        this.context = context;
        this.notificationManager = notificationManager;
    }

    public void createNotificationChannelsIfNecessary() {
        if (notificationManager == null) {
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        createNotificationChannel(PERMISSION_REQUESTS, R.string.notify_channel_permission, NotificationManager.IMPORTANCE_MIN);
        createNotificationChannel(PASSPHRASE_CACHE, R.string.notify_channel_passcache, NotificationManager.IMPORTANCE_NONE);
        createNotificationChannel(ORBOT, R.string.notify_channel_orbot, NotificationManager.IMPORTANCE_DEFAULT);
        createNotificationChannel(PGP_DETECTED, R.string.notify_channel_pgp_detected, NotificationManager.IMPORTANCE_HIGH);
    }

    @RequiresApi(api = VERSION_CODES.O)
    private void createNotificationChannel(String channelName, @StringRes int channelDescription, int importance) {
        CharSequence descriptionText = context.getString(channelDescription);
        NotificationChannel channel = new NotificationChannel(channelName, descriptionText, importance);
        notificationManager.createNotificationChannel(channel);
    }
}
