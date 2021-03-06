package com.mozilla.grouperfish.batch.handlers;

import com.mozilla.grouperfish.model.Fail;
import com.mozilla.grouperfish.model.Task;

public interface TaskHandler {

    /**
     * Carry out some processing on this task.
     * @return The same task, or some modified version with more information.
     */
    Task handle(Task task) throws Fail;

}
