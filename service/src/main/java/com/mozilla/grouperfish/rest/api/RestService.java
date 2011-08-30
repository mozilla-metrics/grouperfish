package com.mozilla.grouperfish.rest.api;


public interface RestService {

    public Daemon start();

    public static interface Daemon {
        /** Wait for shutdown of the daemon. Intercept the interrupt to clean up your resources. */
        void join() throws InterruptedException;
    }

}
