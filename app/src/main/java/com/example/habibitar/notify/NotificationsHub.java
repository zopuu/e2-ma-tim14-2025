package com.example.habibitar.notify;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.example.habibitar.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class NotificationsHub {

    private static final String CH_INVITES = "alliance_invites";
    private static final String CH_EVENTS  = "alliance_events";

    private static final NotificationsHub INSTANCE = new NotificationsHub();
    public static NotificationsHub get() { return INSTANCE; }

    private Context app;
    private boolean initialized = false;
    private boolean notifEnabled = false; // set from an Activity after user grants permission

    private ListenerRegistration invitesReg;
    private ListenerRegistration acceptsReg;
    private final Set<String> shownAcceptIds =
            Collections.synchronizedSet(new HashSet<>());

    private final FirebaseAuth.AuthStateListener authListener = fa -> {
        // User changed → reattach listeners for the new UID
        detachAll();
        if (notifEnabled) attachAll();
    };

    public void init(Context applicationContext) {
        if (initialized) return;
        this.app = applicationContext.getApplicationContext();
        ensureChannels();
        FirebaseAuth.getInstance().addAuthStateListener(authListener);
        initialized = true;
    }

    /** Call this from any Activity after POST_NOTIFICATIONS is granted (or on <33). */
    public void setNotificationsEnabled(boolean enabled) {
        this.notifEnabled = enabled;
        if (enabled) {
            attachAll();
        } else {
            detachAll();
        }
    }

    private void ensureChannels() {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = app.getSystemService(NotificationManager.class);

            NotificationChannel invites = new NotificationChannel(
                    CH_INVITES, "Alliance invites", NotificationManager.IMPORTANCE_HIGH);
            invites.setDescription("Invitations to join alliances");
            nm.createNotificationChannel(invites);

            NotificationChannel events = new NotificationChannel(
                    CH_EVENTS, "Alliance events", NotificationManager.IMPORTANCE_DEFAULT);
            events.setDescription("Notifications when members accept your invites");
            nm.createNotificationChannel(events);
        }
    }

    private void attachAll() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // --- Invites → sticky foreground notification with actions ---
        if (invitesReg != null) invitesReg.remove();
        invitesReg = db.collection("users").document(uid)
                .collection("notifications")
                .whereEqualTo("type", "alliance_invite")
                .whereEqualTo("pending", true)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null) return;
                    for (DocumentChange ch : snap.getDocumentChanges()) {
                        if (ch.getType() != DocumentChange.Type.ADDED) continue;
                        String docId = ch.getDocument().getId();
                        String allianceId = ch.getDocument().getString("allianceId");
                        String allianceName = ch.getDocument().getString("allianceName");
                        String fromUid = ch.getDocument().getString("fromUid");

                        // Only show if we actually have runtime permission (Android 13+)
                        if (notifEnabled) {
                            InviteForegroundService.start(
                                    app, docId, allianceId, allianceName, fromUid);
                        }
                    }
                });

        // --- Creator events (someone accepted) → normal auto-cancel notification ---
        if (acceptsReg != null) acceptsReg.remove();
        acceptsReg = db.collection("users").document(uid)
                .collection("notifications")
                .whereEqualTo("type", "alliance_accept")
                .whereEqualTo("delivered", false)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null) return;
                    for (DocumentChange ch : snap.getDocumentChanges()) {
                        if (ch.getType() != DocumentChange.Type.ADDED) continue;

                        String id = ch.getDocument().getId();
                        if (!shownAcceptIds.add(id)) continue;

                        String who = ch.getDocument().getString("byUsername");
                        if (who == null || who.isEmpty()) who = ch.getDocument().getString("byUid");
                        String alliance = ch.getDocument().getString("allianceName");
                        if (alliance == null || alliance.isEmpty()) alliance = "your alliance";

                        showAllianceAcceptNotification(id, who, alliance);

                        ch.getDocument().getReference().update(
                                "delivered", true,
                                "deliveredAt", com.google.firebase.firestore.FieldValue.serverTimestamp()
                        );
                    }
                });
    }

    private void detachAll() {
        if (invitesReg != null) { invitesReg.remove(); invitesReg = null; }
        if (acceptsReg != null) { acceptsReg.remove(); acceptsReg = null; }
        shownAcceptIds.clear();
    }

    private void showAllianceAcceptNotification(String docId, String who, String allianceName) {
        int nid = ("accept_" + docId).hashCode();

        Intent openAlliance = new Intent(app, com.example.habibitar.ui.alliance.AllianceActivity.class);
        PendingIntent contentPi = PendingIntent.getActivity(
                app, nid, openAlliance,
                (android.os.Build.VERSION.SDK_INT >= 23)
                        ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        : PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder b = new NotificationCompat.Builder(app, CH_EVENTS)
                .setSmallIcon(R.drawable.ic_groups_24)
                .setContentTitle("Invite accepted")
                .setContentText(who + " joined " + allianceName)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(contentPi);

        NotificationManager nm = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(nid, b.build());
    }
}
