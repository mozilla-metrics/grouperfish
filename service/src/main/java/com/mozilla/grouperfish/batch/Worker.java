package com.mozilla.grouperfish.batch;

import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mozilla.grouperfish.batch.handlers.TaskHandler;

class Worker extends Thread {

    private static Logger log = LoggerFactory.getLogger(Worker.class);
    private static final int NUM_TRIES = 3;

    private final BlockingQueue<Task> inQueue;
    private final BlockingQueue<Task> outQueue;
    private final BlockingQueue<Task> failQueue;
    private final TaskHandler actor;
    private final String name;

    public Worker(final BlockingQueue<Task> failQueue,
                  final BlockingQueue<Task> inQueue,
                  final BlockingQueue<Task> outQueue,
                  final TaskHandler actor) {
        this.inQueue = inQueue;
        this.outQueue = outQueue;
        this.failQueue = failQueue;
        this.actor = actor;
        this.name = String.format("[Worker for %s]", actor.getClass().getSimpleName());
    }

    public String toString() {
        return name;
    }

    public void run() {
        Task task = null;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                task = inQueue.take();
                try {
                    // :TODO: NEXT:
                    // If power fails, tasks can go MIA here.
                    // We should maintain a global map of tasks, check it periodically, and restart tasks that went MIA.
                    // Task update their status there, and clients could check the status using a GET /run/... call.
                    task = actor.handle(task);
                }
                catch (final RetryException e) {
                    log.warn(String.format("%s %s: failed with message '%s'", name, task, e.getMessage()));
                    if (task.failures().size() >= NUM_TRIES) {
                        log.error(String.format("%s %s: Error details:", name, task), e);
                        log.error(String.format("%s %s: Retries exhausted. Failing.", name, task));
                        failQueue.put(task);
                    }
                    else {
                        log.warn(String.format("%s %s: recording failure & requeuing...", name, task));
                        inQueue.put(task.fail(e.getMessage()));
                    }
                    continue;
                }
                catch (final Exception e) {
                    log.error(String.format("%s %s: Exception while handling.", name, task));
                    log.error(String.format("%s %s: Error details:", name, task), e);
                    failQueue.put(task.fail(e.getMessage()));
                    continue;
                }

                if (outQueue != null) outQueue.put(task);
                task = null;
            }
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public void cancel() {
        interrupt();
    }

}
