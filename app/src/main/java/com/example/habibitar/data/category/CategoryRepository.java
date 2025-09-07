package com.example.habibitar.data.category;

import com.example.habibitar.domain.model.Category;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CategoryRepository {
    private static final String COLLECTION = "categories";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance(); 
    private final FirebaseAuth auth = FirebaseAuth.getInstance(); 

    private String requireUid() {
        if (auth.getCurrentUser() == null) throw new IllegalStateException("Not signed in");
        return auth.getCurrentUser().getUid();
    }

    public CompletableFuture<Category> create(String name, String colorCode)
    {
        String loggedUserId = requireUid();
        CompletableFuture<Category> future = new CompletableFuture<>();

        DocumentReference doc = db.collection("categories").document(); 
        Category c = new Category();      
        c.setId(doc.getId());
        c.setName(name);
        c.setColorCode(colorCode);
        c.setOwnerId(loggedUserId);
        doc.set(c) 
                .addOnSuccessListener(v -> future.complete(c)) 
                .addOnFailureListener(future::completeExceptionally); 

        return future;
    }

    public CompletableFuture<List<Category>> getAllForCurrentUser() {
        String uid = requireUid();
        CompletableFuture<List<Category>> future = new CompletableFuture<>();

        db.collection(COLLECTION)
                .whereEqualTo("ownerId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Category> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap) {
                        Category c = d.toObject(Category.class);
                        if (c != null) {
                            if (c.getId() == null || c.getId().isEmpty()) {
                                try {
                                    Field f = Category.class.getDeclaredField("id");
                                    f.setAccessible(true);
                                    f.set(c, d.getId());
                                } catch (Exception ignored) {}
                            }
                            list.add(c);
                        }
                    }
                    future.complete(list);
                })
                .addOnFailureListener(future::completeExceptionally);

        return future;
    }

    public CompletableFuture<Void> updateColor(String categoryId, String newColorCode) {
        CompletableFuture<Void> fut = new CompletableFuture<>();
        db.collection(COLLECTION)
                .document(categoryId)
                .update("colorCode", newColorCode)
                .addOnSuccessListener(v -> fut.complete(null))
                .addOnFailureListener(fut::completeExceptionally);
        return fut;
    }

    public CompletableFuture<Category> getById(String categoryId) {
        CompletableFuture<Category> fut = new CompletableFuture<>();
        db.collection(COLLECTION)
                .document(categoryId)
                .get()
                .addOnSuccessListener(d -> {
                    Category c = d.toObject(Category.class);
                    if (c != null && (c.getId() == null || c.getId().isEmpty())) {
                        try {
                            java.lang.reflect.Field f = Category.class.getDeclaredField("id");
                            f.setAccessible(true);
                            f.set(c, d.getId());
                        } catch (Exception ignored) {}
                    }
                    fut.complete(c);
                })
                .addOnFailureListener(fut::completeExceptionally);
        return fut;
    }

    public CompletableFuture<java.util.Map<String, Category>> getByIds(java.util.Collection<String> ids) {
        CompletableFuture<java.util.Map<String, Category>> fut = new CompletableFuture<>();
        if (ids == null || ids.isEmpty()) {
            fut.complete(new java.util.HashMap<>());
            return fut;
        }

        java.util.List<String> all = new java.util.ArrayList<>(ids);
        java.util.List<CompletableFuture<java.util.List<Category>>> chunks = new java.util.ArrayList<>();

        for (int i = 0; i < all.size(); i += 10) {
            java.util.List<String> slice = all.subList(i, Math.min(i + 10, all.size()));
            CompletableFuture<java.util.List<Category>> cf = new CompletableFuture<>();
            db.collection(COLLECTION)
                    .whereIn(FieldPath.documentId(), slice)
                    .get()
                    .addOnSuccessListener(snap -> {
                        java.util.List<Category> list = new java.util.ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot d : snap) {
                            Category c = d.toObject(Category.class);
                            if (c != null) {
                                if (c.getId() == null || c.getId().isEmpty()) {
                                    try {
                                        java.lang.reflect.Field f = Category.class.getDeclaredField("id");
                                        f.setAccessible(true);
                                        f.set(c, d.getId());
                                    } catch (Exception ignored) {}
                                }
                                list.add(c);
                            }
                        }
                        cf.complete(list);
                    })
                    .addOnFailureListener(cf::completeExceptionally);
            chunks.add(cf);
        }

        CompletableFuture
                .allOf(chunks.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    java.util.Map<String, Category> map = new java.util.HashMap<>();
                    for (CompletableFuture<java.util.List<Category>> cf : chunks) {
                        for (Category c : cf.join()) {
                            map.put(c.getId(), c);
                        }
                    }
                    return map;
                })
                .whenComplete((map, err) -> {
                    if (err != null) fut.completeExceptionally(err);
                    else fut.complete(map);
                });

        return fut;
    }


}
