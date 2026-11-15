package com.zybooks.eventplanning_danpham;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.time.LocalDateTime;

/**
 * ViewModelFactory creates the app's ViewModels and gives each one
 * the real repository and the real clock. Tests skip this class and
 * pass in a fake repository and a fixed time instead.
 */
public class ViewModelFactory implements ViewModelProvider.Factory {

    private final TaskRepository repository;

    public ViewModelFactory(Context context) {
        repository = new SqliteTaskRepository(context);
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(TaskListViewModel.class)) {
            return (T) new TaskListViewModel(repository, LocalDateTime::now);
        }
        if (modelClass.isAssignableFrom(TaskViewModel.class)) {
            return (T) new TaskViewModel(repository, LocalDateTime::now);
        }
        throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
    }
}