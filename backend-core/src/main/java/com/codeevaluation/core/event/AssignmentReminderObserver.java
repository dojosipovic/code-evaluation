package com.codeevaluation.core.event;

import com.codeevaluation.core.service.AssignmentReminderService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class AssignmentReminderObserver {

    private final AssignmentReminderService assignmentReminderService;

    public void onAssignmentCreated(
            @Observes(during = TransactionPhase.AFTER_SUCCESS) AssignmentCreatedEvent event
    ) {
        assignmentReminderService.scheduleOrSendImmediately(
                event.assignmentId(),
                event.startsAt()
        );
    }
}
