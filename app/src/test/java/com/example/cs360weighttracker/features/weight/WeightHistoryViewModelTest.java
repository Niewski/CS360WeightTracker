package com.example.cs360weighttracker.features.weight;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.cs360weighttracker.data.WeightEntry;
import com.example.cs360weighttracker.data.WeightRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WeightHistoryViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private WeightHistoryViewModel viewModel;
    private FakeWeightRepository fakeRepo;
    private static final int TEST_USER_ID = 1;

    @Before
    public void setUp() {
        viewModel = new WeightHistoryViewModel();
        fakeRepo = new FakeWeightRepository();
    }

    // --- init: happy path ---

    @Test
    public void init_validArgs_loadsWeights() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 165.5));

        viewModel.init(fakeRepo, TEST_USER_ID);

        List<WeightEntry> result = viewModel.getWeights().getValue();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("2026-01-15", result.get(0).date);
        assertEquals(165.5, result.get(0).weight, 0.01);
    }

    @Test
    public void init_multipleEntries_loadsAll() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 165.5));
        fakeRepo.weightEntries.add(new WeightEntry(2, "2026-01-16", 164.0));
        fakeRepo.weightEntries.add(new WeightEntry(3, "2026-01-17", 163.2));

        viewModel.init(fakeRepo, TEST_USER_ID);

        List<WeightEntry> result = viewModel.getWeights().getValue();
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void init_calledTwice_doesNotReinit() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 165.5));
        viewModel.init(fakeRepo, TEST_USER_ID);

        FakeWeightRepository secondRepo = new FakeWeightRepository();
        secondRepo.weightEntries.add(new WeightEntry(99, "2026-06-01", 200.0));
        viewModel.init(secondRepo, 99);

        // Should still have the first repo's data
        List<WeightEntry> result = viewModel.getWeights().getValue();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).id);
    }

    // --- init: edge cases ---

    @Test
    public void init_noEntries_returnsEmptyList() {
        viewModel.init(fakeRepo, TEST_USER_ID);

        List<WeightEntry> result = viewModel.getWeights().getValue();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // --- getWeights: before init ---

    @Test
    public void getWeights_beforeInit_returnsEmptyList() {
        List<WeightEntry> result = viewModel.getWeights().getValue();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // --- loadWeights ---

    @Test
    public void loadWeights_afterNewEntry_updatesLiveData() {
        viewModel.init(fakeRepo, TEST_USER_ID);
        assertEquals(0, viewModel.getWeights().getValue().size());

        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 165.5));
        viewModel.loadWeights();

        List<WeightEntry> result = viewModel.getWeights().getValue();
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void loadWeights_beforeInit_doesNotCrash() {
        // No init called — loadWeights should be a no-op
        viewModel.loadWeights();

        List<WeightEntry> result = viewModel.getWeights().getValue();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // --- deleteWeight: happy path ---

    @Test
    public void deleteWeight_existingEntry_returnsTrue() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 165.5));
        fakeRepo.deleteWeightReturn = true;
        viewModel.init(fakeRepo, TEST_USER_ID);

        boolean result = viewModel.deleteWeight(1);

        assertTrue(result);
    }

    @Test
    public void deleteWeight_existingEntry_refreshesList() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 165.5));
        fakeRepo.deleteWeightReturn = true;
        viewModel.init(fakeRepo, TEST_USER_ID);
        assertEquals(1, viewModel.getWeights().getValue().size());

        // Simulate removal from backing store
        fakeRepo.weightEntries.clear();
        viewModel.deleteWeight(1);

        List<WeightEntry> result = viewModel.getWeights().getValue();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // --- deleteWeight: failure cases ---

    @Test
    public void deleteWeight_repositoryFails_returnsFalse() {
        fakeRepo.deleteWeightReturn = false;
        viewModel.init(fakeRepo, TEST_USER_ID);

        boolean result = viewModel.deleteWeight(999);

        assertFalse(result);
    }

    @Test
    public void deleteWeight_repositoryFails_doesNotRefreshList() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 165.5));
        fakeRepo.deleteWeightReturn = false;
        viewModel.init(fakeRepo, TEST_USER_ID);

        viewModel.deleteWeight(1);

        // List should remain unchanged since delete failed
        List<WeightEntry> result = viewModel.getWeights().getValue();
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // --- Fake repository ---

    private static class FakeWeightRepository extends WeightRepository {
        boolean deleteWeightReturn = true;
        final List<WeightEntry> weightEntries = new ArrayList<>();

        FakeWeightRepository() {
            super(null);
        }

        @Override
        public List<WeightEntry> getWeights(int userId) {
            return new ArrayList<>(weightEntries);
        }

        @Override
        public boolean deleteWeight(int weightId) {
            return deleteWeightReturn;
        }
    }
}
