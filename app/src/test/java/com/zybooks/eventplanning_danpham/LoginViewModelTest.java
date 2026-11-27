package com.zybooks.eventplanning_danpham;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Unit tests for LoginViewModel, using a fake user repository
 */
public class LoginViewModelTest {

    // Makes LiveData update instantly on the test thread
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private FakeUserRepository repository;
    private LoginViewModel viewModel;

    @Before
    public void setUp() {
        repository = new FakeUserRepository();
        repository.passwords.put("dan", "secret123");
        viewModel = new LoginViewModel(repository);
    }

    @Test
    public void login_correctPassword_setsUser() {
        viewModel.login("dan", "secret123");

        assertEquals("dan", viewModel.getLoggedInUser().getValue().getUsername());
        assertNull(viewModel.getErrorMessage().getValue());
        assertFalse(viewModel.isLoading().getValue());
    }

    @Test
    public void login_wrongPassword_showsSameMessageAsUnknownUser() {
        viewModel.login("dan", "wrongpass");
        String wrongPassword = viewModel.getErrorMessage().getValue();

        viewModel.login("nobody", "secret123");
        String unknownUser = viewModel.getErrorMessage().getValue();

        // The same message for both, so the screen doesn't reveal which usernames exist
        assertEquals(SqliteUserRepository.ERROR_INVALID_LOGIN, wrongPassword);
        assertEquals(wrongPassword, unknownUser);
        assertNull(viewModel.getLoggedInUser().getValue());
    }

    @Test
    public void login_shortPassword_showsValidationErrorWithoutCallingRepository() {
        repository.shouldFail = true; // Would show a different error if called

        viewModel.login("dan", "123");

        assertEquals("Password must be at least 6 characters", viewModel.getErrorMessage().getValue());
        assertFalse(viewModel.isLoading().getValue());
    }

    @Test
    public void login_unexpectedFailure_showsGeneralMessage() {
        repository.shouldFail = true;

        viewModel.login("dan", "secret123");

        // Internal details like "database is locked" never reach the screen
        assertEquals("Login failed. Please try again.", viewModel.getErrorMessage().getValue());
        assertFalse(viewModel.isLoading().getValue());
    }

    @Test
    public void register_newUsername_setsUser() {
        viewModel.register("newuser", "password1");

        assertEquals("newuser", viewModel.getLoggedInUser().getValue().getUsername());
    }

    @Test
    public void register_takenUsername_showsError() {
        viewModel.register("dan", "password1");

        assertEquals(SqliteUserRepository.ERROR_USERNAME_TAKEN, viewModel.getErrorMessage().getValue());
        assertNull(viewModel.getLoggedInUser().getValue());
    }

    @Test
    public void clearError_removesError() {
        viewModel.login("dan", "wrongpass");

        viewModel.clearError();

        assertNull(viewModel.getErrorMessage().getValue());
    }
}
