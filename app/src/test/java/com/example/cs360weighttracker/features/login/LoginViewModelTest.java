package com.example.cs360weighttracker.features.login;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.cs360weighttracker.data.UserRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LoginViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private LoginViewModel viewModel;
    private FakeUserRepository fakeUserRepo;
    private static final int TEST_USER_ID = 42;

    @Before
    public void setUp() {
        viewModel = new LoginViewModel();
        fakeUserRepo = new FakeUserRepository();
        viewModel.init(fakeUserRepo);
    }

    // --- login: happy path ---

    @Test
    public void login_validCredentials_returnsUserId() {
        fakeUserRepo.loginReturn = TEST_USER_ID;

        viewModel.login("testuser", "testpass");

        assertEquals(TEST_USER_ID, (int) viewModel.getLoginResult().getValue());
    }

    // --- login: failure cases ---

    @Test
    public void login_emptyUsername_returnsNegativeOne() {
        viewModel.login("", "testpass");

        assertEquals(-1, (int) viewModel.getLoginResult().getValue());
    }

    @Test
    public void login_emptyPassword_returnsNegativeOne() {
        viewModel.login("testuser", "");

        assertEquals(-1, (int) viewModel.getLoginResult().getValue());
    }

    @Test
    public void login_bothEmpty_returnsNegativeOne() {
        viewModel.login("", "");

        assertEquals(-1, (int) viewModel.getLoginResult().getValue());
    }

    @Test
    public void login_wrongCredentials_returnsNegativeOne() {
        fakeUserRepo.loginReturn = -1;

        viewModel.login("testuser", "wrongpass");

        assertEquals(-1, (int) viewModel.getLoginResult().getValue());
    }

    // --- login: edge cases ---

    @Test
    public void login_emptyUsername_doesNotCallRepository() {
        fakeUserRepo.loginReturn = TEST_USER_ID;

        viewModel.login("", "testpass");

        // Should return -1 from validation, not the repo's value
        assertEquals(-1, (int) viewModel.getLoginResult().getValue());
    }

    @Test
    public void login_calledTwice_updatesResult() {
        fakeUserRepo.loginReturn = TEST_USER_ID;
        viewModel.login("testuser", "testpass");
        assertEquals(TEST_USER_ID, (int) viewModel.getLoginResult().getValue());

        fakeUserRepo.loginReturn = -1;
        viewModel.login("testuser", "wrongpass");
        assertEquals(-1, (int) viewModel.getLoginResult().getValue());
    }

    // --- init: idempotent ---

    @Test
    public void init_calledTwice_doesNotReplaceRepository() {
        FakeUserRepository secondRepo = new FakeUserRepository();
        secondRepo.loginReturn = -1;

        viewModel.init(secondRepo);

        fakeUserRepo.loginReturn = TEST_USER_ID;
        viewModel.login("testuser", "testpass");

        // Should use the first repo (returns TEST_USER_ID), not the second
        assertEquals(TEST_USER_ID, (int) viewModel.getLoginResult().getValue());
    }

    // --- getLoginResult: initial state ---

    @Test
    public void getLoginResult_beforeLogin_returnsNull() {
        assertNull(viewModel.getLoginResult().getValue());
    }

    // --- Fake repository ---

    private static class FakeUserRepository extends UserRepository {
        int loginReturn = -1;

        FakeUserRepository() {
            super(null);
        }

        @Override
        public int loginUser(String username, String password) {
            return loginReturn;
        }
    }
}
