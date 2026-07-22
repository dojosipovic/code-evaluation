package com.codeevaluation.core.event;

import com.codeevaluation.core.service.AssignmentTimedActionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class AssignmentReminderObserver {

    private final AssignmentTimedActionService assignmentTimedActionService;

    public void onAssignmentCreated(
            @Observes(during = TransactionPhase.AFTER_SUCCESS) AssignmentCreateEvent event
    ) {
        assignmentTimedActionService.scheduleOnCreated(
                event.assignmentId(),
                event.startsAt(),
                event.endsAt()
        );
    }
}
