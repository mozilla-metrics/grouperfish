package com.mozilla.grouperfish.batch;

import com.mozilla.grouperfish.model.Query;
import com.mozilla.grouperfish.model.TransformConfig;
import com.mozilla.grouperfish.naming.Scope;


/**
 * The batch system component as documented at:
 * http://grouperfish.readthedocs.org/en/latest/batch_system.html
 */
public interface BatchService {

    /** Run this specific task. */
    void schedule(Task task);

    /** Run the configured transform over the query results. */
    void schedule(Scope ns, Query query, TransformConfig transform);

    /** Run all configured transforms over the query results. */
    void schedule(Scope ns, Query query);

    /**
     * Run all transforms configurations of this
     * namespace over the results of all queries.
     */
    void schedule(Scope ns);

    /** Start execution of tasks. */
    void start();

    /**
     * Stop execution of new tasks.
     * Should be called before shutting down the node.
     *
     * :TODO: Next:
     * We probably need some sort of lifecycle events so
     * services can manage this transparently.
     */
    void stop();

}
