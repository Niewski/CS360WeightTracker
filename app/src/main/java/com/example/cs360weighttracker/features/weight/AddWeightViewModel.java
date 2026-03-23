package com.example.cs360weighttracker.features.weight;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cs360weighttracker.data.UserRepository;
import com.example.cs360weighttracker.data.WeightRepository;

public class AddWeightViewModel extends ViewModel {

    private WeightRepository weightRepository;
    private UserRepository userRepository;
    private int userId = -1;

    // Save result: null = not yet attempted, true = success, false = failure
    private final MutableLiveData<Boolean> saveResult = new MutableLiveData<>();
    // Goal reached event: emits true when weight <= goal and SMS hasn't been sent yet
    private final MutableLiveData<GoalReachedEvent> goalReachedEvent = new MutableLiveData<>();

    public void init(WeightRepository weightRepository, UserRepository userRepository, int userId) {
        if (this.weightRepository != null) return; // Already initialized
        this.weightRepository = weightRepository;
        this.userRepository = userRepository;
        this.userId = userId;
    }

    public LiveData<Boolean> getSaveResult() {
        return saveResult;
    }

    public LiveData<GoalReachedEvent> getGoalReachedEvent() {
        return goalReachedEvent;
    }

    public void saveWeight(String date, String weightStr) {
        if (date.isEmpty() || weightStr.isEmpty()) {
            saveResult.setValue(false);
            return;
        }

        try {
            double weight = Double.parseDouble(weightStr);
            boolean inserted = weightRepository.addWeight(userId, date, weight);

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
     * Event data for goal-reached SMS trigger.
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
