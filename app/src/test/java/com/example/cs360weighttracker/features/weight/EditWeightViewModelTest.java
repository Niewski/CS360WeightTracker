package com.example.cs360weighttracker.features.weight;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.cs360weighttracker.data.WeightRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EditWeightViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private EditWeightViewModel viewModel;
    private FakeWeightRepository fakeRepo;
    private static final int TEST_WEIGHT_ID = 42;

    @Before
    public void setUp() {
        viewModel = new EditWeightViewModel();
        fakeRepo = new FakeWeightRepository();
        viewModel.init(fakeRepo, TEST_WEIGHT_ID);
    }

    // --- updateWeight: happy path ---

    @Test
    public void updateWeight_validInput_returnsTrue() {
        fakeRepo.updateWeightReturn = true;

        viewModel.updateWeight("2026-01-15", "165.5");

        assertTrue(viewModel.getUpdateResult().getValue());
    }

    @Test
    public void updateWeight_validInput_delegatesToRepository() {
        fakeRepo.updateWeightReturn = true;

        viewModel.updateWeight("2026-01-15", "165.5");

        assertTrue(fakeRepo.updateWeightCalled);
    }

    // --- updateWeight: failure cases ---

    @Test
    public void updateWeight_emptyDate_returnsFalse() {
        viewModel.updateWeight("", "165.5");

        assertFalse(viewModel.getUpdateResult().getValue());
    }

    @Test
    public void updateWeight_emptyWeight_returnsFalse() {
        viewModel.updateWeight("2026-01-15", "");

        assertFalse(viewModel.getUpdateResult().getValue());
    }

    @Test
    public void updateWeight_bothEmpty_returnsFalse() {
        viewModel.updateWeight("", "");

        assertFalse(viewModel.getUpdateResult().getValue());
    }

    @Test
    public void updateWeight_invalidNumber_returnsFalse() {
        viewModel.updateWeight("2026-01-15", "abc");

        assertFalse(viewModel.getUpdateResult().getValue());
    }

    @Test
    public void updateWeight_repositoryFails_returnsFalse() {
        fakeRepo.updateWeightReturn = false;

        viewModel.updateWeight("2026-01-15", "165.5");

        assertFalse(viewModel.getUpdateResult().getValue());
    }

    // --- edge cases ---

    @Test
    public void updateWeight_zeroWeight_returnsTrue() {
        fakeRepo.updateWeightReturn = true;

        viewModel.updateWeight("2026-01-15", "0.0");

        assertTrue(viewModel.getUpdateResult().getValue());
    }

    @Test
    public void updateWeight_negativeWeight_returnsTrue() {
        // ViewModel doesn't validate negative values — repository decides
        fakeRepo.updateWeightReturn = true;

        viewModel.updateWeight("2026-01-15", "-5.0");

        assertTrue(viewModel.getUpdateResult().getValue());
    }

    @Test
    public void getUpdateResult_beforeAnyUpdate_isNull() {
        assertNull(viewModel.getUpdateResult().getValue());
    }

    @Test
    public void init_calledTwice_doesNotReinit() {
        FakeWeightRepository secondRepo = new FakeWeightRepository();
        secondRepo.updateWeightReturn = false;

        viewModel.init(secondRepo, 99);

        // Should still use the first repo (updateWeightReturn = true by default)
        fakeRepo.updateWeightReturn = true;
        viewModel.updateWeight("2026-01-15", "165.5");
        assertTrue(viewModel.getUpdateResult().getValue());
    }

    // --- Fake repository ---

    private static class FakeWeightRepository extends WeightRepository {
        boolean updateWeightReturn = true;
        boolean updateWeightCalled = false;

        FakeWeightRepository() {
            super(null);
        }

        @Override
        public boolean updateWeight(int weightId, String date, double weight) {
            updateWeightCalled = true;
            return updateWeightReturn;
        }
    }
}
