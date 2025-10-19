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

            // Listeneri
            binding.btnComplete.setOnClickListener(v -> listener.onTaskComplete(task.id));
            binding.getRoot().setOnClickListener(v -> listener.onTaskClick(task.id));
        }
    }
}