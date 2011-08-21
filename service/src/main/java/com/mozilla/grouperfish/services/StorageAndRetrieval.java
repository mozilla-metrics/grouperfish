package com.mozilla.grouperfish.services;


public interface StorageAndRetrieval {

    public Daemon start();

    public static interface Daemon {
        /** Wait for shutdown of the daemon. Intercept the interrupt to clean up your resources. */
        void join() throws InterruptedException;
    }

}
