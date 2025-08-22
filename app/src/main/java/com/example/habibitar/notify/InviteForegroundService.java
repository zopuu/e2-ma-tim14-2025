// app/src/main/java/com/example/habibitar/notify/InviteForegroundService.java
package com.example.habibitar.notify;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.habibitar.R;

public class InviteForegroundService extends Service {
    public static final String EXTRA_NOTIF_ID = "notificationId";
    public static final String EXTRA_NOTIF_DOC = "notifDocId";
    public static final String EXTRA_ALLIANCEID = "allianceId";
    public static final String EXTRA_ALLIANCENAME = "allianceName";
    public static final String EXTRA_FROMUID = "fromUid";
    private static final String CH_INVITES = "alliance_invites";

    public static void start(Context ctx, String notifDocId, String allianceId, String allianceName, String fromUid) {
        int id = notifDocId.hashCode();
        Intent i = new Intent(ctx, InviteForegroundService.class)
                .putExtra(EXTRA_NOTIF_ID, id)
                .putExtra(EXTRA_NOTIF_DOC, notifDocId)
                .putExtra(EXTRA_ALLIANCEID, allianceId)
                .putExtra(EXTRA_ALLIANCENAME, allianceName)
                .putExtra(EXTRA_FROMUID, fromUid);
        androidx.core.content.ContextCompat.startForegroundService(ctx, i);
    }

    public static void stop(Context ctx, int notificationId) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(notificationId);
        ctx.stopService(new Intent(ctx, InviteForegroundService.class));
    }

    @Override public int onStartCommand(Intent i, int flags, int startId) {
        int id             = i.getIntExtra(EXTRA_NOTIF_ID, 0);
        String notifDocId  = i.getStringExtra(EXTRA_NOTIF_DOC);
        String allianceId  = i.getStringExtra(EXTRA_ALLIANCEID);
        String allianceName= i.getStringExtra(EXTRA_ALLIANCENAME);
        String fromUid     = i.getStringExtra(EXTRA_FROMUID);

        // build actions → explicit broadcast receiver
        Intent accept = new Intent(getApplicationContext(), com.example.habibitar.notify.AllianceInviteActionReceiver.class)
                .setAction("ALLIANCE_INVITE_ACCEPT")
                .putExtra("notifDocId", notifDocId)
                .putExtra("allianceId", allianceId)
                .putExtra("fromUid", fromUid)
                .putExtra("notificationId", id);

        Intent decline = new Intent(getApplicationContext(), com.example.habibitar.notify.AllianceInviteActionReceiver.class)
                .setAction("ALLIANCE_INVITE_DECLINE")
                .putExtra("notifDocId", notifDocId)
                .putExtra("allianceId", allianceId)
                .putExtra("fromUid", fromUid)
                .putExtra("notificationId", id);

        // NEW: handle swipe-away / clear action
        Intent dismissed = new Intent(getApplicationContext(), com.example.habibitar.notify.AllianceInviteActionReceiver.class)
                .setAction("ALLIANCE_INVITE_DISMISSED")
                .putExtra("notifDocId", notifDocId)
                .putExtra("allianceId", allianceId)
                .putExtra("allianceName", allianceName) // we’ll need this to resurrect
                .putExtra("fromUid", fromUid)
                .putExtra("notificationId", id);

        int flagsPi = (android.os.Build.VERSION.SDK_INT >= 23)
                ? android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
                : android.app.PendingIntent.FLAG_UPDATE_CURRENT;

        android.app.PendingIntent piAccept   = android.app.PendingIntent.getBroadcast(getApplicationContext(), 10000 + id, accept, flagsPi);
        android.app.PendingIntent piDecline  = android.app.PendingIntent.getBroadcast(getApplicationContext(), 20000 + id, decline, flagsPi);
        android.app.PendingIntent piDismiss  = android.app.PendingIntent.getBroadcast(getApplicationContext(), 30000 + id, dismissed, flagsPi);

        Notification n = new NotificationCompat.Builder(getApplicationContext(), CH_INVITES)
                .setSmallIcon(R.drawable.ic_groups_24)
                .setContentTitle("Poziv u savez")
                .setContentText("Savez: " + (allianceName == null ? "" : allianceName))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)      // advise “sticky”
                .setAutoCancel(false)  // never auto-cancel
                .setDeleteIntent(piDismiss) // 👈 catch swipe-away
                .addAction(0, "Prihvati", piAccept)
                .addAction(0, "Odbij",    piDecline)
                .build();

        startForeground(id, n);
        return START_NOT_STICKY;
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
