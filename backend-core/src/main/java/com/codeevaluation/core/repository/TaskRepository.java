package com.codeevaluation.core.repository;

import com.codeevaluation.core.api.dto.task.TaskCreateDto;
import com.codeevaluation.core.api.dto.task.TaskUpdateDto;
import com.codeevaluation.core.api.dto.task.TestDto;
import com.codeevaluation.core.enumeration.TaskStatus;
import com.codeevaluation.core.enumeration.TestVisibility;
import com.codeevaluation.core.model.Task;
import com.codeevaluation.core.model.TaskTest;
import com.codeevaluation.core.model.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
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
        return find("""
                    select distinct t from Task t
                    left join fetch t.tests
                    left join fetch t.user
                    where t.id = ?1
                """,
                id
        ).firstResultOptional();
    }
}
