package org.cus.model;

/**
 * Tuple representing a level reading.
 *
 * @param level the measured Level
 * @param timestamp the timestamp of the received message
 */
public record Reading(float level, long timestamp) { }
