package com.zybooks.eventplanning_danpham;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * EventAdapter displays maintenance tasks as cards in a RecyclerView.
 * Handles creating and binding each task card.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.TaskViewHolder> {
    private List<MaintenanceTask> taskList;
    private final OnEventClickListener listener;

    /**
     * Interface for handling clicks on task cards
     */
    public interface OnEventClickListener {
        void onEventClick(MaintenanceTask task);
        void onEditClick(MaintenanceTask task);
        void onDeleteClick(MaintenanceTask task);
    }

    /**
     * @param taskList List of tasks to display
     * @param listener Click listener for card interactions
     */
    public EventAdapter(List<MaintenanceTask> taskList, OnEventClickListener listener) {
        this.taskList = taskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item_event layout for each task card
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        holder.bind(taskList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    /**
     * Replace the task list and refresh the RecyclerView
     * @param newTaskList Updated list of tasks
     */
    public void updateEvents(List<MaintenanceTask> newTaskList) {
        this.taskList = newTaskList;
        notifyDataSetChanged();
    }

    /**
     * Holds references to all views in one task card
     */
    static class TaskViewHolder extends RecyclerView.ViewHolder {

        private final TextView textViewTaskName;
        private final TextView textViewTailNumber;
        private final TextView textViewTypeStatus;
        private final TextView textViewDueDateTime;
        private final Button buttonEdit;
        private final Button buttonDelete;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTaskName = itemView.findViewById(R.id.textViewTaskName);
            textViewTailNumber = itemView.findViewById(R.id.textViewTailNumber);
            textViewTypeStatus = itemView.findViewById(R.id.textViewTypeStatus);
            textViewDueDateTime = itemView.findViewById(R.id.textViewDueDateTime);
            buttonEdit = itemView.findViewById(R.id.buttonEdit);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }

        /**
         * Show a task's details and set up its click listeners
         * @param task Task to display
         * @param listener Click listener for interactions
         */
        public void bind(MaintenanceTask task, OnEventClickListener listener) {
            textViewTaskName.setText(task.getTaskName());
            textViewTailNumber.setText("Tail: " + task.getTailNumber());
            textViewTypeStatus.setText(task.getTaskType() + " | " + task.getStatus());
            textViewDueDateTime.setText("Due " + task.getDueDate() + " at " + task.getDueTime());

            // Card, edit, and delete clicks
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onEventClick(task);
            });
            buttonEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEditClick(task);
            });
            buttonDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(task);
            });
        }
    }
}