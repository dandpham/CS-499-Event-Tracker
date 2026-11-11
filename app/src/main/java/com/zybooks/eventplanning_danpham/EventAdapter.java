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
 * EventAdapter manages the display of events in a RecyclerView.
 * Handles the creation and binding of event card views.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    private List<Event> eventList;
    private OnEventClickListener listener;

    /**
     * Interface for handling click events on event items
     */
    public interface OnEventClickListener {
        void onEventClick(Event event);
        void onEditClick(Event event);
        void onDeleteClick(Event event);
    }

    /**
     * Constructor for EventAdapter
     * @param eventList List of events to display
     * @param listener Click listener for event interactions
     */
    public EventAdapter(List<Event> eventList, OnEventClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item_event layout for each event card
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        // Get event at current position
        Event event = eventList.get(position);

        // Bind event data to views
        holder.bind(event, listener);
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    /**
     * Update the event list and refresh the RecyclerView
     * @param newEventList Updated list of events
     */
    public void updateEvents(List<Event> newEventList) {
        this.eventList = newEventList;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder class for event items
     * Holds references to all views in an event card
     */
    static class EventViewHolder extends RecyclerView.ViewHolder {

        private TextView textViewEventName;
        private TextView textViewEventDate;
        private TextView textViewEventLocation;
        private TextView textViewEventTime;
        private Button buttonEdit;
        private Button buttonDelete;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize views from item_event layout
            textViewEventName = itemView.findViewById(R.id.textViewEventName);
            textViewEventDate = itemView.findViewById(R.id.textViewEventDate);
            textViewEventLocation = itemView.findViewById(R.id.textViewEventLocation);
            textViewEventTime = itemView.findViewById(R.id.textViewEventTime);
            buttonEdit = itemView.findViewById(R.id.buttonEdit);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }

        /**
         * Bind event data to views and set up click listeners
         * @param event Event object to display
         * @param listener Click listener for interactions
         */
        public void bind(Event event, OnEventClickListener listener) {
            // Set event information
            textViewEventName.setText(event.getEventName());
            textViewEventDate.setText(event.getEventDate());
            textViewEventLocation.setText(event.getLocation() != null && !event.getLocation().isEmpty()
                    ? event.getLocation()
                    : "No location");
            textViewEventTime.setText(event.getEventTime());

            // Set click listener for entire card
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onEventClick(event);
                    }
                }
            });

            // Set click listener for edit button
            buttonEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onEditClick(event);
                    }
                }
            });

            // Set click listener for delete button
            buttonDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onDeleteClick(event);
                    }
                }
            });
        }
    }
}
