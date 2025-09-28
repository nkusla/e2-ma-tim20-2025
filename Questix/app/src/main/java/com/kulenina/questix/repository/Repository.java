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
    protected final FirebaseFirestore db;
    private final String collectionName;
    protected final Mapper<T> mapper;

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

    public Task<T> read(T object) {
      return read(object.getId());
    }

    public Task<List<T>> readAll() {
      return db.collection(collectionName)
        .get()
        .continueWith(task -> toList(task.getResult()));
    }

    public Task<Void> update(T object) {
      Map<String, Object> data = mapper.toMap(object);
      return db.collection(collectionName).document(object.getId()).update(data);
    }

    public Task<Void> delete(T object) {
      return db.collection(collectionName).document(object.getId()).delete();
    }

    public Task<Void> delete(String id) {
      return db.collection(collectionName).document(id).delete();
    }

    protected T toObject(DocumentSnapshot document) {
      if (document == null || !document.exists()) {
        return null;
      }

      String documentId = document.getId();
      Map<String, Object> data = document.getData();
      return mapper.fromMap(data, documentId);
    }

    public DocumentReference getDocumentReference(String id) {
      return db.collection(collectionName).document(id);
    }

    public DocumentReference getDocumentReference(T object) {
      return getDocumentReference(object.getId());
    }

    protected CollectionReference getCollectionReference() {
      return db.collection(collectionName);
    }

    protected List<T> toList(QuerySnapshot querySnapshot) {
      List<T> objects = new ArrayList<>();
      if (querySnapshot != null) {
        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
          T object = toObject(document);
          if (object != null) {
            objects.add(object);
          }
        }
      }
      return objects;
    }
}
