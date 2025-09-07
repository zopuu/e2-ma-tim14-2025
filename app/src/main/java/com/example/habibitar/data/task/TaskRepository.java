package com.example.habibitar.data.task;

import android.util.Log;

import com.example.habibitar.domain.model.Category;
import com.example.habibitar.domain.model.Task;
import com.example.habibitar.domain.model.enums.TaskStatus;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TaskRepository {
    private String TASKS = "tasks";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth auth = FirebaseAuth.getInstance();

    private String getLoggedUserId()
    {
        if(auth.getCurrentUser() == null)
        {
            throw new IllegalStateException("Not signed in");
        }
        return auth.getCurrentUser().getUid();
    }

    public CompletableFuture<Task> create(Task newTask)
    {
        String loggedUserId = getLoggedUserId();
        CompletableFuture<Task> future = new CompletableFuture<>();

        DocumentReference doc = db.collection(TASKS).document();
        newTask.setOwnerId(loggedUserId);
        newTask.setId(doc.getId());

        doc.set(newTask)
                .addOnSuccessListener(v -> future.complete(newTask))
                .addOnFailureListener(future::completeExceptionally);

        return future;
    }

    public CompletableFuture<Void> delete(String taskId) {
        CompletableFuture<Void> fut = new CompletableFuture<>();
        if (taskId == null || taskId.isEmpty()) {
            fut.completeExceptionally(new IllegalArgumentException("taskId is empty"));
            return fut;
        }
        db.collection(TASKS)
                .document(taskId)
                .delete()
                .addOnSuccessListener(v -> fut.complete(null))
                .addOnFailureListener(fut::completeExceptionally);
        return fut;
    }


    public CompletableFuture<Void> updateStatus(String taskId, TaskStatus newStatus) {
        CompletableFuture<Void> fut = new CompletableFuture<>();
        if (taskId == null || taskId.isEmpty()) {
            fut.completeExceptionally(new IllegalArgumentException("taskId is empty"));
            return fut;
        }
        db.collection(TASKS)
                .document(taskId)
                .update("status", newStatus != null ? newStatus.name() : null)
                .addOnSuccessListener(v -> fut.complete(null))
                .addOnFailureListener(e -> {
                    Log.e("TaskRepository", "updateStatus failed", e);
                    fut.completeExceptionally(e);
                });
        return fut;
    }


    public CompletableFuture<Task> getById(String taskId) {
        CompletableFuture<Task> fut = new CompletableFuture<>();
        db.collection(TASKS).document(taskId).get()
                .addOnSuccessListener(snap -> {
                    Task t = snap.toObject(Task.class);
                    if (t != null && (t.getId() == null || t.getId().isEmpty())) t.setId(snap.getId());
                    fut.complete(t);
                })
                .addOnFailureListener(fut::completeExceptionally);
        return fut;
    }

    public CompletableFuture<Void> updateTask(String taskId, Map<String, Object> fields) {
        CompletableFuture<Void> fut = new CompletableFuture<>();
        db.collection(TASKS).document(taskId)
                .update(fields)
                .addOnSuccessListener(v -> fut.complete(null))
                .addOnFailureListener(fut::completeExceptionally);
        return fut;
    }



    public CompletableFuture<java.util.List<Task>> getAllForCurrentUser() {
        String uid = getLoggedUserId();
        CompletableFuture<java.util.List<Task>> future = new CompletableFuture<>();

        db.collection(TASKS)
                .whereEqualTo("ownerId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    java.util.List<Task> tasks = new java.util.ArrayList<>();
                    java.util.Set<String> catIds = new java.util.HashSet<>();

                    for (DocumentSnapshot d : snap) {
                        Task t = d.toObject(Task.class);
                        if (t != null) {
                            if (t.getId() == null || t.getId().isEmpty()) {
                                try {
                                    java.lang.reflect.Field f = Task.class.getDeclaredField("id");
                                    f.setAccessible(true);
                                    f.set(t, d.getId());
                                } catch (Exception ignored) {}
                            }
                            tasks.add(t);
                            if (t.getCategoryId() != null && !t.getCategoryId().isEmpty()) {
                                catIds.add(t.getCategoryId());
                            }
                        }
                    }

                    if (catIds.isEmpty()) {
                        future.complete(tasks);
                        return;
                    }

                    new com.example.habibitar.data.category.CategoryRepository()
                            .getByIds(catIds)
                            .thenAccept(map -> {
                                for (Task t : tasks) {
                                    Category c = map.get(t.getCategoryId());
                                    if (c != null) t.setCategory(c); 
                                }
                                future.complete(tasks);
                            })
                            .exceptionally(ex -> {
                                future.completeExceptionally(ex);
                                return null;
                            });
                })
                .addOnFailureListener(future::completeExceptionally);

        return future;
    }
}
