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
import com.kulenina.questix.model.AppTask; // Za konstante
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

    // Podaci o zadatku
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

        // Inicijalizacija ViewModel-a (koristeći Activity scope, što je uobičajeno)
        appTaskViewModel = new ViewModelProvider(requireActivity()).get(AppTaskViewModel.class);
        categoryViewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);

        // Inicijalizacija kalendara na trenutno vreme
        executionCalendar = Calendar.getInstance();
        startCalendar = (Calendar) executionCalendar.clone();
        endCalendar = Calendar.getInstance();
        endCalendar.add(Calendar.MONTH, 1); // Krajnji datum podrazumevano 1 mesec kasnije

        // Povezivanje UI elemenata
        setupCategorySpinner();
        setupDifficultyAndImportanceSpinners();
        setupDateTimePickers();
        setupRecurringToggle();
        setupCreateButton();

        // Ažuriranje vremena na dugmadima pri pokretanju
        updateDateTimeButtons();
    }

    // --- 1. Učitavanje Kategorija ---

    // U CreateTaskFragment.java, Korigovana metoda setupCategorySpinner()

    private void setupCategorySpinner() {
        categoryViewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null && !categories.isEmpty()) {
                // Logika za Adapter i Listener je ispravna ovde...
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        categories.stream().map(Category::getName).toArray(String[]::new));

                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                binding.spinnerCategory.setAdapter(adapter);

                // Listener je OK...

                // Sada je SIGURNO: Podrazumevani odabir prve kategorije
                selectedCategoryId = categories.get(0).getId();
            } else {
                // Ako nema kategorija, prikaži poruku i postavi ID na null
                Toast.makeText(requireContext(), "Nema kreiranih kategorija. Molimo kreirajte jednu.", Toast.LENGTH_LONG).show();
                selectedCategoryId = null; // Ovde MORA da se postavi na null
            }
        });
        // Poziv za učitavanje kategorija OSTAJE OVDE
        categoryViewModel.loadCategories();
    }

    // --- 2. Odabir Datuma i Vremena ---

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

    // --- 3. Logika Ponavljanja ---

    private void setupRecurringToggle() {
        binding.cbIsRecurring.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.layoutRecurringDetails.setVisibility(View.VISIBLE);
            } else {
                binding.layoutRecurringDetails.setVisibility(View.GONE);
            }
        });

        // Postavljanje jedinica ponavljanja
        ArrayAdapter<CharSequence> unitAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.repetition_units,
                android.R.layout.simple_spinner_item);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerRepetitionUnit.setAdapter(unitAdapter);
    }

    // --- 4. Kreiranje Zadataka ---

    private void setupCreateButton() {
        binding.btnCreateTask.setOnClickListener(v -> {
            if (!validateInputs()) return;

            // 1. Prikupljanje osnovnih podataka
            String name = binding.etTaskName.getText().toString();
            String description = binding.etTaskDescription.getText().toString();
            String difficulty = getSelectedDifficulty(binding.spinnerDifficulty.getSelectedItem().toString());
            String importance = getSelectedImportance(binding.spinnerImportance.getSelectedItem().toString());

            // 2. Podaci o vremenu i ponavljanju
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

            // 3. Poziv ViewModel-a
            appTaskViewModel.createTask(
                            selectedCategoryId, name, difficulty, importance,
                            isRecurring, executionTime, repetitionInterval,
                            repetitionUnit, startDate, endDate, description)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(requireContext(), "Zadatak kreiran uspešno!", Toast.LENGTH_SHORT).show();
                            // Zatvori fragment (navigacija nazad, zavisno od arhitekture)
                            if (getActivity() != null) {
                                getActivity().onBackPressed();
                            }
                        } else {
                            Toast.makeText(requireContext(), "Greška: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    // --- 5. Validacija i Pomoćne metode ---

    private boolean validateInputs() {
        if (binding.etTaskName.getText().toString().trim().isEmpty()) {
            binding.etTaskName.setError("Naziv zadatka je obavezan.");
            return false;
        }
        if (selectedCategoryId == null) {
            Toast.makeText(requireContext(), "Morate odabrati kategoriju.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (binding.cbIsRecurring.isChecked()) {
            String intervalStr = binding.etRepetitionInterval.getText().toString();
            if (intervalStr.isEmpty() || Integer.parseInt(intervalStr) <= 0) {
                binding.etRepetitionInterval.setError("Interval ponavljanja mora biti pozitivan broj.");
                return false;
            }
            if (startCalendar.after(endCalendar)) {
                Toast.makeText(requireContext(), "Početni datum ponavljanja ne može biti nakon krajnjeg.", Toast.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
    }

    /**
     * Konvertuje UI string (srpski) u kod string (engleski)
     */
    private String convertRepetitionUnit(String uiUnit) {
        if (uiUnit.equals("Dan")) {
            return AppTask.UNIT_DAY;
        } else if (uiUnit.equals("Nedelja")) {
            return AppTask.UNIT_WEEK;
        }
        return null;
    }

    /**
     * Izvlaci Difficulty string iz niza (e.g., "Very Easy (1 XP)" -> "Very Easy")
     */
    private String getSelectedDifficulty(String uiText) {
        return uiText.split(" \\(")[0];
    }

    /**
     * Izvlaci Importance string iz niza (e.g., "Normal (1 XP)" -> "Normal")
     */
    private String getSelectedImportance(String uiText) {
        return uiText.split(" \\(")[0];
    }

    public CreateTaskFragment(){}


    private void setupDifficultyAndImportanceSpinners() {
        // 1. Difficulty Spinner
        ArrayAdapter<CharSequence> difficultyAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.task_difficulties, // OVO MORA POSTOJATI U res/values/arrays.xml
                android.R.layout.simple_spinner_item);
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerDifficulty.setAdapter(difficultyAdapter);

        // 2. Importance Spinner
        ArrayAdapter<CharSequence> importanceAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.task_importance, // OVO MORA POSTOJATI U res/values/arrays.xml
                android.R.layout.simple_spinner_item);
        importanceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerImportance.setAdapter(importanceAdapter);
    }
}