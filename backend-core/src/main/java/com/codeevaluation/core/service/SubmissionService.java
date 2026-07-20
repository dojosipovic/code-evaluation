package com.codeevaluation.core.service;

import com.codeevaluation.core.api.dto.submission.SubmissionDetailResponseDto;
import com.codeevaluation.core.api.dto.submission.SubmissionFilterParams;
import com.codeevaluation.core.api.query.SubmissionListQueryParams;
import com.codeevaluation.core.api.dto.PagedResponse;
import com.codeevaluation.core.api.dto.submission.SubmissionListItemDto;
import com.codeevaluation.core.helper.PagedContext;
import com.codeevaluation.core.helper.PagedSearchSubmissionImpl;
import com.codeevaluation.core.helper.SubmissionAccessPolicy;
import com.codeevaluation.core.model.Submission;
import com.codeevaluation.core.model.SubmissionSimilarity;
import com.codeevaluation.core.model.User;
import com.codeevaluation.core.provider.CurrentUserProvider;
import com.codeevaluation.core.repository.SubmissionRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class SubmissionService {

    private final PagedSearchSubmissionImpl pagedSearchSubmission;
    private final SubmissionRepository submissionRepository;
    private final CurrentUserProvider currentUserProvider;

    public PagedResponse<SubmissionListItemDto> getSubmissions(
            SubmissionListQueryParams submissionListQueryParams
    ) {
        PagedContext pagedContext = pagedSearchSubmission.generateFrom(submissionListQueryParams);
        SubmissionFilterParams submissionFilterParams =
                pagedSearchSubmission.generateFilterParams(submissionListQueryParams)
                        .user(currentUserProvider.getCurrentUser())
                        .build();

        PanacheQuery<Submission> query =
                submissionRepository.findSubmissions(pagedContext, submissionFilterParams);
        List<SubmissionListItemDto> items = SubmissionListItemDto.from(query.list());
        long totalItems = query.count();
        int page = pagedContext.page();
        int size = pagedContext.size();

        return new PagedResponse<>(items, page, size, totalItems);
    }

    @Transactional
    public SubmissionDetailResponseDto getSubmission(Long submissionId) {
        Submission submission = submissionRepository.findByIdWithRelations(submissionId)
                .orElseThrow(() -> new NotFoundException("Submission not found"));

        User currentUser = currentUserProvider.getCurrentUser();
        if (!SubmissionAccessPolicy.canSeeSubmission(submission, currentUser)) {
            throw new ForbiddenException("You cannot see this submission");
        }

        List<SubmissionSimilarity> similarities =
                submissionRepository.findSimilaritiesForSubmission(submission.getId());

        return SubmissionDetailResponseDto.from(
                submission,
                SubmissionAccessPolicy.canSeeHiddenExpectedOutputs(submission, currentUser),
                similarities
        );
    }
}
