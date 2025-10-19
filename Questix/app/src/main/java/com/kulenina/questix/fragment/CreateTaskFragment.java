package com.kulenina.questix.fragment;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.kulenina.questix.R;
import com.kulenina.questix.databinding.FragmentCreateTaskBinding;
import com.kulenina.questix.model.Category;
import com.kulenina.questix.model.AppTask;
import com.kulenina.questix.viewmodel.AppTaskViewModel;
import com.kulenina.questix.viewmodel.CategoryViewModel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CreateTaskFragment extends Fragment {

    private FragmentCreateTaskBinding binding;
    private AppTaskViewModel appTaskViewModel;
    private CategoryViewModel categoryViewModel;

    private Calendar executionCalendar;
    private Calendar startCalendar;
    private Calendar endCalendar;
    private String selectedCategoryId = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCreateTaskBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        appTaskViewModel = new ViewModelProvider(requireActivity()).get(AppTaskViewModel.class);
        categoryViewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);

        executionCalendar = Calendar.getInstance();
        startCalendar = (Calendar) executionCalendar.clone();
        endCalendar = Calendar.getInstance();
        endCalendar.add(Calendar.MONTH, 1);

        setupCategorySpinner();
        setupDifficultyAndImportanceSpinners();
        setupDateTimePickers();
        setupRecurringToggle();
        setupCreateButton();

        updateDateTimeButtons();
    }

    private void setupCategorySpinner() {
        categoryViewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null && !categories.isEmpty()) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        categories.stream().map(Category::getName).toArray(String[]::new));

                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                binding.spinnerCategory.setAdapter(adapter);

                binding.spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (position >= 0 && position < categories.size()) {
                            selectedCategoryId = categories.get(position).getId();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });

                selectedCategoryId = categories.get(0).getId();
            } else {
                selectedCategoryId = null;
            }
        });
        categoryViewModel.loadCategories();
    }

    private void setupDateTimePickers() {
        binding.btnSelectDate.setOnClickListener(v -> showDatePicker(executionCalendar, binding.btnSelectDate));
        binding.btnSelectTime.setOnClickListener(v -> showTimePicker(executionCalendar, binding.btnSelectTime));
        binding.btnSelectStartDate.setOnClickListener(v -> showDatePicker(startCalendar, binding.btnSelectStartDate));
        binding.btnSelectEndDate.setOnClickListener(v -> showDatePicker(endCalendar, binding.btnSelectEndDate));
    }

    private void updateDateTimeButtons() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        binding.btnSelectDate.setText(dateFormat.format(executionCalendar.getTime()));
        binding.btnSelectTime.setText(timeFormat.format(executionCalendar.getTime()));
        binding.btnSelectStartDate.setText(dateFormat.format(startCalendar.getTime()));
        binding.btnSelectEndDate.setText(dateFormat.format(endCalendar.getTime()));
    }

    private void showDatePicker(Calendar calendar, View button) {
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            updateDateTimeButtons();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker(Calendar calendar, View button) {
        new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            updateDateTimeButtons();
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private void setupRecurringToggle() {
        binding.cbIsRecurring.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.layoutRecurringDetails.setVisibility(View.VISIBLE);
            } else {
                binding.layoutRecurringDetails.setVisibility(View.GONE);
            }
        });

        ArrayAdapter<CharSequence> unitAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.repetition_units,
                android.R.layout.simple_spinner_item);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerRepetitionUnit.setAdapter(unitAdapter);
    }

    private void setupCreateButton() {
        binding.btnCreateTask.setOnClickListener(v -> {
            if (!validateInputs()) return;

            String name = binding.etTaskName.getText().toString();
            String description = binding.etTaskDescription.getText().toString();
            String difficulty = getSelectedDifficulty(binding.spinnerDifficulty.getSelectedItem().toString());
            String importance = getSelectedImportance(binding.spinnerImportance.getSelectedItem().toString());

            long executionTime = executionCalendar.getTimeInMillis();
            boolean isRecurring = binding.cbIsRecurring.isChecked();
            Integer repetitionInterval = null;
            String repetitionUnit = null;
            long startDate = 0;
            long endDate = 0;

            if (isRecurring) {
                String intervalStr = binding.etRepetitionInterval.getText().toString();
                if (!intervalStr.isEmpty()) {
                    repetitionInterval = Integer.parseInt(intervalStr);
                }
                repetitionUnit = convertRepetitionUnit(binding.spinnerRepetitionUnit.getSelectedItem().toString());
                startDate = startCalendar.getTimeInMillis();
                endDate = endCalendar.getTimeInMillis();
            }

            appTaskViewModel.createTask(
                            selectedCategoryId, name, difficulty, importance,
                            isRecurring, executionTime, repetitionInterval,
                            repetitionUnit, startDate, endDate, description)
                    .addOnCompleteListener(task -> {
                        if (!isAdded() || getContext() == null) {
                            return;
                        }

                        if (task.isSuccessful()) {
                            Toast.makeText(requireContext(), "Task created successfully!", Toast.LENGTH_SHORT).show();
                            resetForm();
                        } else {
                            String errorMessage = "Unknown error";
                            if (task.getException() != null && task.getException().getMessage() != null) {
                                errorMessage = task.getException().getMessage();
                            }
                            Toast.makeText(requireContext(), "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    private boolean validateInputs() {
        if (binding.etTaskName.getText().toString().trim().isEmpty()) {
            binding.etTaskName.setError("Task name is required.");
            return false;
        }
        if (selectedCategoryId == null) {
            Toast.makeText(requireContext(), "You must select a category.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (binding.cbIsRecurring.isChecked()) {
            String intervalStr = binding.etRepetitionInterval.getText().toString();
            if (intervalStr.isEmpty() || Integer.parseInt(intervalStr) <= 0) {
                binding.etRepetitionInterval.setError("Repetition interval must be a positive number.");
                return false;
            }
            if (startCalendar.after(endCalendar)) {
                Toast.makeText(requireContext(), "Start date cannot be after end date.", Toast.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
    }

    private String convertRepetitionUnit(String uiUnit) {
        if (uiUnit.equals("Day")) {
            return AppTask.UNIT_DAY;
        } else if (uiUnit.equals("Week")) {
            return AppTask.UNIT_WEEK;
        }
        return null;
    }

    private String getSelectedDifficulty(String uiText) {
        return uiText.split(" \\(")[0];
    }

    private String getSelectedImportance(String uiText) {
        return uiText.split(" \\(")[0];
    }

    public CreateTaskFragment(){}


    private void setupDifficultyAndImportanceSpinners() {
        ArrayAdapter<CharSequence> difficultyAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.task_difficulties,
                android.R.layout.simple_spinner_item);
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerDifficulty.setAdapter(difficultyAdapter);

        ArrayAdapter<CharSequence> importanceAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.task_importance,
                android.R.layout.simple_spinner_item);
        importanceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerImportance.setAdapter(importanceAdapter);
    }

    private void resetForm() {
        binding.etTaskName.setText("");
        binding.etTaskDescription.setText("");
        binding.etRepetitionInterval.setText("");

        binding.cbIsRecurring.setChecked(false);
        binding.layoutRecurringDetails.setVisibility(View.GONE);

        binding.spinnerDifficulty.setSelection(0);
        binding.spinnerImportance.setSelection(0);
        binding.spinnerRepetitionUnit.setSelection(0);

        binding.spinnerCategory.setSelection(0);

        executionCalendar = Calendar.getInstance();
        startCalendar = (Calendar) executionCalendar.clone();
        endCalendar = Calendar.getInstance();
        endCalendar.add(Calendar.MONTH, 1);

        updateDateTimeButtons();

        binding.etTaskName.setError(null);
        binding.etRepetitionInterval.setError(null);
    }
}