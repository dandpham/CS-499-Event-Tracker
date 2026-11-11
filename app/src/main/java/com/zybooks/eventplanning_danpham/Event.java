package com.zybooks.eventplanning_danpham;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Event model class representing an event in the database.
 * Implements Parcelable to allow passing Event objects between activities.
 */
public class Event implements Parcelable {
    private int eventId;
    private int userId;
    private String eventName;
    private String eventDate;
    private String eventTime;
    private String location;
    private String description;

    // Constructor for creating new events
    public Event(int userId, String eventName, String eventDate, String eventTime,
                 String location, String description) {
        this.userId = userId;
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.eventTime = eventTime;
        this.location = location;
        this.description = description;
    }

    // Constructor for existing events with ID
    public Event(int eventId, int userId, String eventName, String eventDate,
                 String eventTime, String location, String description) {
        this.eventId = eventId;
        this.userId = userId;
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.eventTime = eventTime;
        this.location = location;
        this.description = description;
    }

    // Parcelable implementation for passing objects between activities
    protected Event(Parcel in) {
        eventId = in.readInt();
        userId = in.readInt();
        eventName = in.readString();
        eventDate = in.readString();
        eventTime = in.readString();
        location = in.readString();
        description = in.readString();
    }

    public static final Creator<Event> CREATOR = new Creator<Event>() {
        @Override
        public Event createFromParcel(Parcel in) {
            return new Event(in);
        }

        @Override
        public Event[] newArray(int size) {
            return new Event[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(eventId);
        dest.writeInt(userId);
        dest.writeString(eventName);
        dest.writeString(eventDate);
        dest.writeString(eventTime);
        dest.writeString(location);
        dest.writeString(description);
    }

    // Getters and setters
    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getEventDate() {
        return eventDate;
    }

    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }

    public String getEventTime() {
        return eventTime;
    }

    public void setEventTime(String eventTime) {
        this.eventTime = eventTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
