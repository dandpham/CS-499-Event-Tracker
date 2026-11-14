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
 * Overdue tasks are shown in red, and completed tasks are faded.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.TaskViewHolder> {

    private static final int OVERDUE_COLOR = 0xFFD32F2F; // Red
    private static final float COMPLETED_ALPHA = 0.6f;   // Faded
    private static final float NORMAL_ALPHA = 1.0f;

    private List<TaskListItem> items;
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
     * @param items Tasks to display, with their overdue flags
     * @param listener Click listener for card interactions
     */
    public EventAdapter(List<TaskListItem> items, OnEventClickListener listener) {
        this.items = items;
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
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Replace the list and refresh the RecyclerView
     * @param newItems Updated tasks with their overdue flags
     */
    public void updateItems(List<TaskListItem> newItems) {
        this.items = newItems;
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
        private final int normalDueColor; // The theme's text color, saved so it can be restored

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTaskName = itemView.findViewById(R.id.textViewTaskName);
            textViewTailNumber = itemView.findViewById(R.id.textViewTailNumber);
            textViewTypeStatus = itemView.findViewById(R.id.textViewTypeStatus);
            textViewDueDateTime = itemView.findViewById(R.id.textViewDueDateTime);
            buttonEdit = itemView.findViewById(R.id.buttonEdit);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
            normalDueColor = textViewDueDateTime.getCurrentTextColor();
        }

        /**
         * Show a task's details and set up its click listeners
         * @param item Task to display, with its overdue flag
         * @param listener Click listener for interactions
         */
        public void bind(TaskListItem item, OnEventClickListener listener) {
            MaintenanceTask task = item.getTask();

            textViewTaskName.setText(task.getTaskName());
            textViewTailNumber.setText("Tail: " + task.getTailNumber());
            textViewTypeStatus.setText(task.getTaskType() + " | " + task.getStatus());

            String due = "Due " + task.getDueDate() + " at " + task.getDueTime();

            // Overdue tasks stand out in red. Cards are reused while scrolling,
            // so the color is always set, never just changed for overdue ones.
            if (item.isOverdue()) {
                textViewDueDateTime.setText("OVERDUE: " + due);
                textViewDueDateTime.setTextColor(OVERDUE_COLOR);
            } else {
                textViewDueDateTime.setText(due);
                textViewDueDateTime.setTextColor(normalDueColor);
            }

            // Completed tasks are faded so open work stands out
            itemView.setAlpha(task.isComplete() ? COMPLETED_ALPHA : NORMAL_ALPHA);

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