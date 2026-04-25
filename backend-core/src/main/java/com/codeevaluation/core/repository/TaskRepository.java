package com.codeevaluation.core.repository;

import com.codeevaluation.core.api.dto.task.TaskCreateDto;
import com.codeevaluation.core.api.dto.task.TaskFilterParams;
import com.codeevaluation.core.api.dto.task.TaskPatchDto;
import com.codeevaluation.core.api.dto.task.TaskUpdateDto;
import com.codeevaluation.core.api.dto.task.TestDto;
import com.codeevaluation.core.enumeration.TaskStatus;
import com.codeevaluation.core.enumeration.TestVisibility;
import com.codeevaluation.core.helper.PagedContext;
import com.codeevaluation.core.model.Task;
import com.codeevaluation.core.model.TaskTest;
import com.codeevaluation.core.model.User;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

@ApplicationScoped
public class TaskRepository implements PanacheRepository<Task> {

    @Transactional
    public Task create(TaskCreateDto taskCreateDto, User user) {
        Task task = new Task();

        task.setUser(user);
        task.setTitle(taskCreateDto.title().trim());
        task.setDescription(taskCreateDto.description().trim());

        if (Boolean.TRUE.equals(taskCreateDto.includeStarterCode())) {
            task.setStarterCode(taskCreateDto.starterCode().code().trim());
        }

        task.setEnabled(true);
        task.setShared(Boolean.TRUE.equals(taskCreateDto.shared()));
        task.setStatus(TaskStatus.DRAFT);

        for (TestDto publicTestDto : taskCreateDto.publicTests()) {
            String input = StringUtils.defaultIfEmpty(publicTestDto.input(), null);
            String output = StringUtils.defaultIfEmpty(publicTestDto.output(), null);

            TaskTest test = new TaskTest();
            test.setInput(input);
            test.setOutput(output);
            test.setVisibility(TestVisibility.PUBLIC);
            task.addTest(test);
        }

        for (TestDto hiddenTestDto : taskCreateDto.hiddenTests()) {
            String input = StringUtils.defaultIfEmpty(hiddenTestDto.input(), null);
            String output = StringUtils.defaultIfEmpty(hiddenTestDto.output(), null);

            TaskTest test = new TaskTest();
            test.setInput(input);
            test.setOutput(output);
            test.setVisibility(TestVisibility.HIDDEN);
            task.addTest(test);
        }

        task.persist();

        return task;
    }

    public Task update(Task task, TaskUpdateDto taskUpdateDto) {
        task.setTitle(taskUpdateDto.title().trim());
        task.setDescription(taskUpdateDto.description().trim());

        if (Boolean.TRUE.equals(taskUpdateDto.includeStarterCode())) {
            task.setStarterCode(taskUpdateDto.starterCode().code().trim());
        }

        task.setEnabled(true);
        task.setShared(Boolean.TRUE.equals(taskUpdateDto.shared()));

        task.removeAllTests();

        for (TestDto publicTestDto : taskUpdateDto.publicTests()) {
            String input = StringUtils.defaultIfEmpty(publicTestDto.input(), null);
            String output = StringUtils.defaultIfEmpty(publicTestDto.output(), null);

            TaskTest test = new TaskTest();
            test.setInput(input);
            test.setOutput(output);
            test.setVisibility(TestVisibility.PUBLIC);
            task.addTest(test);
        }

        for (TestDto hiddenTestDto : taskUpdateDto.hiddenTests()) {
            String input = StringUtils.defaultIfEmpty(hiddenTestDto.input(), null);
            String output = StringUtils.defaultIfEmpty(hiddenTestDto.output(), null);

            TaskTest test = new TaskTest();
            test.setInput(input);
            test.setOutput(output);
            test.setVisibility(TestVisibility.HIDDEN);
            task.addTest(test);
        }

        task.persist();

        return task;
    }

    public Optional<Task> getTask(Long id) {
        return find(
                """
                    select distinct t from Task t
                    left join fetch t.tests
                    left join fetch t.user
                    where t.id = ?1
                """,
                id
        ).firstResultOptional();
    }

    public Task publish(Task task) {
        task.setStatus(TaskStatus.PUBLISHED);
        task.persist();
        return task;
    }

    @Transactional
    public Task patch(TaskPatchDto taskPatchDto, Task task) {
        task.setEnabled(Boolean.TRUE.equals(taskPatchDto.enabled()));
        return task;
    }

    public PanacheQuery<Task> findTasks(
            PagedContext pagedContext, TaskFilterParams taskFilterParams) {

        StringBuilder query = new StringBuilder("from Task t join fetch t.user where 1=1");
        Map<String, Object> params = new HashMap<>();

        if (!StringUtils.isBlank(pagedContext.search())) {
            query.append(
                    """
                    and (
                            lower(t.title) like :search
                            or lower(t.user.firstname) like :search
                            or lower(t.user.lastname) like :search
                            or lower(concat(t.user.firstname, ' ', t.user.lastname)) like :search
                        )
                    """);

            params.put("search", "%" + pagedContext.search().toLowerCase().trim() + "%");
        }

        if (Boolean.TRUE.equals(taskFilterParams.excludeUser())) {
            query.append(" and t.user.id != :userId");
        } else {
            query.append(" and t.user.id = :userId");
        }
        params.put("userId", taskFilterParams.user().getId());

        if (taskFilterParams.status() != null) {
            query.append(" and t.status = :status");
            params.put("status", taskFilterParams.status());
        }

        if (taskFilterParams.enabled() != null) {
            query.append(" and t.enabled = :enabled");
            params.put("enabled", taskFilterParams.enabled());
        }

        if (taskFilterParams.shared() != null) {
            query.append(" and t.shared = :shared");
            params.put("shared", taskFilterParams.shared());
        }

        Sort sort = pagedContext.sort();
        int page = pagedContext.page();
        int size = pagedContext.size();

        return find(query.toString(), sort, params).page(Page.of(page, size));
    }
}
