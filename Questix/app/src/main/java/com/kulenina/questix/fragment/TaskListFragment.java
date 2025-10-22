package com.kulenina.questix.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
//import androidx.navigation.NavController;
//import androidx.navigation.Navigation;
import com.google.android.material.tabs.TabLayout;
import com.kulenina.questix.R;
import com.kulenina.questix.activity.MainActivity;
import com.kulenina.questix.databinding.FragmentTaskListBinding;
import com.kulenina.questix.model.AppTask;
import com.kulenina.questix.viewmodel.AppTaskViewModel;
import com.kulenina.questix.adapter.TaskListAdapter;

import java.util.ArrayList;
import java.util.List;

public class TaskListFragment extends Fragment implements TaskListAdapter.TaskActionListener {

    private FragmentTaskListBinding binding;
    private AppTaskViewModel appTaskViewModel;
    private TaskListAdapter adapter;
    //private NavController navController;
    
    // Filtering variables
    private List<AppTask> allTasks = new ArrayList<>();
    private String currentFilter = "ALL"; // ALL, RECURRING, NON_RECURRING

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentTaskListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        appTaskViewModel = new ViewModelProvider(requireActivity()).get(AppTaskViewModel.class);

        setupRecyclerView();
        setupObservers();
        setupFilterTabs();

        appTaskViewModel.loadTasksForList();
        
        // Proveri i označi stare zadatke kao "undone"
        appTaskViewModel.checkMissedTasks()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Ponovo učitaj listu nakon provere
                        appTaskViewModel.loadTasksForList();
                    }
                });
    }

    private void setupRecyclerView() {
        adapter = new TaskListAdapter(this);
        binding.recyclerViewTasks.setAdapter(adapter);
    }
    
    private void setupFilterTabs() {
        // Add tabs
        binding.tabLayoutFilter.addTab(binding.tabLayoutFilter.newTab().setText("All Tasks"));
        binding.tabLayoutFilter.addTab(binding.tabLayoutFilter.newTab().setText("Recurring"));
        binding.tabLayoutFilter.addTab(binding.tabLayoutFilter.newTab().setText("One-time"));

        // Set default selection
        binding.tabLayoutFilter.selectTab(binding.tabLayoutFilter.getTabAt(0));

        // Add tab selection listener
        binding.tabLayoutFilter.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        currentFilter = "ALL";
                        break;
                    case 1:
                        currentFilter = "RECURRING";
                        break;
                    case 2:
                        currentFilter = "NON_RECURRING";
                        break;
                }
                applyFilter();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }
    
    private void applyFilter() {
        List<AppTask> filteredTasks = new ArrayList<>();
        
        // First filter: Show active tasks (including those within 3-day grace period)
        List<AppTask> activeTasks = new ArrayList<>();
        long now = System.currentTimeMillis();
        
        for (AppTask task : allTasks) {
            // Include tasks that are active (including those within 3-day grace period)
            if (task.isActive()) {
                activeTasks.add(task);
            }
        }
        
        // Second filter: Apply user-selected filter (ALL, RECURRING, NON_RECURRING)
        switch (currentFilter) {
            case "ALL":
                filteredTasks = new ArrayList<>(activeTasks);
                break;
            case "RECURRING":
                for (AppTask task : activeTasks) {
                    if (task.isRecurring) {
                        filteredTasks.add(task);
                    }
                }
                break;
            case "NON_RECURRING":
                for (AppTask task : activeTasks) {
                    if (!task.isRecurring) {
                        filteredTasks.add(task);
                    }
                }
                break;
        }
        
        // Update UI
        if (filteredTasks.isEmpty()) {
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            adapter.setTasks(null);
        } else {
            binding.tvEmptyState.setVisibility(View.GONE);
            adapter.setTasks(filteredTasks);
        }
    }
    
    public TaskListFragment() {}
    private void setupObservers() {
        appTaskViewModel.getTasksForList().observe(getViewLifecycleOwner(), tasks -> {
            binding.progressBar.setVisibility(View.GONE);

            // Store all tasks for filtering
            allTasks = tasks != null ? new ArrayList<>(tasks) : new ArrayList<>();
            
            // Apply current filter
            applyFilter();
        });

        appTaskViewModel.getErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                appTaskViewModel.clearError(); // Obriši grešku nakon prikaza
            }
        });
    }



    @Override
    public void onTaskComplete(String taskId) {
        // Poziva servis da označi zadatak kao završen (AppTask.STATUS_DONE)
        appTaskViewModel.markTaskAsDone(taskId)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(), "Task completed! XP awarded.", Toast.LENGTH_SHORT).show();
                        // Potrebno je ponovo učitati listu da bi se ažurirala
                        appTaskViewModel.loadTasksForList();
                    } else {
                        Toast.makeText(requireContext(), "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onTaskCancel(String taskId) {
        // Poziva servis da otkaže zadatak (AppTask.STATUS_CANCELED)
        appTaskViewModel.cancelTask(taskId)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(), "Task canceled.", Toast.LENGTH_SHORT).show();
                        appTaskViewModel.loadTasksForList();
                    } else {
                        Toast.makeText(requireContext(), "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }


    @Override
    public void onTaskPause(String taskId) {
        // Poziva servis da pauzira ponavljajući zadatak (AppTask.STATUS_PAUSED)
        appTaskViewModel.pauseTask(taskId)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(), "Recurring task paused.", Toast.LENGTH_SHORT).show();
                        appTaskViewModel.loadTasksForList();
                    } else {
                        Toast.makeText(requireContext(), "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onTaskResume(String taskId) {
        // Poziva servis da nastavi ponavljajući zadatak (AppTask.STATUS_ACTIVE)
        appTaskViewModel.resumeTask(taskId)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(), "Recurring task resumed.", Toast.LENGTH_SHORT).show();
                        appTaskViewModel.loadTasksForList();
                    } else {
                        Toast.makeText(requireContext(), "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onTaskClick(String taskId) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showTaskDetail(taskId);
        } else {
            Toast.makeText(requireContext(), "Error opening task details", Toast.LENGTH_SHORT).show();
        }
    }
}