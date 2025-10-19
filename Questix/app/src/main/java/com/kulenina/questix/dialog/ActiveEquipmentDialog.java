package com.kulenina.questix.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kulenina.questix.R;
import com.kulenina.questix.adapter.EquipmentDisplayAdapter;
import com.kulenina.questix.model.Equipment;

import java.util.ArrayList;
import java.util.List;

public class ActiveEquipmentDialog extends DialogFragment {
    private static final String ARG_EQUIPMENT_LIST = "equipment_list";
    private List<Equipment> equipmentList;
    private EquipmentDisplayAdapter adapter;

    public static ActiveEquipmentDialog newInstance(List<Equipment> equipmentList) {
        ActiveEquipmentDialog dialog = new ActiveEquipmentDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_EQUIPMENT_LIST, new ArrayList<>(equipmentList));
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            equipmentList = (List<Equipment>) getArguments().getSerializable(ARG_EQUIPMENT_LIST);
        }
        if (equipmentList == null) {
            equipmentList = new ArrayList<>();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = requireContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_active_equipment, null);

        setupViews(view);

        return new AlertDialog.Builder(context)
            .setTitle("Active Equipment")
            .setView(view)
            .setPositiveButton("Close", (dialog, which) -> dismiss())
            .create();
    }

    private void setupViews(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.rv_equipment_list);
        TextView emptyView = view.findViewById(R.id.tv_empty_equipment);

        if (equipmentList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);

            adapter = new EquipmentDisplayAdapter();

            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setAdapter(adapter);
            adapter.setEquipmentList(equipmentList);
        }
    }
}
