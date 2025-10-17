package com.kulenina.questix.fragment;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
//import androidx.navigation.NavController;
//import androidx.navigation.Navigation;
import com.kulenina.questix.R;
import com.kulenina.questix.databinding.FragmentCalendarBinding;
import com.kulenina.questix.adapter.TaskListAdapter;
import com.kulenina.questix.viewmodel.AppTaskViewModel;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.text.SimpleDateFormat;
import com.kulenina.questix.fragment.TaskDetailFragment;

public class CalendarFragment extends Fragment implements TaskListAdapter.TaskActionListener {

    private FragmentCalendarBinding binding;
    private AppTaskViewModel appTaskViewModel;
    private TaskListAdapter adapter;
    //private NavController navController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //navController = Navigation.findNavController(view);
        appTaskViewModel = new ViewModelProvider(requireActivity()).get(AppTaskViewModel.class);

        setupRecyclerView();
        setupCalendarView();
        setupObservers();

        // Inicijalno učitavanje zadataka za današnji datum
        loadTasksForSelectedDate(System.currentTimeMillis());
    }

    private void setupRecyclerView() {
        adapter = new TaskListAdapter(this);
        binding.recyclerViewDayTasks.setAdapter(adapter);
    }

    public CalendarFragment(){}
    private void setupCalendarView() {
        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar selectedDate = Calendar.getInstance();
            // Postavljamo datum na početak dana (00:00:00)
            selectedDate.set(year, month, dayOfMonth, 0, 0, 0);
            selectedDate.set(Calendar.MILLISECOND, 0);

            // Učitavamo zadatke za odabrani datum
            loadTasksForSelectedDate(selectedDate.getTimeInMillis());
        });
    }

    private void setupObservers() {
        // ViewModel mora obezbediti LiveData za zadatke na odabrani datum
        appTaskViewModel.getTasksForSelectedDate().observe(getViewLifecycleOwner(), tasks -> {
            if (tasks == null || tasks.isEmpty()) {
                binding.tvEmptyStateCalendar.setVisibility(View.VISIBLE);
                adapter.setTasks(null);
            } else {
                binding.tvEmptyStateCalendar.setVisibility(View.GONE);
                adapter.setTasks(tasks);
            }
        });

        appTaskViewModel.getErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), "Greška: " + error, Toast.LENGTH_LONG).show();
                appTaskViewModel.clearError();
            }
        });
    }

    /**
     * Učitava zadatke (ili ponavljanja) koji su zakazani za određeni datum.
     * @param dateInMillis Milisekunde odabranog datuma (na početku dana).
     */
    private void loadTasksForSelectedDate(long dateInMillis) {
        // Dobijamo kraj dana (23:59:59.999) za pretragu
        long dateEndInMillis = dateInMillis + TimeUnit.DAYS.toMillis(1) - 1;

        // Pozivamo ViewModel. Ova metoda u VM mora da izračuna ponavljanja
        // i vrati listu AppTask objekata koji padaju u taj dan.
        appTaskViewModel.loadTaskOccurrencesByDateRange(dateInMillis, dateEndInMillis);

        // Ažuriranje teksta iznad liste
        SimpleDateFormat dateFormat = new SimpleDateFormat("EE, dd. MMM yyyy.", Locale.getDefault());
        binding.tvTasksForDate.setText(String.format("Zadaci za %s:", dateFormat.format(new Date(dateInMillis))));
    }

    // --- TaskActionListener Implementacija (isti kao u TaskListFragment) ---

    @Override
    public void onTaskComplete(String taskId) {
        // Implementiramo istu logiku završavanja, a zatim ponovo učitavamo podatke za tekući datum
        appTaskViewModel.resolveTask(taskId, com.kulenina.questix.model.AppTask.STATUS_DONE)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(), "Zadatak završen!", Toast.LENGTH_SHORT).show();

                        // Ponovo učitavamo zadatke za trenutno odabrani datum
                        long selectedDate = binding.calendarView.getDate();
                        loadTasksForSelectedDate(selectedDate);

                    } else {
                        Toast.makeText(requireContext(), "Greška: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onTaskClick(String taskId) {
        TaskDetailFragment fragment = TaskDetailFragment.newInstance(taskId);
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null); // da bi mogli da se vratite na prethodni fragment
        transaction.commit();
    }

}