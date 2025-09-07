package com.example.habibitar.notify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.NotificationManager;
import android.widget.Toast;

import com.example.habibitar.data.notify.NotificationsRepository;

public class AllianceInviteActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent i) {
        final String action = i.getAction();
        final String notifDocId = i.getStringExtra("notifDocId");
        final String allianceId  = i.getStringExtra("allianceId");
        final String fromUid     = i.getStringExtra("fromUid");
        final String fromName    = i.getStringExtra("fromName");
        final int notificationId = i.getIntExtra("notificationId", 0);

        // Work in background safely
        final PendingResult pr = goAsync();
        NotificationsRepository repo = new NotificationsRepository();


        if ("ALLIANCE_INVITE_ACCEPT".equals(action)) {
            android.util.Log.d("Invite",
                    "accept pressed by=" + com.google.firebase.auth.FirebaseAuth.getInstance().getUid() +
                            ", notifDoc=" + notifDocId + ", allianceId=" + allianceId + ", fromUid=" + fromUid);

            repo.acceptAllianceInvite(notifDocId, allianceId, fromUid)
                    .thenAccept(v -> {
                        com.example.habibitar.notify.InviteForegroundService.stop(ctx, notificationId);
                        pr.finish();
                    })
                    .exceptionally(ex -> { toast(ctx, ex.getMessage()); toast(ctx, "Couldn't accept. Check connection or try again."); pr.finish();
                        com.example.habibitar.notify.InviteForegroundService.stop(ctx, notificationId);return null; });
        } else if ("ALLIANCE_INVITE_DECLINE".equals(action)) {
            repo.declineAllianceInvite(notifDocId)
                    .thenAccept(v -> {
                        com.example.habibitar.notify.InviteForegroundService.stop(ctx, notificationId);
                        pr.finish();
                    })
                    .exceptionally(ex -> { toast(ctx, ex.getMessage()); toast(ctx, "Couldn't decline. Try again."); pr.finish();
                        com.example.habibitar.notify.InviteForegroundService.stop(ctx, notificationId);return null; });
        }else if ("ALLIANCE_INVITE_DISMISSED".equals(action)) {
            final String allianceName = i.getStringExtra("allianceName");
            // re-check Firestore; if still pending, resurrect
            //final PendingResult pr = goAsync(); already declared
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(com.google.firebase.auth.FirebaseAuth.getInstance().getUid())
                    .collection("notifications")
                    .document(notifDocId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        Boolean pending = doc.getBoolean("pending");
                        if (Boolean.TRUE.equals(pending)) {
                            // bring it back after a tiny delay to avoid flicker
                            new android.os.Handler(ctx.getMainLooper()).postDelayed(() ->
                                            com.example.habibitar.notify.InviteForegroundService.start(
                                                    ctx.getApplicationContext(),
                                                    notifDocId, allianceId, allianceName, fromUid, fromName),
                                    400);
                        }
                        pr.finish();
                    })
                    .addOnFailureListener(e -> pr.finish());
        } else {
            pr.finish();
        }
    }
    private void toast(Context c, String m) {
        android.os.Handler h = new android.os.Handler(c.getMainLooper());
        h.post(() -> Toast.makeText(c, m == null ? "Error" : m, Toast.LENGTH_LONG).show());
    }
}
