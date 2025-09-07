package com.example.habibitar.ui.alliance;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habibitar.R;
import com.example.habibitar.data.chat.MessagesRepository;
import com.example.habibitar.data.chat.MessagesRepository.ChatMessage;
import com.example.habibitar.data.chat.MessagesRepository.ChatMessageChange;
import com.example.habibitar.notify.NotificationsHub;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AllianceChatActivity extends AppCompatActivity {

    private String allianceId, allianceName;
    private MessagesRepository repo = new MessagesRepository();
    private androidx.recyclerview.widget.RecyclerView rv;
    private ChatAdapter adapter;
    private com.google.firebase.firestore.ListenerRegistration reg;
    private TextInputEditText etMessage;
    private ImageButton btnSend;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alliance_chat);

        allianceId = getIntent().getStringExtra("allianceId");
        allianceName = getIntent().getStringExtra("allianceName");

        MaterialToolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        tb.setNavigationOnClickListener(v -> onBackPressed());
        tb.setTitle(allianceName != null ? allianceName : "Alliance chat");

        rv = findViewById(R.id.rv);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rv.setLayoutManager(lm);
        adapter = new ChatAdapter();
        rv.setAdapter(adapter);

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                btnSend.setEnabled(s != null && s.toString().trim().length() > 0);
            }
        });

        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText() == null ? "" : etMessage.getText().toString().trim();
            if (text.isEmpty()) return;
            btnSend.setEnabled(false);
            repo.sendMessage(allianceId, text).thenAccept(x ->
                    runOnUiThread(() -> {
                        etMessage.setText("");
                        btnSend.setEnabled(true);
                    })
            ).exceptionally(ex -> {
                runOnUiThread(() -> {
                    btnSend.setEnabled(true);
                    Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
                });
                return null;
            });
        });
    }

    @Override protected void onResume() {
        super.onResume();
        NotificationsHub.get().setActiveChat(allianceId);

        // attach the realtime listener (you already have applyChanges)
        if (reg == null) {
            reg = repo.listen(
                    allianceId,
                    100,
                    this::applyChanges,
                    e -> runOnUiThread(() ->
                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show())
            );
        }

        // sweep undelivered message notifications (keep your existing sweep code)
        String me = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (me != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(me)
                    .collection("notifications")
                    .whereEqualTo("type", "alliance_message")
                    .whereEqualTo("allianceId", allianceId)
                    .whereEqualTo("delivered", false)
                    .get()
                    .addOnSuccessListener(q -> {
                        var b = com.google.firebase.firestore.FirebaseFirestore.getInstance().batch();
                        for (var d : q.getDocuments()) {
                            b.update(d.getReference(),
                                    "delivered", true,
                                    "deliveredAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
                        }
                        b.commit();
                    });
        }
    }

    @Override protected void onPause() {
        NotificationsHub.get().setActiveChat(null);
        if (reg != null) { reg.remove(); reg = null; }
        super.onPause();
    }



    private void applyChanges(List<ChatMessageChange> changes) {
        runOnUiThread(() -> {
            boolean movedOrAdded = false;
            for (ChatMessageChange ch : changes) {
                if (ch.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                    adapter.add(ch.message);                    // append (or insertAt if you track index)
                    movedOrAdded = true;
                } else if (ch.type == com.google.firebase.firestore.DocumentChange.Type.MODIFIED) {
                    adapter.updateById(ch.message);             // refresh timestamp/text
                    // If your MessagesRepository exposes oldIndex/newIndex, also move:
                    // adapter.move(ch.oldIndex, ch.newIndex);
                } else if (ch.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                    adapter.removeById(ch.message.id);
                }
            }
            if (movedOrAdded) rv.scrollToPosition(adapter.getItemCount() - 1);
        });
    }


    // ------- Adapter (mine vs other) -------
    static class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int MINE = 1, OTHER = 2;
        private final List<ChatMessage> data = new ArrayList<>();
        private final String me = FirebaseAuth.getInstance().getUid();

        void add(ChatMessage m) { data.add(m); notifyItemInserted(data.size()-1); }

        @Override public int getItemViewType(int position) {
            ChatMessage m = data.get(position);
            return (m.senderUid != null && m.senderUid.equals(me)) ? MINE : OTHER;
        }

        @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup p, int vt) {
            if (vt == MINE) {
                View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_chat_message_mine, p, false);
                return new MineVH(v);
            } else {
                View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_chat_message_other, p, false);
                return new OtherVH(v);
            }
        }

        @Override public void onBindViewHolder(RecyclerView.ViewHolder h, int pos) {
            ChatMessage m = data.get(pos);
            String time = formatTime(h.itemView.getContext(), m.createdAt);
            if (h instanceof MineVH) ((MineVH) h).bind(m, time);
            else ((OtherVH) h).bind(m, time);
        }

        @Override public int getItemCount() { return data.size(); }

        static String formatTime(Context ctx, com.google.firebase.Timestamp ts) {
            if (ts == null) return "";
            java.text.DateFormat df = android.text.format.DateFormat.getTimeFormat(ctx); // ← not null
            return df.format(ts.toDate());
        }
        static class MineVH extends RecyclerView.ViewHolder {
            final TextView tvText, tvMeta;
            MineVH(View v) { super(v); tvText = v.findViewById(R.id.tvText); tvMeta = v.findViewById(R.id.tvMeta); }
            void bind(ChatMessage m, String time) {
                tvText.setText(m.text);
                tvMeta.setText(m.senderUsername + " • " + time);
            }
        }
        static class OtherVH extends RecyclerView.ViewHolder {
            final TextView tvText, tvMeta;
            OtherVH(View v) { super(v); tvText = v.findViewById(R.id.tvText); tvMeta = v.findViewById(R.id.tvMeta); }
            void bind(ChatMessage m, String time) {
                tvText.setText(m.text);
                tvMeta.setText(m.senderUsername + " • " + time);
            }
        }
        void updateById(ChatMessage m) {
            for (int i = 0; i < data.size(); i++) {
                if (data.get(i).id.equals(m.id)) {
                    data.set(i, m);
                    notifyItemChanged(i);
                    return;
                }
            }
            // fallback: if we didn't find it, add it
            add(m);
        }

        void removeById(String id) {
            for (int i = 0; i < data.size(); i++) {
                if (data.get(i).id.equals(id)) {
                    data.remove(i);
                    notifyItemRemoved(i);
                    return;
                }
            }
        }


    }
}
