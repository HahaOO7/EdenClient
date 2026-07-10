package at.haha007.edenclient.callbacks;


import java.util.Optional;

public interface StepUpHeightCallback {
    Event<StepUpHeightCallback> EVENT = new Event<>(
            listeners -> () -> {
                for (StepUpHeightCallback listener : listeners) {
                    Optional<Float> stepUpHeight = listener.getStepUpHeight();
                    if (stepUpHeight.isPresent()) {
                        return stepUpHeight;
                    }
                }
                return Optional.empty();
            });

    Optional<Float> getStepUpHeight();
}
