package com.example.cs360weighttracker.features.weight;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cs360weighttracker.data.UserRepository;
import com.example.cs360weighttracker.data.WeightRepository;

/**
 * ViewModel for the "add weight" screen.
 *
 * <p>Handles validation and insertion of a new weight entry via
 * {@link WeightRepository}. After a successful insert it checks the
 * user's goal via {@link UserRepository} and emits a
 * {@link GoalReachedEvent} if the goal was reached and the SMS has
 * not yet been sent.</p>
 */
public class AddWeightViewModel extends ViewModel {

    private WeightRepository weightRepository;
    private UserRepository userRepository;
    private int userId = -1;

    // Save result: null = not yet attempted, true = success, false = failure
    private final MutableLiveData<Boolean> saveResult = new MutableLiveData<>();
    // Goal reached event: emits when weight <= goal and SMS hasn't been sent yet
    private final MutableLiveData<GoalReachedEvent> goalReachedEvent = new MutableLiveData<>();

    /**
     * One-time initializer — supplies repositories and userId.
     */
    public void init(WeightRepository weightRepository, UserRepository userRepository, int userId) {
        if (this.weightRepository != null) return; // Already initialized
        this.weightRepository = weightRepository;
        this.userRepository = userRepository;
        this.userId = userId;
    }

    /**
     * Observable result of the save operation.
     */
    public LiveData<Boolean> getSaveResult() {
        return saveResult;
    }

    /**
     * Observable event emitted when the goal is reached and an SMS may
     * need to be sent.
     */
    public LiveData<GoalReachedEvent> getGoalReachedEvent() {
        return goalReachedEvent;
    }

    /**
     * Validates input, inserts the weight row, and emits events as
     * appropriate.
     */
    public void saveWeight(String date, String weightStr, String notes) {
        if (date.isEmpty() || weightStr.isEmpty()) {
            saveResult.setValue(false);
            return;
        }

        try {
            double weight = Double.parseDouble(weightStr);
            boolean inserted = weightRepository.addWeight(userId, date, weight, notes);

            if (!inserted) {
                saveResult.setValue(false);
                return;
            }

            // Check goal
            double goalWeight = userRepository.getGoalWeight(userId);
            if (weight <= goalWeight && !userRepository.isGoalSmsSent(userId)) {
                String phoneNumber = userRepository.getPhoneNumber(userId);
                goalReachedEvent.setValue(new GoalReachedEvent(phoneNumber));
            }

            saveResult.setValue(true);
        } catch (NumberFormatException e) {
            saveResult.setValue(false);
        }
    }

    /**
     * Called by the Activity ONLY after SMS was actually sent successfully.
     */
    public void markGoalSmsSent() {
        userRepository.setGoalSmsSent(userId);
    }

    /**
     * Event data for goal-reached SMS trigger. Contains the phone
     * number and a handled flag to prevent re-delivery on
     * configuration changes.
     */
    public static class GoalReachedEvent {
        public final String phoneNumber;
        private boolean handled = false;

        public GoalReachedEvent(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public boolean isHandled() {
            return handled;
        }

        public void setHandled() {
            this.handled = true;
        }
    }
}
