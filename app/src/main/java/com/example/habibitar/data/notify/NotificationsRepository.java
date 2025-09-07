package com.example.habibitar.data.notify;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class NotificationsRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private String requireUid() {
        if (auth.getCurrentUser() == null) throw new IllegalStateException("Not signed in");
        return auth.getCurrentUser().getUid();
    }

    /** Sends an "alliance_invite" notification to ALL of my friends. Idempotent per allianceId. */
    public CompletableFuture<Void> sendAllianceInviteToAllFriends(
            @NonNull String allianceId, @NonNull String allianceName, @NonNull String inviterUsername) {

        String me = requireUid();
        return loadFriendUids(me).thenCompose(friendUids ->
                writeAllianceInvites(me, friendUids, allianceId, allianceName, inviterUsername)
        );
    }

    // ---- internal helpers ----

    private CompletableFuture<List<String>> loadFriendUids(@NonNull String ownerUid) {
        CompletableFuture<List<String>> f = new CompletableFuture<>();
        db.collection("users").document(ownerUid).collection("friends").get()
                .addOnSuccessListener(snaps -> {
                    List<String> ids = new ArrayList<>();
                    for (DocumentSnapshot d : snaps) ids.add(d.getId());
                    f.complete(ids);
                })
                .addOnFailureListener(f::completeExceptionally);
        return f;
    }

    private CompletableFuture<Void> writeAllianceInvites(
            @NonNull String fromUid,
            @NonNull List<String> friendUids,
            @NonNull String allianceId,
            @NonNull String allianceName,
            @NonNull String inviterUsername) {

        List<CompletableFuture<Void>> writes = new ArrayList<>();
        for (String friendUid : friendUids) {
            // Deterministic doc id -> prevents duplicates for the same alliance
            String docId = "alliance_invite_" + allianceId;
            DocumentReference notif = db.collection("users").document(friendUid)
                    .collection("notifications").document(docId);

            HashMap<String, Object> payload = new HashMap<>();
            payload.put("type", "alliance_invite");
            payload.put("fromUid", fromUid);
            payload.put("fromUsername", inviterUsername);
            payload.put("allianceId", allianceId);
            payload.put("allianceName", allianceName);
            payload.put("pending", true); // must be accepted/declined later
            payload.put("createdAt", FieldValue.serverTimestamp());

            // Merge to be idempotent if the same invite is sent twice
            CompletableFuture<Void> one = new CompletableFuture<>();
            notif.set(payload, SetOptions.merge())
                    .addOnSuccessListener(unused -> one.complete(null))
                    .addOnFailureListener(one::completeExceptionally);
            writes.add(one);
        }
        return allOf(writes);
    }

    private static CompletableFuture<Void> allOf(List<CompletableFuture<Void>> fs) {
        return CompletableFuture.allOf(fs.toArray(new CompletableFuture[0]));
    }
    public CompletableFuture<Void> acceptAllianceInvite(
            @NonNull String notifDocId, @NonNull String allianceId, @NonNull String fromUid) {

        String me = requireUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference aRef = db.collection("alliances").document(allianceId);
        DocumentReference meUserRef = db.collection("users").document(me);
        DocumentReference myNotif = db.collection("users").document(me)
                .collection("notifications").document(notifDocId);

        WriteBatch batch = db.batch();
        java.util.Map<String, Object> member = new java.util.HashMap<>();
        member.put("role", "member");
        member.put("joinedAt", FieldValue.serverTimestamp());
        batch.set(aRef.collection("members").document(me), member, SetOptions.merge());
        batch.update(meUserRef, "allianceId", allianceId);

        java.util.Map<String, Object> handled = new java.util.HashMap<>();
        handled.put("pending", false);
        handled.put("decision", "accepted");
        handled.put("handledAt", FieldValue.serverTimestamp());
        batch.update(myNotif, handled);

        CompletableFuture<Void> f = new CompletableFuture<>();
        batch.commit()
                .addOnSuccessListener(unused -> notifyCreatorOfAccept(db, fromUid, me, allianceId, f))
                .addOnFailureListener(e -> {
                    // If only the notif flip was a late retry, accept as success when server is handled
                    myNotif.get().addOnSuccessListener(doc -> {
                        Boolean pending = doc.getBoolean("pending");
                        String decision = doc.getString("decision");
                        if (Boolean.FALSE.equals(pending) && "accepted".equals(decision)) {
                            notifyCreatorOfAccept(db, fromUid, me, allianceId, f);
                        } else {
                            f.completeExceptionally(e);
                        }
                    }).addOnFailureListener(f::completeExceptionally);
                });

        return f;
    }

    public java.util.concurrent.CompletableFuture<Void> declineAllianceInvite(@NonNull String notifDocId) {
        String me = requireUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference myNotif = db.collection("users").document(me)
                .collection("notifications").document(notifDocId);

        CompletableFuture<Void> f = new CompletableFuture<>();
        myNotif.update(new java.util.HashMap<String, Object>() {{
                    put("pending", false);
                    put("decision", "declined");
                    put("handledAt", FieldValue.serverTimestamp());
                }}).addOnSuccessListener(unused -> f.complete(null))
                .addOnFailureListener(f::completeExceptionally);
        return f;
    }
    private void notifyCreatorOfAccept(
            FirebaseFirestore db, String creatorUid, String me, String allianceId, CompletableFuture<Void> f) {

        DocumentReference allianceRef = db.collection("alliances").document(allianceId);
        DocumentReference myUserRef   = db.collection("users").document(me);

        // Fetch alliance name and my username in parallel, then write creator's notif
        allianceRef.get().addOnSuccessListener(aSnap -> {
            final String allianceName = aSnap.getString("name");

            myUserRef.get().addOnSuccessListener(meSnap -> {
                final String byUsername = meSnap.getString("username");

                String docId = "alliance_accept_" + me + "_" + allianceId;
                db.collection("users").document(creatorUid)
                        .collection("notifications").document(docId)
                        .set(new java.util.HashMap<String, Object>() {{
                            put("type", "alliance_accept");
                            put("byUid", me);
                            put("byUsername", byUsername);
                            put("allianceId", allianceId);
                            put("allianceName", allianceName);
                            put("createdAt", FieldValue.serverTimestamp());
                            put("pending", false);
                            put("delivered", false);
                        }}, SetOptions.merge())
                        .addOnSuccessListener(u -> f.complete(null))
                        .addOnFailureListener(f::completeExceptionally);

            }).addOnFailureListener(f::completeExceptionally);
        }).addOnFailureListener(f::completeExceptionally);
    }
}
