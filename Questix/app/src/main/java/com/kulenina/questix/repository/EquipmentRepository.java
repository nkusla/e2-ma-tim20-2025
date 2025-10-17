package com.kulenina.questix.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.kulenina.questix.mapper.Mapper;
import com.kulenina.questix.model.Clothing;
import com.kulenina.questix.model.Equipment;
import com.kulenina.questix.model.Potion;
import com.kulenina.questix.model.Weapon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EquipmentRepository extends Repository<Equipment> {
    private final Mapper<Potion> potionMapper;
    private final Mapper<Weapon> weaponMapper;
    private final Mapper<Clothing> clothingMapper;

    public EquipmentRepository() {
        super("equipment", Equipment.class);
        this.potionMapper = new Mapper<>(Potion.class);
        this.weaponMapper = new Mapper<>(Weapon.class);
        this.clothingMapper = new Mapper<>(Clothing.class);
    }

    @Override
    public Task<DocumentReference> create(Equipment object) {
        Map<String, Object> data = toMap(object);
        return db.collection("equipment").add(data);
    }

    @Override
    public Task<Void> createWithId(Equipment object) {
        Map<String, Object> data = toMap(object);
        return db.collection("equipment").document(object.getId()).set(data);
    }

    @Override
    public Task<Void> update(Equipment object) {
        Map<String, Object> data = toMap(object);
        return db.collection("equipment").document(object.getId()).update(data);
    }

    private Map<String, Object> toMap(Equipment object) {
        if (object instanceof Potion) {
            return potionMapper.toMap((Potion) object);
        } else if (object instanceof Weapon) {
            return weaponMapper.toMap((Weapon) object);
        } else if (object instanceof Clothing) {
            return clothingMapper.toMap((Clothing) object);
        } else {
            return mapper.toMap(object);
        }
    }

    @Override
    protected Equipment toObject(DocumentSnapshot document) {
        if (document == null || !document.exists()) {
            return null;
        }

        String documentId = document.getId();
        Map<String, Object> data = document.getData();

        if (data == null) {
            return null;
        }

        String typeStr = (String) data.get("type");
        if (typeStr == null) {
            return mapper.fromMap(data, documentId);
        }

        Equipment.EquipmentType type;
        try {
            type = Equipment.EquipmentType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            return mapper.fromMap(data, documentId);
        }

        switch (type) {
            case POTION:
                return potionMapper.fromMap(data, documentId);
            case WEAPON:
                return weaponMapper.fromMap(data, documentId);
            case CLOTHING:
                return clothingMapper.fromMap(data, documentId);
            default:
                return mapper.fromMap(data, documentId);
        }
    }

    @Override
    protected List<Equipment> toList(QuerySnapshot querySnapshot) {
        List<Equipment> objects = new ArrayList<>();
        if (querySnapshot != null) {
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                Equipment object = toObject(document);
                if (object != null) {
                    objects.add(object);
                }
            }
        }
        return objects;
    }

    public Task<List<Equipment>> findByUserId(String userId) {
        return getCollectionReference()
                .whereEqualTo("userId", userId)
                .get()
                .continueWith(task -> toList(task.getResult()));
    }

    public Task<List<Equipment>> findActiveByUserId(String userId) {
        return getCollectionReference()
                .whereEqualTo("userId", userId)
                .whereEqualTo("isActive", true)
                .get()
                .continueWith(task -> toList(task.getResult()));
    }
}
