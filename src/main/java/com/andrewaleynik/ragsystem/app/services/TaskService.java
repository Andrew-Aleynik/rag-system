package com.andrewaleynik.ragsystem.app.services;

import com.andrewaleynik.ragsystem.domains.Task;
import com.andrewaleynik.ragsystem.domains.TaskStatus;
import com.andrewaleynik.ragsystem.domains.TaskType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

@Service
public class TaskService {
    private final ConcurrentMap<Long, Task> tasks = new ConcurrentHashMap<>();
    private final ConcurrentMap<TaskType, Semaphore> taskTypesSemaphores = new ConcurrentHashMap<>();
    private final long cleanupPeriodMillis;

    public TaskService(
            @Value("${services.task.cleanup_period_millis:60000}") long cleanupPeriodMillis,
            @Value("${services.task.permitted_count_synchronous_tasks:1}") int permittedCountRunningTasks
    ) {
        this.cleanupPeriodMillis = cleanupPeriodMillis;
        Arrays.stream(TaskType.values()).forEach(taskType ->
                taskTypesSemaphores.put(taskType, new Semaphore(permittedCountRunningTasks)));
        Thread thread = new Thread(removingTerminatedTasksRunnable());
        thread.setDaemon(true);
        thread.start();
    }

    public boolean contains(Long id) {
        return tasks.containsKey(id);
    }

    boolean tryAddTask(Long projectId, Task task) {
        return tasks.putIfAbsent(projectId, task) == null;
    }

    public Optional<Task> getTask(Long id) {
        return Optional.ofNullable(tasks.get(id));
    }

    public void updateStatus(Long id, TaskStatus newStatus) {
        tasks.computeIfPresent(id, (k, task) -> {
            task.setStatus(newStatus);
            return task;
        });
    }

    public void acquireSemaphore(TaskType taskType) throws InterruptedException {
        taskTypesSemaphores.get(taskType).acquire();
    }

    public void releaseSemaphore(TaskType taskType) {
        taskTypesSemaphores.get(taskType).release();
    }

    private Runnable removingTerminatedTasksRunnable() {
        return () -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(cleanupPeriodMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                for (Map.Entry<Long, Task> entry : tasks.entrySet()) {
                    Task task = entry.getValue();
                    if (task.getStatus().isTerminated()
                            && task.getUpdatedAt().plusMinutes(1).isBefore(LocalDateTime.now())) {
                        tasks.remove(entry.getKey());
                    }
                }
            }
        };
    }
}
