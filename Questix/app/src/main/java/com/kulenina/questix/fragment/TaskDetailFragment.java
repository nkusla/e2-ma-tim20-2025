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
            Toast.makeText(requireContext(), "Greška: ID zadatka nije prosleđen.", Toast.LENGTH_SHORT).show();
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }

        String taskId = getArguments() != null ? getArguments().getString("taskId") : null;

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
                Toast.makeText(requireContext(), "Zadatak nije pronađen.", Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).popBackStack();
            }
        });

        // Observator za kategoriju (Potrebno za prikaz imena kategorije)
        categoryViewModel.getCategoryById(currentTask.categoryId).observe(getViewLifecycleOwner(), category -> {
            if (category != null) {
                binding.tvCategoryName.setText(category.getName());
            }
        });

        // Observator za greške
        appTaskViewModel.getErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), "Greška: " + error, Toast.LENGTH_LONG).show();
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
                binding.btnPauseResume.setText("Nastavi");
                binding.btnPauseResume.setBackgroundResource(R.color.colorSuccess);
            } else {
                binding.btnPauseResume.setText("Pauziraj");
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
     * Izvlaci Difficulty/Importance string iz niza (e.g., "Very Easy (1 XP)" -> "Very Easy")
     */
    private String getSelectedText(String uiText) {
        return uiText.split(" \\(")[0];
    }

    // --- Logika Dugmadi (Izmena, Brisanje, Pauza/Nastavak) ---

    private void setupButtons() {
        binding.btnUpdateTask.setOnClickListener(v -> handleUpdate());
        binding.btnDeleteTask.setOnClickListener(v -> handleDelete());
        binding.btnPauseResume.setOnClickListener(v -> handlePauseResume());
    }

    private void handleUpdate() {
        if (currentTask == null || currentTask.isFinished()) return;

        String name = binding.etTaskName.getText().toString().trim();
        String description = binding.etTaskDescription.getText().toString();

        if (name.isEmpty()) {
            binding.etTaskName.setError("Naziv zadatka je obavezan.");
            return;
        }

        String difficulty = getSelectedText(binding.spinnerDifficulty.getSelectedItem().toString());
        String importance = getSelectedText(binding.spinnerImportance.getSelectedItem().toString());
        long newExecutionTime = executionCalendar.getTimeInMillis();

        // Pozivamo servis za izmenu
        appTaskViewModel.updateTask(
                        currentTask.id, name, description, newExecutionTime,
                        difficulty, importance)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(), "Zadatak uspešno izmenjen.", Toast.LENGTH_SHORT).show();
                        NavHostFragment.findNavController(this).popBackStack();
                    } else {
                        Toast.makeText(requireContext(), "Greška pri izmeni: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleDelete() {
        // Obično se ovde dodaje AlertDialog za potvrdu brisanja
        if (currentTask == null || currentTask.isFinished()) return;

        appTaskViewModel.deleteTask(currentTask.id)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(), "Zadatak uspešno obrisan.", Toast.LENGTH_SHORT).show();
                        NavHostFragment.findNavController(this).popBackStack();
                    } else {
                        Toast.makeText(requireContext(), "Greška pri brisanju: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handlePauseResume() {
        if (currentTask == null || !currentTask.isRecurring) return;

        String newStatus = currentTask.status.equals(AppTask.STATUS_PAUSED) ? AppTask.STATUS_ACTIVE : AppTask.STATUS_PAUSED;

        appTaskViewModel.resolveTask(currentTask.id, newStatus)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(), "Status zadatka promenjen na: " + newStatus, Toast.LENGTH_SHORT).show();
                        // Pošto se status menja, ponovo učitavamo detalje da bismo osvežili UI
                        appTaskViewModel.loadTaskDetails(currentTask.id);
                    } else {
                        Toast.makeText(requireContext(), "Greška pri promeni statusa: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
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
