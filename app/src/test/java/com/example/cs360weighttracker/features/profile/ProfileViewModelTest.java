package com.example.cs360weighttracker.features.profile;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.cs360weighttracker.data.UserProfile;
import com.example.cs360weighttracker.data.UserRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ProfileViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private ProfileViewModel viewModel;
    private FakeUserRepository fakeUserRepo;
    private static final int TEST_USER_ID = 1;

    @Before
    public void setUp() {
        viewModel = new ProfileViewModel();
        fakeUserRepo = new FakeUserRepository();
        fakeUserRepo.userProfile = new UserProfile("testuser", 150.0, null);
        viewModel.init(fakeUserRepo, TEST_USER_ID);
    }

    // --- init / loadProfile: happy path ---

    @Test
    public void init_validUser_loadsProfile() {
        UserProfile profile = viewModel.getProfile().getValue();

        assertNotNull(profile);
        assertEquals("testuser", profile.username);
        assertEquals(150.0, profile.goalWeight, 0.01);
        assertNull(profile.phoneNumber);
    }

    @Test
    public void init_userWithPhone_loadsPhoneNumber() {
        ProfileViewModel vm = new ProfileViewModel();
        FakeUserRepository repo = new FakeUserRepository();
        repo.userProfile = new UserProfile("testuser", 150.0, "5551234567");
        vm.init(repo, TEST_USER_ID);

        UserProfile profile = vm.getProfile().getValue();

        assertNotNull(profile);
        assertEquals("5551234567", profile.phoneNumber);
    }

    // --- updateProfile: happy path ---

    @Test
    public void updateProfile_validGoalWeight_returnsTrue() {
        fakeUserRepo.updateReturn = true;

        viewModel.updateProfile("160.0", "");

        assertTrue(viewModel.getUpdateResult().getValue());
    }

    @Test
    public void updateProfile_validGoalWeightAndPhone_returnsTrue() {
        fakeUserRepo.updateReturn = true;

        viewModel.updateProfile("160.0", "5551234567");

        assertTrue(viewModel.getUpdateResult().getValue());
    }

    // --- updateProfile: failure cases ---

    @Test
    public void updateProfile_emptyGoalWeight_returnsFalse() {
        viewModel.updateProfile("", "5551234567");

        assertFalse(viewModel.getUpdateResult().getValue());
    }

    @Test
    public void updateProfile_invalidGoalWeight_returnsFalse() {
        viewModel.updateProfile("abc", "");

        assertFalse(viewModel.getUpdateResult().getValue());
    }

    @Test
    public void updateProfile_repoFails_returnsFalse() {
        fakeUserRepo.updateReturn = false;

        viewModel.updateProfile("160.0", "");

        assertFalse(viewModel.getUpdateResult().getValue());
    }

    // --- updateProfile: edge cases ---

    @Test
    public void updateProfile_zeroGoalWeight_returnsTrue() {
        fakeUserRepo.updateReturn = true;

        viewModel.updateProfile("0.0", "");

        assertTrue(viewModel.getUpdateResult().getValue());
    }

    @Test
    public void updateProfile_negativeGoalWeight_returnsTrue() {
        fakeUserRepo.updateReturn = true;

        viewModel.updateProfile("-5.0", "");

        assertTrue(viewModel.getUpdateResult().getValue());
    }

    @Test
    public void updateProfile_emptyPhone_passesEmptyString() {
        fakeUserRepo.updateReturn = true;

        viewModel.updateProfile("150.0", "");

        assertTrue(viewModel.getUpdateResult().getValue());
    }

    // --- loadProfile ---

    @Test
    public void loadProfile_refreshesProfileData() {
        // Change the fake data and reload
        fakeUserRepo.userProfile = new UserProfile("testuser", 200.0, "5559999999");

        viewModel.loadProfile();

        UserProfile profile = viewModel.getProfile().getValue();
        assertNotNull(profile);
        assertEquals(200.0, profile.goalWeight, 0.01);
        assertEquals("5559999999", profile.phoneNumber);
    }

    // --- init: idempotent ---

    @Test
    public void init_calledTwice_doesNotReplaceRepository() {
        FakeUserRepository secondRepo = new FakeUserRepository();
        secondRepo.updateReturn = false;

        viewModel.init(secondRepo, TEST_USER_ID);

        fakeUserRepo.updateReturn = true;
        viewModel.updateProfile("160.0", "");

        // Should use the first repo (returns true)
        assertTrue(viewModel.getUpdateResult().getValue());
    }

    // --- getUpdateResult: initial state ---

    @Test
    public void getUpdateResult_beforeUpdate_returnsNull() {
        assertNull(viewModel.getUpdateResult().getValue());
    }

    // --- Fake repository ---

    private static class FakeUserRepository extends UserRepository {
        UserProfile userProfile = null;
        boolean updateReturn = true;

        FakeUserRepository() {
            super(null);
        }

        @Override
        public UserProfile getUserProfile(int userId) {
            return userProfile;
        }

        @Override
        public boolean updateUser(int userId, double goalWeight, String phoneNumber) {
            return updateReturn;
        }
    }
}
