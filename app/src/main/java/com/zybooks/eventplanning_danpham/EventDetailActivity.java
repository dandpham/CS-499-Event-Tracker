package com.zybooks.eventplanning_danpham;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/**
 * EventDetailActivity displays complete information for a single event.
 * Provides options to edit, delete, or share the event.
 */
public class EventDetailActivity extends AppCompatActivity {

    // UI components
    private TextView textViewDetailTitle;
    private TextView textViewDetailDate;
    private TextView textViewDetailTime;
    private TextView textViewDetailLocation;
    private TextView textViewDetailDescription;
    private Button buttonEdit;
    private Button buttonDelete;
    private Button buttonShare;

    // Data
    private Event event;
    private DatabaseHelper databaseHelper;

    private static final int REQUEST_EDIT_EVENT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        // Initialize database helper
        databaseHelper = new DatabaseHelper(this);

        // Get event from intent
        event = getIntent().getParcelableExtra("EVENT");

        if (event == null) {
            Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI components
        initializeViews();

        // Display event data
        displayEventData();

        // Set up listeners
        setupListeners();
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews() {
        textViewDetailTitle = findViewById(R.id.textViewDetailTitle);
        textViewDetailDate = findViewById(R.id.textViewDetailDate);
        textViewDetailTime = findViewById(R.id.textViewDetailTime);
        textViewDetailLocation = findViewById(R.id.textViewDetailLocation);
        textViewDetailDescription = findViewById(R.id.textViewDetailDescription);
        buttonEdit = findViewById(R.id.buttonEdit);
        buttonDelete = findViewById(R.id.buttonDelete);
        buttonShare = findViewById(R.id.buttonShare);
    }

    /**
     * Display event information in UI
     */
    private void displayEventData() {
        textViewDetailTitle.setText(event.getEventName());
        textViewDetailDate.setText(event.getEventDate());
        textViewDetailTime.setText(event.getEventTime());

        // Handle optional location
        if (event.getLocation() != null && !event.getLocation().isEmpty()) {
            textViewDetailLocation.setText(event.getLocation());
        } else {
            textViewDetailLocation.setText("No location specified");
        }

        // Handle optional description
        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            textViewDetailDescription.setText(event.getDescription());
        } else {
            textViewDetailDescription.setText("No description provided");
        }
    }

    /**
     * Set up button click listeners
     */
    private void setupListeners() {
        // Edit button - navigate to edit screen
        buttonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EventDetailActivity.this, AddEditEventActivity.class);
                intent.putExtra("EVENT_ID", event.getEventId());
                intent.putExtra("USER_ID", event.getUserId());
                startActivityForResult(intent, REQUEST_EDIT_EVENT);
            }
        });

        // Delete button - show confirmation dialog
        buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteConfirmation();
            }
        });

        // Share button - create share intent
        buttonShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareEvent();
            }
        });
    }

    /**
     * Show confirmation dialog before deleting event
     */
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteEvent();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Delete event from database
     */
    private void deleteEvent() {
        int result = databaseHelper.deleteEvent(event.getEventId());

        if (result > 0) {
            Toast.makeText(this, "Event deleted successfully", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Failed to delete event", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Share event details using Android share intent
     */
    private void shareEvent() {
        // Create shareable text with event details
        String shareText = "Event: " + event.getEventName() + "\n" +
                "Date: " + event.getEventDate() + "\n" +
                "Time: " + event.getEventTime() + "\n";

        if (event.getLocation() != null && !event.getLocation().isEmpty()) {
            shareText += "Location: " + event.getLocation() + "\n";
        }

        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            shareText += "Description: " + event.getDescription();
        }

        // Create share intent
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Event: " + event.getEventName());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        // Show share chooser
        startActivity(Intent.createChooser(shareIntent, "Share event via"));
    }

    /**
     * Handle result from edit activity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_EDIT_EVENT && resultCode == RESULT_OK) {
            // Reload event data after edit
            event = databaseHelper.getEvent(event.getEventId());
            if (event != null) {
                displayEventData();
                setResult(RESULT_OK); // Pass result back to MainActivity
            } else {
                // Event was deleted in edit screen
                finish();
            }
        }
    }
}
