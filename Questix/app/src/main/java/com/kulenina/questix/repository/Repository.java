package com.kulenina.questix.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.kulenina.questix.model.IIdentifiable;
import com.kulenina.questix.mapper.Mapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Repository<T extends IIdentifiable> {
    private final FirebaseFirestore db;
    private final String collectionName;
    private final Mapper<T> mapper;

    public Repository(String collectionName, Class<T> clazz) {
			this.db = FirebaseFirestore.getInstance();
			this.collectionName = collectionName;
			this.mapper = new Mapper<T>(clazz);
    }

    public Task<DocumentReference> create(T object) {
      Map<String, Object> data = mapper.toMap(object);
      return db.collection(collectionName).add(data);
    }

    public Task<Void> createWithId(T object) {
      Map<String, Object> data = mapper.toMap(object);
      return db.collection(collectionName).document(object.getId()).set(data);
    }

    public Task<T> read(String id) {
      return db.collection(collectionName)
        .document(id)
        .get()
        .continueWith(task ->
          toObject(task.getResult())
        );
    }

    public Task<QuerySnapshot> readAll() {
      return db.collection(collectionName).get();
    }

    public Task<Void> update(T object) {
      Map<String, Object> data = mapper.toMap(object);
      return db.collection(collectionName).document(object.getId()).update(data);
    }

    public Task<Void> delete(T object) {
      return db.collection(collectionName).document(object.getId()).delete();
    }

    private T toObject(DocumentSnapshot document) {
      if (document == null || !document.exists()) {
        return null;
      }

      String documentId = document.getId();
      Map<String, Object> data = document.getData();
      return mapper.fromMap(data, documentId);
    }
}
