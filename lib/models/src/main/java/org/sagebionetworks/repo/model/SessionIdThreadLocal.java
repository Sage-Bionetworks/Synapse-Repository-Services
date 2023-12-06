package org.sagebionetworks.repo.model;

import java.util.Optional;
import java.util.UUID;

/**
 * Provides both binding and retrieval of the current thread's sessionId.
 * 
 * A new session ID is created at the start of a web service request and then
 * bound to the thread local. The session ID can then be fetched from anywhere
 * in the current thread's stack.
 *
 */
public class SessionIdThreadLocal {

	// The default initial value will be null for each thread.
	private static final ThreadLocal<String> sessionIdThreadLocal = new ThreadLocal<String>();

	/**
	 * Get the session ID bound to the current thread.
	 * 
	 * @return {@link Optional#empty()} if the thread does not have a session ID.
	 */
	public static Optional<String> getThreadsSessionId() {
		return Optional.ofNullable(sessionIdThreadLocal.get());
	}

	/**
	 * Assign a new UUID to the current thread's session Id.
	 * 
	 * @return The newly created UUID bound to the caller's thread.
	 */
	public static String createNewSessionIdForThread() {
		String sessionId = UUID.randomUUID().toString();
		sessionIdThreadLocal.set(sessionId);
		return sessionId;
	}

	/**
	 * Clear the session ID for the calling thread.
	 */
	public static void clearThreadsSessionId() {
		sessionIdThreadLocal.set(null);
	}

}
