package com.example.habibitar.data.friends;

import com.example.habibitar.domain.model.Friend;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FriendsRepository {

    // TODO: replace with Firestore queries
    public CompletableFuture<List<Friend>> loadMyFriends() {
        return CompletableFuture.supplyAsync(() -> {
            ArrayList<Friend> demo = new ArrayList<>();
            demo.add(new Friend("u_anna", "anna", "avatar_1"));
            demo.add(new Friend("u_bob", "bobby", "avatar_2"));
            demo.add(new Friend("u_carl", "carl", null));
            return demo;
        });
    }

    // TODO: Firestore: search by username (case-insensitive)
    public CompletableFuture<List<Friend>> searchUsers(String query) {
        return CompletableFuture.supplyAsync(() -> {
            ArrayList<Friend> res = new ArrayList<>();
            if (query == null || query.trim().isEmpty()) return res;
            String q = query.toLowerCase();
            // fake: return a couple of matches that look like the query
            res.add(new Friend("u_" + q, q, null));
            res.add(new Friend("u_" + q + "_2", q + "_2", "avatar_3"));
            return res;
        });
    }

    // TODO: Firestore: send/accept friend request; for now we just succeed
    public CompletableFuture<Boolean> addFriend(String targetUid) {
        return CompletableFuture.completedFuture(true);
    }
}
