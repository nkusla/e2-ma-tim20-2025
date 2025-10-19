package com.kulenina.questix.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.kulenina.questix.R;
import com.kulenina.questix.databinding.TaskListItemBinding;
import com.kulenina.questix.model.AppTask;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TaskListAdapter extends RecyclerView.Adapter<TaskListAdapter.TaskViewHolder> {

    private List<AppTask> taskList;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd. MMM yyyy. - HH:mm", Locale.getDefault());
    private final TaskActionListener listener;

    public interface TaskActionListener {
        void onTaskComplete(String taskId);
        void onTaskClick(String taskId);
        void onTaskCancel(String taskId);
        void onTaskPause(String taskId);
        void onTaskResume(String taskId);
    }

    public TaskListAdapter(TaskActionListener listener) {
        this.listener = listener;
    }

    public void setTasks(List<AppTask> tasks) {
        this.taskList = tasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TaskListItemBinding binding = TaskListItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new TaskViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        holder.bind(taskList.get(position));
    }

    @Override
    public int getItemCount() {
        return taskList != null ? taskList.size() : 0;
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        private final TaskListItemBinding binding;

        public TaskViewHolder(TaskListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(AppTask task) {
            binding.tvTaskName.setText(task.name);
            binding.tvExecutionTime.setText(dateFormat.format(task.executionTime));

            // Prikaz XP i Ponavljanja
            String recurrenceInfo = task.isRecurring ?
                    String.format("(%d %s)", task.repetitionInterval, task.repetitionUnit) : "Non-recurring";
            String xpInfo = String.format("%s | %d XP", recurrenceInfo, task.totalXpValue);
            binding.tvRecurrenceXp.setText(xpInfo);

            // Boja Kategorije
            try {
                binding.categoryColorIndicator.setBackgroundColor(Color.parseColor(task.colorHex));
            } catch (IllegalArgumentException | NullPointerException e) {
                // Ako je boja neispravna, koristi se podrazumevana
                binding.categoryColorIndicator.setBackgroundColor(itemView.getContext().getResources().getColor(R.color.colorAccent));
            }

            // Vizuelno označavanje završenih zadataka
            if (task.isDone()) {
                // Završeni zadaci - sivkasta boja i disabled dugmad
                binding.tvTaskName.setTextColor(itemView.getContext().getResources().getColor(R.color.colorTextSecondary));
                binding.tvExecutionTime.setTextColor(itemView.getContext().getResources().getColor(R.color.colorTextSecondary));
                binding.tvRecurrenceXp.setTextColor(itemView.getContext().getResources().getColor(R.color.colorTextSecondary));

                // Disable dugmad za završene zadatke
                binding.btnComplete.setEnabled(false);
                binding.btnCancel.setEnabled(false);

                // Vizuelno označavanje da je završen
                binding.btnComplete.setAlpha(0.3f);
                binding.btnCancel.setAlpha(0.3f);

            } else if (task.isUndone()) {
                // Undone zadaci - sivkasta boja i disabled dugmad
                binding.tvTaskName.setTextColor(itemView.getContext().getResources().getColor(R.color.colorTextSecondary));
                binding.tvExecutionTime.setTextColor(itemView.getContext().getResources().getColor(R.color.colorTextSecondary));
                binding.tvRecurrenceXp.setTextColor(itemView.getContext().getResources().getColor(R.color.colorTextSecondary));

                // Disable dugmad za undone zadatke
                binding.btnComplete.setEnabled(false);
                binding.btnCancel.setEnabled(false);

                // Vizuelno označavanje da je undone
                binding.btnComplete.setAlpha(0.3f);
                binding.btnCancel.setAlpha(0.3f);

            } else if (task.isFinished()) {
                // Canceled/Missed zadaci - sivkasta boja i disabled dugmad
                binding.tvTaskName.setTextColor(itemView.getContext().getResources().getColor(R.color.colorTextSecondary));
                binding.tvExecutionTime.setTextColor(itemView.getContext().getResources().getColor(R.color.colorTextSecondary));
                binding.tvRecurrenceXp.setTextColor(itemView.getContext().getResources().getColor(R.color.colorTextSecondary));

                // Disable dugmad za canceled/missed zadatke
                binding.btnComplete.setEnabled(false);
                binding.btnCancel.setEnabled(false);

                // Vizuelno označavanje da je canceled/missed
                binding.btnComplete.setAlpha(0.3f);
                binding.btnCancel.setAlpha(0.3f);

            } else {
                // Aktivni zadaci - normalne boje i enabled dugmad
                binding.tvTaskName.setTextColor(itemView.getContext().getResources().getColor(R.color.colorTextPrimary));
                binding.tvExecutionTime.setTextColor(itemView.getContext().getResources().getColor(R.color.colorTextSecondary));
                binding.tvRecurrenceXp.setTextColor(itemView.getContext().getResources().getColor(R.color.colorTextSecondary));

                // Enable dugmad za aktivne zadatke
                binding.btnComplete.setEnabled(true);
                binding.btnCancel.setEnabled(true);

                // Normalna vidljivost
                binding.btnComplete.setAlpha(1.0f);
                binding.btnCancel.setAlpha(1.0f);
            }

            // Hide pause/resume buttons by default
            binding.btnPause.setVisibility(View.GONE);
            binding.btnResume.setVisibility(View.GONE);

            // Show pause/resume buttons for recurring tasks
            if (task.isRecurring && !task.isFinished()) {
                if (task.isPaused()) {
                    // Show resume button for paused recurring tasks
                    binding.btnResume.setVisibility(View.VISIBLE);
                    binding.btnResume.setEnabled(true);
                    binding.btnResume.setAlpha(1.0f);
                } else {
                    // Show pause button for active recurring tasks
                    binding.btnPause.setVisibility(View.VISIBLE);
                    binding.btnPause.setEnabled(true);
                    binding.btnPause.setAlpha(1.0f);
                }
            }

            // Listeneri
            binding.btnComplete.setOnClickListener(v -> listener.onTaskComplete(task.id));
            binding.btnCancel.setOnClickListener(v -> listener.onTaskCancel(task.id));
            binding.btnPause.setOnClickListener(v -> listener.onTaskPause(task.id));
            binding.btnResume.setOnClickListener(v -> listener.onTaskResume(task.id));
            binding.getRoot().setOnClickListener(v -> listener.onTaskClick(task.id));
        }
    }
}