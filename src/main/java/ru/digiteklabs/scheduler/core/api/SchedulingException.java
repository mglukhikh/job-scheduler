package ru.digiteklabs.scheduler.core.api;

/**
 * A checked exception for a scheduler
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
