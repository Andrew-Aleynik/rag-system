package com.andrewaleynik.ragsystem.app.services;

import com.andrewaleynik.ragsystem.domains.Task;
import com.andrewaleynik.ragsystem.domains.TaskStatus;
import com.andrewaleynik.ragsystem.domains.TaskType;
import org.awaitility.core.ConditionTimeoutException;
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
        assertFalse(taskService.contains(1L));
    }

    @Test
    void testContainsTrue() {
        taskService.tryAddTask(1L, createTask(TaskType.INDEXING, TaskStatus.NOT_QUEUED));

        assertTrue(taskService.contains(1L));
    }

    @Test
    void testAddTaskSuccess() {
        Task task = createTask(TaskType.INDEXING, TaskStatus.NOT_QUEUED);

        taskService.tryAddTask(1L, task);

        assertTrue(taskService.contains(1L));
    }

    @Test
    void testUpdateExistedTaskStatusSuccess() {
        TaskStatus expected = TaskStatus.FAILED;

        taskService.tryAddTask(1L, createTask(TaskType.INDEXING, TaskStatus.NOT_QUEUED));

        assertDoesNotThrow(() -> taskService.updateStatus(1L, expected));
        assertEquals(getTask(taskService, 1L).getStatus(), expected);
    }

    @Test
    void testUpdateNotExistedTaskStatusNotThrowException() {
        assertDoesNotThrow(() -> taskService.updateStatus(999L, TaskStatus.DONE));
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

    @ParameterizedTest
    @EnumSource(TaskStatus.class)
    void testCleanupThreadWorksCorrect(TaskStatus taskStatus) {
        Task oldTask = Task.builder()
                .type(TaskType.INDEXING)
                .status(taskStatus)
                .updatedAt(LocalDateTime.now().minusMinutes(5))
                .build();
        taskService.tryAddTask(1L, oldTask);

        try {
            await().atMost(Duration.ofSeconds(1))
                    .pollInterval(Duration.ofMillis(50))
                    .until(() -> !taskService.contains(1L));
        } catch (ConditionTimeoutException ignored) {
        }

        boolean expected = taskStatus.isTerminated();
        boolean actual = !taskService.contains(1L);

        assertEquals(expected, actual,
                "Task with status " + taskStatus + " should " + (expected ? "" : "not ") + "be removed");
    }

    private Task getTask(TaskService taskService, Long taskId) {
        String tasksFieldName = "tasks";
        try {
            Field tasksField = taskService.getClass().getDeclaredField(tasksFieldName);
            tasksField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Task task = ((ConcurrentMap<Long, Task>) tasksField.get(taskService)).get(taskId);
            tasksField.setAccessible(false);
            return task;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("No such field: " + tasksFieldName, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Not accessible field: " + tasksFieldName, e);
        }
    }

    private Task createTask(TaskType type, TaskStatus status) {
        return Task.builder()
                .type(type)
                .status(status)
                .updatedAt(LocalDateTime.now())
                .build();
    }
}