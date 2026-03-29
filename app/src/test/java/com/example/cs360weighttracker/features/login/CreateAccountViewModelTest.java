package com.example.cs360weighttracker.features.login;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.cs360weighttracker.data.UserRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CreateAccountViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private CreateAccountViewModel viewModel;
    private FakeUserRepository fakeUserRepo;

    @Before
    public void setUp() {
        viewModel = new CreateAccountViewModel();
        fakeUserRepo = new FakeUserRepository();
        viewModel.init(fakeUserRepo);
    }

    // --- createAccount: happy path ---

    @Test
    public void createAccount_validInput_returnsTrue() {
        fakeUserRepo.createUserReturn = true;

        viewModel.createAccount("testuser", "testpass", "150.0", "");

        assertTrue(viewModel.getCreateResult().getValue());
    }

    @Test
    public void createAccount_validInputWithPhone_returnsTrue() {
        fakeUserRepo.createUserReturn = true;

        viewModel.createAccount("testuser", "testpass", "150.0", "5551234567");

        assertTrue(viewModel.getCreateResult().getValue());
    }

    // --- createAccount: failure cases ---

    @Test
    public void createAccount_emptyUsername_returnsFalse() {
        viewModel.createAccount("", "testpass", "150.0", "");

        assertFalse(viewModel.getCreateResult().getValue());
    }

    @Test
    public void createAccount_emptyPassword_returnsFalse() {
        viewModel.createAccount("testuser", "", "150.0", "");

        assertFalse(viewModel.getCreateResult().getValue());
    }

    @Test
    public void createAccount_emptyGoalWeight_returnsFalse() {
        viewModel.createAccount("testuser", "testpass", "", "");

        assertFalse(viewModel.getCreateResult().getValue());
    }

    @Test
    public void createAccount_allFieldsEmpty_returnsFalse() {
        viewModel.createAccount("", "", "", "");

        assertFalse(viewModel.getCreateResult().getValue());
    }

    @Test
    public void createAccount_invalidGoalWeight_returnsFalse() {
        viewModel.createAccount("testuser", "testpass", "abc", "");

        assertFalse(viewModel.getCreateResult().getValue());
    }

    @Test
    public void createAccount_duplicateUsername_returnsFalse() {
        fakeUserRepo.createUserReturn = false;

        viewModel.createAccount("testuser", "testpass", "150.0", "");

        assertFalse(viewModel.getCreateResult().getValue());
    }

    // --- createAccount: edge cases ---

    @Test
    public void createAccount_zeroGoalWeight_returnsTrue() {
        fakeUserRepo.createUserReturn = true;

        viewModel.createAccount("testuser", "testpass", "0.0", "");

        assertTrue(viewModel.getCreateResult().getValue());
    }

    @Test
    public void createAccount_negativeGoalWeight_returnsTrue() {
        fakeUserRepo.createUserReturn = true;

        viewModel.createAccount("testuser", "testpass", "-5.0", "");

        assertTrue(viewModel.getCreateResult().getValue());
    }

    @Test
    public void createAccount_veryLargeGoalWeight_returnsTrue() {
        fakeUserRepo.createUserReturn = true;

        viewModel.createAccount("testuser", "testpass", "9999.9", "");

        assertTrue(viewModel.getCreateResult().getValue());
    }

    // --- init: idempotent ---

    @Test
    public void init_calledTwice_doesNotReplaceRepository() {
        FakeUserRepository secondRepo = new FakeUserRepository();
        secondRepo.createUserReturn = false;

        viewModel.init(secondRepo);

        fakeUserRepo.createUserReturn = true;
        viewModel.createAccount("testuser", "testpass", "150.0", "");

        assertTrue(viewModel.getCreateResult().getValue());
    }

    // --- getCreateResult: initial state ---

    @Test
    public void getCreateResult_beforeCreate_returnsNull() {
        assertNull(viewModel.getCreateResult().getValue());
    }

    // --- Fake repository ---

    private static class FakeUserRepository extends UserRepository {
        boolean createUserReturn = true;

        FakeUserRepository() {
            super(null);
        }

        @Override
        public boolean createUser(String username, String password, double goalWeight, String phoneNumber) {
            return createUserReturn;
        }
    }
}
