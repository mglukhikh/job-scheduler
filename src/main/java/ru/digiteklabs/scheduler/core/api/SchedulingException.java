package ru.digiteklabs.scheduler.core.api;

/**
 * A checked exception for a scheduler.
 * Used when a scheduler cannot perform some scheduling operation
 *
 * @author Mikhail Glukhikh
 */
public class SchedulingException extends Exception {

    /**
     * Constructs a scheduling exception
     * @param message an exception message
     */
    public SchedulingException(final String message) {
        super(message);
    }
}
