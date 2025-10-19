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
import com.kulenina.questix.R;
import com.kulenina.questix.activity.MainActivity;
import com.kulenina.questix.databinding.FragmentTaskListBinding;
import com.kulenina.questix.model.AppTask;
import com.kulenina.questix.viewmodel.AppTaskViewModel;
import com.kulenina.questix.adapter.TaskListAdapter;

public class TaskListFragment extends Fragment implements TaskListAdapter.TaskActionListener {

    private FragmentTaskListBinding binding;
    private AppTaskViewModel appTaskViewModel;
    private TaskListAdapter adapter;

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

        appTaskViewModel.loadTasksForList();
    }

    private void setupRecyclerView() {
        adapter = new TaskListAdapter(this);
        binding.recyclerViewTasks.setAdapter(adapter);
    }
    public TaskListFragment() {}
    private void setupObservers() {
        appTaskViewModel.getTasksForList().observe(getViewLifecycleOwner(), tasks -> {
            binding.progressBar.setVisibility(View.GONE);

            if (tasks == null || tasks.isEmpty()) {
                binding.tvEmptyState.setVisibility(View.VISIBLE);
                adapter.setTasks(null);
            } else {
                binding.tvEmptyState.setVisibility(View.GONE);
                adapter.setTasks(tasks);
            }
        });

        appTaskViewModel.getErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                appTaskViewModel.clearError();
            }
        });
    }



    @Override
    public void onTaskComplete(String taskId) {
        appTaskViewModel.resolveTask(taskId, AppTask.STATUS_DONE)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(), "Task completed! XP awarded.", Toast.LENGTH_SHORT).show();
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