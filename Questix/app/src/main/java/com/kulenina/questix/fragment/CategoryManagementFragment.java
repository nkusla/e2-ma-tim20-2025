package com.kulenina.questix.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.Observable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kulenina.questix.BR;
import com.kulenina.questix.R;
import com.kulenina.questix.adapter.CategoryAdapter;
import com.kulenina.questix.model.Category;
import com.kulenina.questix.viewmodel.CategoryViewModel;

public class CategoryManagementFragment extends Fragment implements CategoryAdapter.CategoryActionListener {

    private CategoryViewModel viewModel;
    private CategoryAdapter adapter;
    private ProgressBar progressBar;
    private FloatingActionButton fabAddCategory;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_category_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewCategories);
        progressBar = view.findViewById(R.id.progressBar);
        fabAddCategory = view.findViewById(R.id.fabAddCategory);

        viewModel = new CategoryViewModel();

        adapter = new CategoryAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        viewModel.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                if (propertyId == BR.categories) {
                    adapter.setCategories(viewModel.getCategories());
                }
                else if (propertyId == BR.isLoading) {
                    progressBar.setVisibility(viewModel.getIsLoading() ? View.VISIBLE : View.GONE);
                }
                else if (propertyId == BR.errorMessage) {
                    String errorMessage = viewModel.getErrorMessage();
                    if (errorMessage != null && !errorMessage.isEmpty()) {
                        Toast.makeText(getContext(), "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                        viewModel.clearErrorMessage();
                    }
                }
            }
        });

        fabAddCategory.setOnClickListener(v -> showCreateCategoryDialog(getContext()));
    }

    @Override
    public void onDeleteClicked(Category category) {
        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.category_delete_title))
                .setMessage(getString(R.string.category_delete_message, category.getName()))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    viewModel.deleteCategory(category.getId());
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onColorChangeClicked(Category category) {
        showColorPickerDialog(getContext(), category);
    }

    private void showCreateCategoryDialog(Context context) {
        final View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_category, null);
        final EditText etName = dialogView.findViewById(R.id.editTextCategoryName);
        final EditText etColor = dialogView.findViewById(R.id.editTextCategoryColorHex);

        new AlertDialog.Builder(context)
                .setTitle("Create New Category")
                .setView(dialogView)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String colorHex = etColor.getText().toString().trim();

                    if (!name.isEmpty() && colorHex.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")) {
                        viewModel.createCategory(name, colorHex);
                    } else {
                        Toast.makeText(context, "Invalid name or Hex color format (e.g., #FF00AA)", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showColorPickerDialog(Context context, Category category) {
        final View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_change_color, null);
        final EditText etNewColor = dialogView.findViewById(R.id.editTextNewColorHex);
        etNewColor.setText(category.getColorHex());

        new AlertDialog.Builder(context)
                .setTitle("Change Color for: " + category.getName())
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newColorHex = etNewColor.getText().toString().trim();

                    if (newColorHex.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")) {
                        viewModel.updateCategoryColor(category.getId(), newColorHex);
                    } else {
                        Toast.makeText(context, "Invalid Hex color format (e.g., #FF00AA)", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}