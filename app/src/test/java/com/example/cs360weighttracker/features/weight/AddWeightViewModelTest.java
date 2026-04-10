package com.example.cs360weighttracker.features.weight;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.cs360weighttracker.data.UserRepository;
import com.example.cs360weighttracker.data.WeightRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AddWeightViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AddWeightViewModel viewModel;
    private FakeWeightRepository fakeWeightRepo;
    private FakeUserRepository fakeUserRepo;
    private static final int TEST_USER_ID = 1;

    @Before
    public void setUp() {
        viewModel = new AddWeightViewModel();
        fakeWeightRepo = new FakeWeightRepository();
        fakeUserRepo = new FakeUserRepository();
        viewModel.init(fakeWeightRepo, fakeUserRepo, TEST_USER_ID);
    }

    // --- saveWeight: happy path ---

    @Test
    public void saveWeight_validInput_returnsTrue() {
        fakeWeightRepo.addWeightReturn = true;
        fakeUserRepo.goalWeight = 150.0;
        fakeUserRepo.goalSmsSent = false;

        viewModel.saveWeight("2026-01-15", "165.5", "feeling good");

        assertTrue(viewModel.getSaveResult().getValue());
    }

    // --- saveWeight: failure cases ---

    @Test
    public void saveWeight_emptyDate_returnsFalse() {
        viewModel.saveWeight("", "165.5", "");

        assertFalse(viewModel.getSaveResult().getValue());
    }

    @Test
    public void saveWeight_emptyWeight_returnsFalse() {
        viewModel.saveWeight("2026-01-15", "", "");

        assertFalse(viewModel.getSaveResult().getValue());
    }

    @Test
    public void saveWeight_bothEmpty_returnsFalse() {
        viewModel.saveWeight("", "", "");

        assertFalse(viewModel.getSaveResult().getValue());
    }

    @Test
    public void saveWeight_invalidNumber_returnsFalse() {
        viewModel.saveWeight("2026-01-15", "abc", "");

        assertFalse(viewModel.getSaveResult().getValue());
    }

    @Test
    public void saveWeight_insertFails_returnsFalse() {
        fakeWeightRepo.addWeightReturn = false;

        viewModel.saveWeight("2026-01-15", "165.5", "");

        assertFalse(viewModel.getSaveResult().getValue());
    }

    // --- saveWeight: goal-reached event ---

    @Test
    public void saveWeight_weightBelowGoal_emitsGoalEvent() {
        fakeWeightRepo.addWeightReturn = true;
        fakeUserRepo.goalWeight = 170.0;
        fakeUserRepo.goalSmsSent = false;
        fakeUserRepo.phoneNumber = "5551234567";

        viewModel.saveWeight("2026-01-15", "165.5", "");

        AddWeightViewModel.GoalReachedEvent event = viewModel.getGoalReachedEvent().getValue();
        assertNotNull(event);
        assertEquals("5551234567", event.phoneNumber);
    }

    @Test
    public void saveWeight_weightEqualsGoal_emitsGoalEvent() {
        fakeWeightRepo.addWeightReturn = true;
        fakeUserRepo.goalWeight = 165.5;
        fakeUserRepo.goalSmsSent = false;
        fakeUserRepo.phoneNumber = "5551234567";

        viewModel.saveWeight("2026-01-15", "165.5", "");

        AddWeightViewModel.GoalReachedEvent event = viewModel.getGoalReachedEvent().getValue();
        assertNotNull(event);
        assertEquals("5551234567", event.phoneNumber);
    }

    @Test
    public void saveWeight_goalReachedSmsSent_noEvent() {
        fakeWeightRepo.addWeightReturn = true;
        fakeUserRepo.goalWeight = 170.0;
        fakeUserRepo.goalSmsSent = true;
        fakeUserRepo.phoneNumber = "5551234567";

        viewModel.saveWeight("2026-01-15", "165.5", "");

        assertNull(viewModel.getGoalReachedEvent().getValue());
    }

    @Test
    public void saveWeight_aboveGoal_noEvent() {
        fakeWeightRepo.addWeightReturn = true;
        fakeUserRepo.goalWeight = 150.0;
        fakeUserRepo.goalSmsSent = false;

        viewModel.saveWeight("2026-01-15", "165.5", "");

        assertNull(viewModel.getGoalReachedEvent().getValue());
    }

    @Test
    public void saveWeight_goalReachedNullPhone_emitsEventWithNull() {
        fakeWeightRepo.addWeightReturn = true;
        fakeUserRepo.goalWeight = 170.0;
        fakeUserRepo.goalSmsSent = false;
        fakeUserRepo.phoneNumber = null;

        viewModel.saveWeight("2026-01-15", "165.5", "");

        AddWeightViewModel.GoalReachedEvent event = viewModel.getGoalReachedEvent().getValue();
        assertNotNull(event);
        assertNull(event.phoneNumber);
    }

    // --- saveWeight: edge cases ---

    @Test
    public void saveWeight_zeroWeight_returnsTrue() {
        fakeWeightRepo.addWeightReturn = true;
        fakeUserRepo.goalWeight = 150.0;

        viewModel.saveWeight("2026-01-15", "0.0", "");

        assertTrue(viewModel.getSaveResult().getValue());
    }

    @Test
    public void saveWeight_negativeWeight_returnsTrue() {
        fakeWeightRepo.addWeightReturn = true;
        fakeUserRepo.goalWeight = 150.0;

        viewModel.saveWeight("2026-01-15", "-5.0", "");

        assertTrue(viewModel.getSaveResult().getValue());
    }

    // --- init ---

    @Test
    public void init_calledTwice_ignoresSecondCall() {
        FakeWeightRepository secondRepo = new FakeWeightRepository();
        secondRepo.addWeightReturn = false;
        FakeUserRepository secondUserRepo = new FakeUserRepository();

        viewModel.init(secondRepo, secondUserRepo, 99);

        // First repo should still be used; set it to succeed
        fakeWeightRepo.addWeightReturn = true;
        fakeUserRepo.goalWeight = 150.0;
        viewModel.saveWeight("2026-01-15", "165.5", "");

        assertTrue(viewModel.getSaveResult().getValue());
    }

    // --- markGoalSmsSent ---

    @Test
    public void markGoalSmsSent_delegatesToRepository() {
        viewModel.markGoalSmsSent();

        assertTrue(fakeUserRepo.goalSmsSentCalled);
    }

    // --- Fake repositories ---

    private static class FakeWeightRepository extends WeightRepository {
        boolean addWeightReturn = true;

        FakeWeightRepository() {
            super(null);
        }

        @Override
        public boolean addWeight(int userId, String date, double weight, String notes) {
            return addWeightReturn;
        }
    }

    private static class FakeUserRepository extends UserRepository {
        double goalWeight = 150.0;
        boolean goalSmsSent = false;
        String phoneNumber = null;
        boolean goalSmsSentCalled = false;

        FakeUserRepository() {
            super(null);
        }

        @Override
        public double getGoalWeight(int userId) {
            return goalWeight;
        }

        @Override
        public boolean isGoalSmsSent(int userId) {
            return goalSmsSent;
        }

        @Override
        public String getPhoneNumber(int userId) {
            return phoneNumber;
        }

        @Override
        public void setGoalSmsSent(int userId) {
            goalSmsSentCalled = true;
        }
    }
}
