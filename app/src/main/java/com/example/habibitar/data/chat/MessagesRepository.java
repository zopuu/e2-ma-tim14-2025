package com.example.habibitar.data.chat;

import androidx.annotation.NonNull;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MessagesRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private String requireUid() {
        if (auth.getCurrentUser() == null) throw new IllegalStateException("Not signed in");
        return auth.getCurrentUser().getUid();
    }

    /** Listen to the last N messages (ascending). Returns a ListenerRegistration you should remove(). */
    public ListenerRegistration listen(String allianceId, int limit,
                                       java.util.function.Consumer<List<ChatMessageChange>> onChanges,
                                       java.util.function.Consumer<Throwable> onError) {
        Query q = db.collection("alliances").document(allianceId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limit(limit);

        return q.addSnapshotListener((snap, err) -> {
            if (err != null) { if (onError != null) onError.accept(err); return; }
            if (snap == null) return;
            List<ChatMessageChange> changes = new ArrayList<>();
            for (DocumentChange ch : snap.getDocumentChanges()) {
                ChatMessage m = ChatMessage.from(ch.getDocument());
                changes.add(new ChatMessageChange(ch.getType(), m));
            }
            onChanges.accept(changes);
        });
    }

    /** Sends a message and notifies all other members. */
    public CompletableFuture<Void> sendMessage(@NonNull String allianceId, @NonNull String text) {
        String me = requireUid();

        CompletableFuture<Void> f = new CompletableFuture<>();

        // Load my username & alliance name once
        DocumentReference meRef = db.collection("users").document(me);
        DocumentReference aRef = db.collection("alliances").document(allianceId);

        meRef.get().continueWithTask(t1 -> {
                    DocumentSnapshot meDoc = t1.getResult();
                    String myUsername = meDoc != null ? meDoc.getString("username") : "user";

                    return aRef.get().continueWithTask(t2 -> {
                        DocumentSnapshot aDoc = t2.getResult();
                        String allianceName = aDoc != null ? aDoc.getString("name") : "Alliance";

                        // 1) write the message
                        DocumentReference msgRef = aRef.collection("messages").document();
                        Map<String, Object> msg = new HashMap<>();
                        msg.put("text", text);
                        msg.put("senderUid", me);
                        msg.put("senderUsername", myUsername);
                        msg.put("createdAt", FieldValue.serverTimestamp());

                        return msgRef.set(msg).continueWithTask(t3 -> {
                            // 2) notify every other member
                            return aRef.collection("members").get().continueWithTask(t4 -> {
                                List<DocumentSnapshot> members = t4.getResult().getDocuments();
                                List<com.google.android.gms.tasks.Task<Void>> writes = new ArrayList<>();
                                String preview = text.length() > 60 ? text.substring(0, 60) + "…" : text;

                                for (DocumentSnapshot m : members) {
                                    String uid = m.getId();
                                    if (uid.equals(me)) continue;

                                    String nid = "alliance_message_" + allianceId + "_" + msgRef.getId();
                                    DocumentReference notif = db.collection("users").document(uid)
                                            .collection("notifications").document(nid);

                                    Map<String, Object> payload = new HashMap<>();
                                    payload.put("type", "alliance_message");
                                    payload.put("allianceId", allianceId);
                                    payload.put("allianceName", allianceName);
                                    payload.put("fromUid", me);
                                    payload.put("fromUsername", myUsername);
                                    payload.put("messagePreview", preview);
                                    payload.put("delivered", false);
                                    payload.put("createdAt", FieldValue.serverTimestamp());

                                    writes.add(notif.set(payload, SetOptions.merge()));
                                }
                                return com.google.android.gms.tasks.Tasks.whenAll(writes);
                            });
                        });
                    });
                }).addOnSuccessListener(u -> f.complete(null))
                .addOnFailureListener(f::completeExceptionally);

        return f;
    }

    // --------- models ---------
    public static class ChatMessage {
        public String id, text, senderUid, senderUsername;
        public Timestamp createdAt;

        public static ChatMessage from(DocumentSnapshot d) {
            ChatMessage m = new ChatMessage();
            m.id = d.getId();
            m.text = d.getString("text");
            m.senderUid = d.getString("senderUid");
            m.senderUsername = d.getString("senderUsername");
            m.createdAt = d.getTimestamp("createdAt");
            return m;
        }
    }
    public static class ChatMessageChange {
        public final DocumentChange.Type type;
        public final ChatMessage message;
        public ChatMessageChange(DocumentChange.Type t, ChatMessage m) { this.type = t; this.message = m; }
    }
}
