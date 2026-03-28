package com.example.cs360weighttracker.features.weight;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class GoalReachedEventTest {

    @Test
    public void constructor_validPhone_setsPhoneNumber() {
        AddWeightViewModel.GoalReachedEvent event =
                new AddWeightViewModel.GoalReachedEvent("5551234567");
        assertEquals("5551234567", event.phoneNumber);
    }

    @Test
    public void constructor_nullPhone_storesNull() {
        AddWeightViewModel.GoalReachedEvent event =
                new AddWeightViewModel.GoalReachedEvent(null);
        assertNull(event.phoneNumber);
    }

    @Test
    public void constructor_emptyPhone_storesEmpty() {
        AddWeightViewModel.GoalReachedEvent event =
                new AddWeightViewModel.GoalReachedEvent("");
        assertEquals("", event.phoneNumber);
    }

    @Test
    public void isHandled_initially_returnsFalse() {
        AddWeightViewModel.GoalReachedEvent event =
                new AddWeightViewModel.GoalReachedEvent("5551234567");
        assertFalse(event.isHandled());
    }

    @Test
    public void setHandled_afterCall_returnsTrue() {
        AddWeightViewModel.GoalReachedEvent event =
                new AddWeightViewModel.GoalReachedEvent("5551234567");
        event.setHandled();
        assertTrue(event.isHandled());
    }
}
