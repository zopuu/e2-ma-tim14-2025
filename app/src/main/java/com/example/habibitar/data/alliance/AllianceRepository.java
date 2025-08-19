package com.example.habibitar.data.alliance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.habibitar.domain.model.Alliance;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AllianceRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private String requireUid() {
        if (auth.getCurrentUser() == null) throw new IllegalStateException("Not signed in");
        return auth.getCurrentUser().getUid();
    }

    /** Returns the user's current alliance, or null if none. */
    public CompletableFuture<Alliance> getMyAlliance() {
        String me = requireUid();
        CompletableFuture<Alliance> future = new CompletableFuture<>();

        db.collection("users").document(me).get()
                .addOnSuccessListener(userDoc -> {
                    String allianceId = userDoc.getString("allianceId");
                    if (allianceId == null || allianceId.isEmpty()) { future.complete(null); return; }

                    DocumentReference aRef = db.collection("alliances").document(allianceId);
                    aRef.get().addOnSuccessListener(aSnap -> {
                        if (!aSnap.exists()) { future.complete(null); return; }

                        final String name = aSnap.getString("name");
                        final String ownerUid = aSnap.getString("ownerUid");
                        final Boolean missionActive = Boolean.TRUE.equals(aSnap.getBoolean("missionActive"));
                        String ownerUsername = aSnap.getString("ownerUsername");

                        // Fallback if older docs don't have ownerUsername yet
                        CompletableFuture<String> ownerNameFuture = new CompletableFuture<>();
                        if (ownerUsername != null) {
                            ownerNameFuture.complete(ownerUsername);
                        } else {
                            db.collection("users").document(ownerUid).get()
                                    .addOnSuccessListener(d -> ownerNameFuture.complete(
                                            d.getString("username") != null ? d.getString("username") : ownerUid))
                                    .addOnFailureListener(ownerNameFuture::completeExceptionally);
                        }

                        aRef.collection("members").get().addOnSuccessListener(members -> {
                            ownerNameFuture.thenAccept(ownerName -> {
                                future.complete(new Alliance(
                                        aSnap.getId(),
                                        name != null ? name : "Alliance",
                                        ownerUid,
                                        members.size(),
                                        ownerName,
                                        missionActive
                                ));
                            }).exceptionally(ex -> { future.completeExceptionally(ex); return null; });
                        }).addOnFailureListener(future::completeExceptionally);

                    }).addOnFailureListener(future::completeExceptionally);
                })
                .addOnFailureListener(future::completeExceptionally);

        return future;
    }


    /**
     * Creates an alliance with 'name', sets current user as leader, marks user membership,
     * and sends notifications to all the user's friends.
     */
    public CompletableFuture<Alliance> createAllianceAndNotifyFriends(@NonNull String name) {
        String me = requireUid();
        CompletableFuture<Alliance> future = new CompletableFuture<>();

        // fetch my username once to store in alliance doc
        db.collection("users").document(me).get()
                .addOnSuccessListener(myDoc -> {
                    final String ownerUsernameFinal =
                            myDoc.getString("username") != null ? myDoc.getString("username") : "user";

                    DocumentReference allianceRef = db.collection("alliances").document();
                    DocumentReference leaderMemberRef = allianceRef.collection("members").document(me);
                    DocumentReference meUserRef = db.collection("users").document(me);

                    WriteBatch batch = db.batch();

                    java.util.HashMap<String, Object> a = new java.util.HashMap<>();
                    a.put("name", name);
                    a.put("ownerUid", me);
                    a.put("ownerUsername", ownerUsernameFinal); // ✅ denormalized
                    a.put("missionActive", false);         // ✅ placeholder flag
                    a.put("createdAt", FieldValue.serverTimestamp());
                    batch.set(allianceRef, a);

                    java.util.HashMap<String, Object> leader = new java.util.HashMap<>();
                    leader.put("role", "leader");
                    leader.put("joinedAt", FieldValue.serverTimestamp());
                    batch.set(leaderMemberRef, leader);

                    batch.update(meUserRef, "allianceId", allianceRef.getId());

                    batch.commit().addOnSuccessListener(unused -> {
                        notifyAllFriendsOfAlliance(me, allianceRef.getId(), name)
                                .thenAccept(v -> future.complete(
                                        new Alliance(allianceRef.getId(), name, me, 1, ownerUsernameFinal, false)
                                ))
                                .exceptionally(ex -> { future.completeExceptionally(ex); return null; });
                    }).addOnFailureListener(future::completeExceptionally);
                })
                .addOnFailureListener(future::completeExceptionally);

        return future;
    }


    /** Writes a simple "alliance_invite" notification for each friend. */
    private CompletableFuture<Void> notifyAllFriendsOfAlliance(
            @NonNull String ownerUid, @NonNull String allianceId, @NonNull String allianceName) {

        CompletableFuture<Void> f = new CompletableFuture<>();

        // load my friends
        db.collection("users").document(ownerUid).collection("friends").get()
                .addOnSuccessListener((QuerySnapshot friends) -> {
                    List<CompletableFuture<Void>> writes = new ArrayList<>();
                    for (DocumentSnapshot d : friends) {
                        String friendUid = d.getId();
                        DocumentReference notif =
                                db.collection("users").document(friendUid)
                                        .collection("notifications").document();

                        HashMap<String, Object> payload = new HashMap<>();
                        payload.put("type", "alliance_invite");
                        payload.put("fromUid", ownerUid);
                        payload.put("allianceId", allianceId);
                        payload.put("allianceName", allianceName);
                        payload.put("createdAt", FieldValue.serverTimestamp());
                        payload.put("pending", true); // must be accepted/declined per spec
                        // You can add 'message' if you want

                        CompletableFuture<Void> one = new CompletableFuture<>();
                        notif.set(payload)
                                .addOnSuccessListener(unused -> one.complete(null))
                                .addOnFailureListener(one::completeExceptionally);
                        writes.add(one);
                    }
                    allOf(writes).thenAccept(v -> f.complete(null))
                            .exceptionally(ex -> { f.completeExceptionally(ex); return null; });
                })
                .addOnFailureListener(f::completeExceptionally);

        return f;
    }

    private static CompletableFuture<Void> allOf(List<CompletableFuture<Void>> fs) {
        return CompletableFuture.allOf(fs.toArray(new CompletableFuture[0]));
    }
    public CompletableFuture<Void> disbandAlliance(@NonNull String allianceId) {
        String me = requireUid();
        CompletableFuture<Void> future = new CompletableFuture<>();

        DocumentReference aRef = db.collection("alliances").document(allianceId);
        aRef.get().addOnSuccessListener(aSnap -> {
            if (!aSnap.exists()) { future.complete(null); return; }

            String ownerUid = aSnap.getString("ownerUid");
            boolean missionActive = Boolean.TRUE.equals(aSnap.getBoolean("missionActive"));

            if (!me.equals(ownerUid)) {
                future.completeExceptionally(new IllegalStateException("Samo vođa može ukinuti savez."));
                return;
            }
            if (missionActive) {
                future.completeExceptionally(new IllegalStateException("Misija je pokrenuta. Savez se ne može ukinuti."));
                return;
            }

            // load members and remove memberships + delete subdocs
            aRef.collection("members").get().addOnSuccessListener(members -> {
                WriteBatch batch = db.batch();
                for (DocumentSnapshot m : members) {
                    String uid = m.getId();
                    batch.update(db.collection("users").document(uid), "allianceId", null);
                    batch.delete(aRef.collection("members").document(uid));
                }
                batch.delete(aRef);
                batch.commit().addOnSuccessListener(unused -> future.complete(null))
                        .addOnFailureListener(future::completeExceptionally);
            }).addOnFailureListener(future::completeExceptionally);

        }).addOnFailureListener(future::completeExceptionally);

        return future;
    }

    public CompletableFuture<Void> leaveAlliance(@NonNull String allianceId) {
        String me = requireUid();
        CompletableFuture<Void> future = new CompletableFuture<>();

        DocumentReference aRef = db.collection("alliances").document(allianceId);
        aRef.get().addOnSuccessListener(aSnap -> {
            if (!aSnap.exists()) { future.complete(null); return; }

            String ownerUid = aSnap.getString("ownerUid");
            boolean missionActive = Boolean.TRUE.equals(aSnap.getBoolean("missionActive"));

            if (me.equals(ownerUid)) {
                future.completeExceptionally(new IllegalStateException("Vođa ne može napustiti savez. Može samo ukinuti."));
                return;
            }
            if (missionActive) {
                future.completeExceptionally(new IllegalStateException("Misija je pokrenuta. Ne možeš napustiti savez."));
                return;
            }

            WriteBatch batch = db.batch();
            batch.update(db.collection("users").document(me), "allianceId", null);
            batch.delete(aRef.collection("members").document(me));
            batch.commit().addOnSuccessListener(unused -> future.complete(null))
                    .addOnFailureListener(future::completeExceptionally);

        }).addOnFailureListener(future::completeExceptionally);

        return future;
    }

}
