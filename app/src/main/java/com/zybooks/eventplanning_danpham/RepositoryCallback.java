package com.zybooks.eventplanning_danpham;

/**
 * Receives the result of a background database operation.
 * Exactly one of these methods is called, on the main thread.
 * Shared by TaskRepository and UserRepository.
 */
public interface RepositoryCallback<T> {
    void onSuccess(T result);

    void onError(Exception e);
}
