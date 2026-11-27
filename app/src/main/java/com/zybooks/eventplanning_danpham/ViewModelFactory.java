package com.zybooks.eventplanning_danpham;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.time.LocalDateTime;

/**
 * ViewModelFactory creates the app's ViewModels and gives each one
 * the real repositories and the real clock. Tests skip this class and
 * pass in fake repositories and a fixed time instead.
 */
public class ViewModelFactory implements ViewModelProvider.Factory {

    private final Context appContext;

    public ViewModelFactory(Context context) {
        appContext = context.getApplicationContext();
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(TaskListViewModel.class)) {
            return (T) new TaskListViewModel(new SqliteTaskRepository(appContext), LocalDateTime::now);
        }
        if (modelClass.isAssignableFrom(TaskViewModel.class)) {
            return (T) new TaskViewModel(new SqliteTaskRepository(appContext), LocalDateTime::now);
        }
        if (modelClass.isAssignableFrom(ReportsViewModel.class)) {
            return (T) new ReportsViewModel(new SqliteTaskRepository(appContext));
        }
        if (modelClass.isAssignableFrom(LoginViewModel.class)) {
            return (T) new LoginViewModel(new SqliteUserRepository(appContext));
        }
        throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
    }
}
