package com.andrewaleynik.ragsystem.app.services.core;

import com.andrewaleynik.ragsystem.data.ProjectData;
import com.andrewaleynik.ragsystem.domains.Task;
import com.andrewaleynik.ragsystem.domains.TaskId;
import com.andrewaleynik.ragsystem.domains.TaskStatus;
import com.andrewaleynik.ragsystem.domains.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

class TaskServiceTest {

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(100, 1);
    }

    @Test
    void testContainsFalse() {
        assertFalse(taskService.contains(new TaskId(ProjectData.class, 1L)));
    }

    @Test
    void testContainsTrue() {
        TaskId taskId = new TaskId(ProjectData.class, 1L);

        taskService.tryAddTask(createTask(taskId, TaskType.INDEXING, TaskStatus.NOT_QUEUED));

        assertTrue(taskService.contains(taskId));
    }

    @Test
    void testAddTaskSuccess() {
        TaskId taskId = new TaskId(ProjectData.class, 1L);
        Task task = createTask(taskId, TaskType.INDEXING, TaskStatus.NOT_QUEUED);

        taskService.tryAddTask(task);

        assertTrue(taskService.contains(taskId));
    }

    @Test
    void testUpdateExistedTaskStatusSuccess() {
        TaskId taskId = new TaskId(ProjectData.class, 1L);
        TaskStatus expected = TaskStatus.FAILED;

        taskService.tryAddTask(createTask(taskId, TaskType.INDEXING, TaskStatus.NOT_QUEUED));

        assertDoesNotThrow(() -> taskService.updateStatus(taskId, expected));
        assertEquals(getTask(taskService, taskId).getStatus(), expected);
    }

    @Test
    void testUpdateNotExistedTaskStatusNotThrowException() {
        assertDoesNotThrow(() -> taskService.updateStatus(new TaskId(ProjectData.class, 999L), TaskStatus.DONE));
    }

    @Test
    void testAcquireSemaphoreShoudBlock() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger acquired = new AtomicInteger(0);

        Thread t1 = new Thread(() -> {
            try {
                taskService.acquireSemaphore(TaskType.INDEXING);
                acquired.incrementAndGet();
                latch.countDown();
                await().atMost(Duration.ofSeconds(1)).until(() -> acquired.get() != 2);
                taskService.releaseSemaphore(TaskType.INDEXING);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                latch.await();
                taskService.acquireSemaphore(TaskType.INDEXING);
                acquired.incrementAndGet();
                taskService.releaseSemaphore(TaskType.INDEXING);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertEquals(2, acquired.get());
    }

    @ParameterizedTest
    @EnumSource(TaskType.class)
    void testSemaphorePerTaskTypeIndependent(TaskType taskType) throws InterruptedException {
        for (TaskType other : TaskType.values()) {
            if (other != taskType) {
                taskService.acquireSemaphore(other);
            }
        }
        assertTimeout(Duration.ofMillis(100), () -> {
            taskService.acquireSemaphore(taskType);
            taskService.releaseSemaphore(taskType);
        });
        for (TaskType other : TaskType.values()) {
            if (other != taskType) {
                taskService.releaseSemaphore(other);
            }
        }
    }

    private Task getTask(TaskService taskService, TaskId taskId) {
        String tasksFieldName = "tasks";
        try {
            Field tasksField = taskService.getClass().getDeclaredField(tasksFieldName);
            tasksField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Task task = ((ConcurrentMap<TaskId, Task>) tasksField.get(taskService)).get(taskId);
            tasksField.setAccessible(false);
            return task;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("No such field: " + tasksFieldName, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Not accessible field: " + tasksFieldName, e);
        }
    }

    private Task createTask(TaskId taskId, TaskType type, TaskStatus status) {
        return Task.builder()
                .id(taskId)
                .type(type)
                .status(status)
                .updatedAt(LocalDateTime.now())
                .build();
    }
}