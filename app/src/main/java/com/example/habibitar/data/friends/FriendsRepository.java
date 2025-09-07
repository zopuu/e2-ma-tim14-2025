package com.example.habibitar.data.friends;

import androidx.annotation.NonNull;

import com.example.habibitar.domain.model.Friend;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FriendsRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private String requireUid() {
        if (auth.getCurrentUser() == null) throw new IllegalStateException("Not signed in.");
        return auth.getCurrentUser().getUid();
    }

    // ---------- Public API ----------

    /** Load my friends as simple Friend rows (uid, username, avatarKey). */
    public CompletableFuture<List<Friend>> loadMyFriends() {
        String me = requireUid();
        CompletableFuture<List<Friend>> future = new CompletableFuture<>();

        db.collection("users").document(me).collection("friends")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(friendRefs -> {
                    List<String> ids = new ArrayList<>();
                    for (DocumentSnapshot d : friendRefs) ids.add(d.getId());
                    if (ids.isEmpty()) { future.complete(new ArrayList<>()); return; }
                    fetchUsersByIdsBatched(ids)
                            .thenAccept(future::complete)
                            .exceptionally(ex -> { future.completeExceptionally(ex); return null; });
                })
                .addOnFailureListener(future::completeExceptionally);

        return future;
    }

    /** Case-insensitive prefix search by username. Requires 'username_lower' field. */
    public CompletableFuture<List<Friend>> searchUsers(@NonNull String query) {
        String q = query.trim().toLowerCase();
        CompletableFuture<List<Friend>> future = new CompletableFuture<>();
        if (q.isEmpty()) { future.complete(new ArrayList<>()); return future; }

        String me = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        db.collection("users")
                .orderBy("username_lower")
                .startAt(q)
                .endAt(q + "\uf8ff")
                .limit(20)
                .get()
                .addOnSuccessListener(snaps -> {
                    List<Friend> out = new ArrayList<>();
                    for (DocumentSnapshot d : snaps) {
                        String uid = d.getId();
                        if (me != null && me.equals(uid)) continue; // hide self
                        String username = d.getString("username");
                        String avatarKey = d.getString("avatarKey");
                        if (username != null) out.add(new Friend(uid, username, avatarKey));
                    }
                    future.complete(out);
                })
                .addOnFailureListener(future::completeExceptionally);

        return future;
    }

    /** Add friend immediately (no requests): creates mirrored edges in a batch. */
    public CompletableFuture<Boolean> addFriend(@NonNull String targetUid) {
        String me = requireUid();
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        if (me.equals(targetUid)) {
            future.complete(false);
            return future;
        }

        DocumentReference myEdge = db.collection("users").document(me)
                .collection("friends").document(targetUid);
        DocumentReference otherEdge = db.collection("users").document(targetUid)
                .collection("friends").document(me);

        WriteBatch batch = db.batch();

        // Edge data = createdAt server timestamp (extra fields can be added later)
        java.util.Map<String, Object> edge = new HashMap<>();
        edge.put("createdAt", FieldValue.serverTimestamp());

        batch.set(myEdge, edge);
        batch.set(otherEdge, edge);

        batch.commit()
                .addOnSuccessListener(unused -> future.complete(true))
                .addOnFailureListener(future::completeExceptionally);

        return future;
    }

    // ---------- Internal helpers ----------

    /** Fetch users by ids in chunks of 10 (Firestore whereIn limit). */
    private CompletableFuture<List<Friend>> fetchUsersByIdsBatched(List<String> ids) {
        List<CompletableFuture<List<Friend>>> chunks = new ArrayList<>();
        for (int i = 0; i < ids.size(); i += 10) {
            int to = Math.min(i + 10, ids.size());
            chunks.add(fetchUsersByIds(ids.subList(i, to)));
        }
        return allOfList(chunks).thenApply(parts -> {
            List<Friend> all = new ArrayList<>();
            for (List<Friend> p : parts) all.addAll(p);
            // preserve original order by ids list
            all.sort((a, b) -> Integer.compare(ids.indexOf(a.uid), ids.indexOf(b.uid)));
            return all;
        });
    }

    private CompletableFuture<List<Friend>> fetchUsersByIds(List<String> ids) {
        CompletableFuture<List<Friend>> f = new CompletableFuture<>();
        db.collection("users")
                .whereIn(FieldPath.documentId(), ids)
                .get()
                .addOnSuccessListener(snaps -> {
                    List<Friend> out = new ArrayList<>();
                    for (DocumentSnapshot d : snaps) {
                        String uid = d.getId();
                        String username = d.getString("username");
                        String avatarKey = d.getString("avatarKey");
                        if (username != null) out.add(new Friend(uid, username, avatarKey));
                    }
                    f.complete(out);
                })
                .addOnFailureListener(f::completeExceptionally);
        return f;
    }

    private static <T> CompletableFuture<List<T>> allOfList(List<CompletableFuture<T>> futures) {
        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        return all.thenApply(v -> {
            List<T> out = new ArrayList<>(futures.size());
            for (CompletableFuture<T> f : futures) out.add(f.join());
            return out;
        });
    }
}
