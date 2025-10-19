package com.kulenina.questix.fragment;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.kulenina.questix.R;
import com.kulenina.questix.databinding.FragmentTaskDetailBinding;
import com.kulenina.questix.model.AppTask;
import com.kulenina.questix.viewmodel.AppTaskViewModel;
import com.kulenina.questix.viewmodel.CategoryViewModel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class TaskDetailFragment extends Fragment {

    private FragmentTaskDetailBinding binding;
    private AppTaskViewModel appTaskViewModel;
    private CategoryViewModel categoryViewModel;

    private String taskId;
    private AppTask currentTask;
    private Calendar executionCalendar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentTaskDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Dohvatanje taskId iz argumenata
        if (getArguments() != null) {
            taskId = getArguments().getString("taskId");
        }

        if (taskId == null) {
            Toast.makeText(requireContext(), "Error: Task ID not provided.", Toast.LENGTH_SHORT).show();
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }


        appTaskViewModel = new ViewModelProvider(requireActivity()).get(AppTaskViewModel.class);
        categoryViewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);

        // Inicijalizacija kalendara
        executionCalendar = Calendar.getInstance();

        setupObservers();
        setupDateTimePickers();
        setupButtons();

        // Učitavanje zadatka
        appTaskViewModel.loadTaskDetails(taskId);
    }

    private void setupObservers() {
        // Observator za detalje zadatka
        appTaskViewModel.getTaskDetails().observe(getViewLifecycleOwner(), task -> {
            if (task != null) {
                currentTask = task;
                populateUI(task);
            } else {
                Toast.makeText(requireContext(), "Task not found.", Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).popBackStack();
            }
        });

        // Observator za kategoriju (Potrebno za prikaz imena kategorije)
        // Moved to populateUI method to avoid null pointer exception

        // Observator za greške
        appTaskViewModel.getErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                appTaskViewModel.clearError();
            }
        });
    }

    private void populateUI(AppTask task) {
        binding.etTaskName.setText(task.name);
        binding.etTaskDescription.setText(task.description);
        binding.tvTaskStatus.setText("Status: " + task.status);

        // Inicijalizacija kalendara sa vremenom izvršenja
        executionCalendar.setTimeInMillis(task.executionTime);
        updateDateTimeButtons();

        // Postavljanje spinera na trenutne vrednosti
        setSpinnerSelection(binding.spinnerDifficulty, R.array.task_difficulties, task.difficulty);
        setSpinnerSelection(binding.spinnerImportance, R.array.task_importance, task.importance);

        // Observator za kategoriju (moved here to avoid null pointer)
        if (task.categoryId != null) {
            categoryViewModel.getCategoryById(task.categoryId).observe(getViewLifecycleOwner(), category -> {
                if (category != null) {
                    binding.tvCategoryName.setText(category.getName());
                }
            });
        }

        // Ažuriranje dugmeta Pauza/Nastavak
        if (task.isFinished()) {
            // Zabranjujemo izmene i akcije za završen/propušten zadatak
            binding.btnUpdateTask.setEnabled(false);
            binding.btnPauseResume.setVisibility(View.GONE);
            binding.btnDeleteTask.setVisibility(View.GONE); // Ne može se brisati završen
            binding.etTaskName.setEnabled(false);
            binding.etTaskDescription.setEnabled(false);
        } else if (task.isRecurring) {
            // Ponavljajući zadaci mogu se pauzirati/nastaviti
            binding.btnPauseResume.setVisibility(View.VISIBLE);
            if (task.status.equals(AppTask.STATUS_PAUSED)) {
                binding.btnPauseResume.setText("Resume");
                binding.btnPauseResume.setBackgroundResource(R.color.colorSuccess);
            } else {
                binding.btnPauseResume.setText("Pause");
                binding.btnPauseResume.setBackgroundResource(R.color.colorWarning);
            }
        } else {
            binding.btnPauseResume.setVisibility(View.GONE);
        }
    }

    // --- Pomoćne metode za UI ---

    private void setSpinnerSelection(android.widget.Spinner spinner, int arrayId, String targetValue) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(), arrayId, android.R.layout.simple_spinner_item);

        for (int i = 0; i < adapter.getCount(); i++) {
            // Upoređujemo samo prvi deo stringa (npr. "Very Easy" iz "Very Easy (1 XP)")
            String spinnerItem = adapter.getItem(i).toString().split(" \\(")[0];
            if (spinnerItem.equals(targetValue)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void setupDateTimePickers() {
        binding.btnSelectDate.setOnClickListener(v -> showDatePicker(executionCalendar));
        binding.btnSelectTime.setOnClickListener(v -> showTimePicker(executionCalendar));
    }

    private void updateDateTimeButtons() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        binding.btnSelectDate.setText(dateFormat.format(executionCalendar.getTime()));
        binding.btnSelectTime.setText(timeFormat.format(executionCalendar.getTime()));
    }

    private void showDatePicker(Calendar calendar) {
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            updateDateTimeButtons();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker(Calendar calendar) {
        new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            updateDateTimeButtons();
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    /**
     * Extracts Difficulty/Importance string from array (e.g., "Very Easy (1 XP)" -> "Very Easy")
     */
    private String getSelectedText(String uiText) {
        System.out.println("DEBUG: getSelectedText() called with: '" + uiText + "'");
        
        if (uiText == null || uiText.isEmpty()) {
            System.out.println("DEBUG: UI text is null or empty");
            throw new IllegalArgumentException("UI text is null or empty");
        }
        
        String[] parts = uiText.split(" \\(");
        System.out.println("DEBUG: Split result: " + java.util.Arrays.toString(parts));
        
        if (parts.length == 0) {
            System.out.println("DEBUG: Invalid UI text format");
            throw new IllegalArgumentException("Invalid UI text format: " + uiText);
        }
        
        String result = parts[0].trim();
        System.out.println("DEBUG: Extracted '" + result + "' from '" + uiText + "'");
        return result;
    }

    // --- Logika Dugmadi (Izmena, Brisanje, Pauza/Nastavak) ---

    private void setupButtons() {
        binding.btnUpdateTask.setOnClickListener(v -> handleUpdate());
        binding.btnDeleteTask.setOnClickListener(v -> handleDelete());
        binding.btnPauseResume.setOnClickListener(v -> handlePauseResume());
    }

    private void handleUpdate() {
        try {
            System.out.println("DEBUG: handleUpdate() started");
            
            if (currentTask == null) {
                Toast.makeText(requireContext(), "No task selected.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentTask.isFinished()) {
                Toast.makeText(requireContext(), "Cannot update finished tasks.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentTask.isUndone()) {
                Toast.makeText(requireContext(), "Cannot update undone tasks.", Toast.LENGTH_SHORT).show();
                return;
            }

            System.out.println("DEBUG: Task validation passed");

            String name = binding.etTaskName.getText().toString().trim();
            String description = binding.etTaskDescription.getText().toString();

            if (name.isEmpty()) {
                binding.etTaskName.setError("Task name is required.");
                return;
            }

            System.out.println("DEBUG: Name validation passed: '" + name + "'");

            // Get spinner values with null checks
            Object difficultyObj = binding.spinnerDifficulty.getSelectedItem();
            Object importanceObj = binding.spinnerImportance.getSelectedItem();
            
            System.out.println("DEBUG: Difficulty object: " + difficultyObj);
            System.out.println("DEBUG: Importance object: " + importanceObj);
            
            if (difficultyObj == null || importanceObj == null) {
                Toast.makeText(requireContext(), "Please select difficulty and importance.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String difficulty = getSelectedText(difficultyObj.toString());
            String importance = getSelectedText(importanceObj.toString());
            long newExecutionTime = executionCalendar.getTimeInMillis();

            // Debug logging
            System.out.println("DEBUG: Difficulty: '" + difficulty + "', Importance: '" + importance + "'");
            System.out.println("DEBUG: Execution time: " + newExecutionTime);
            System.out.println("DEBUG: Task ID: " + currentTask.id);

            // Pozivamo servis za izmenu
            System.out.println("DEBUG: Calling appTaskViewModel.updateTask()");
            appTaskViewModel.updateTask(
                            currentTask.id, name, description, newExecutionTime,
                            difficulty, importance)
                    .addOnCompleteListener(task -> {
                        System.out.println("DEBUG: updateTask completed, success: " + task.isSuccessful());
                        if (task.isSuccessful()) {
                            Toast.makeText(requireContext(), "Task updated successfully.", Toast.LENGTH_SHORT).show();
                            NavHostFragment.findNavController(this).popBackStack();
                        } else {
                            String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                            System.out.println("DEBUG: Update failed with error: " + errorMsg);
                            Toast.makeText(requireContext(), "Error updating task: " + errorMsg, Toast.LENGTH_LONG).show();
                        }
                    });
        } catch (Exception e) {
            System.out.println("DEBUG: Exception in handleUpdate: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error preparing update: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void handleDelete() {
        if (currentTask == null) {
            Toast.makeText(requireContext(), "No task selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentTask.isFinished()) {
            Toast.makeText(requireContext(), "Cannot delete finished tasks.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentTask.isUndone()) {
            Toast.makeText(requireContext(), "Cannot delete undone tasks.", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Add confirmation dialog here
        appTaskViewModel.deleteTask(currentTask.id)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(), "Task deleted successfully.", Toast.LENGTH_SHORT).show();
                        NavHostFragment.findNavController(this).popBackStack();
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(requireContext(), "Error deleting task: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handlePauseResume() {
        if (currentTask == null) {
            Toast.makeText(requireContext(), "No task selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!currentTask.isRecurring) {
            Toast.makeText(requireContext(), "Only recurring tasks can be paused/resumed.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentTask.isFinished()) {
            Toast.makeText(requireContext(), "Cannot pause/resume finished tasks.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentTask.isUndone()) {
            Toast.makeText(requireContext(), "Cannot pause/resume undone tasks.", Toast.LENGTH_SHORT).show();
            return;
        }

        String newStatus = currentTask.status.equals(AppTask.STATUS_PAUSED) ? AppTask.STATUS_ACTIVE : AppTask.STATUS_PAUSED;

        appTaskViewModel.resolveTask(currentTask.id, newStatus)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(), "Task status changed to: " + newStatus, Toast.LENGTH_SHORT).show();
                        // Pošto se status menja, ponovo učitavamo detalje da bismo osvežili UI
                        appTaskViewModel.loadTaskDetails(currentTask.id);
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(requireContext(), "Error changing status: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    public static TaskDetailFragment newInstance(String taskId) {
        TaskDetailFragment fragment = new TaskDetailFragment();
        Bundle args = new Bundle();
        args.putString("taskId", taskId);
        fragment.setArguments(args);
        return fragment;
    }

}
