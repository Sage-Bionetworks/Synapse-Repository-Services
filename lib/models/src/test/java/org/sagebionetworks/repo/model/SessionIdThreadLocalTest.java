package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

public class SessionIdThreadLocalTest {

	@Test
	public void testGetThreadsSessionIdWithNotInitialized() throws Exception {

		ExecutorService executor = Executors.newFixedThreadPool(1);
		Future<Optional<String>> future = executor.submit(() -> {
			// call under test
			return SessionIdThreadLocal.getThreadsSessionId();

		});
		assertEquals(Optional.empty(), future.get());

	}

	@Test
	public void testCreateGetClearWithMultipleThreads() throws Exception {

		ExecutorService executor = Executors.newFixedThreadPool(1);
		Future<String> future = executor.submit(() -> {
			// call under test
			return SessionIdThreadLocal.createNewSessionIdForThread();

		});
		// call under test
		String thisThreadStartSessionId = SessionIdThreadLocal.createNewSessionIdForThread();
		String otherThreadStartSessionId = future.get();
		assertNotNull(otherThreadStartSessionId);
		assertEquals(Optional.of(otherThreadStartSessionId), executor.submit(() -> {
			// call under test
			return SessionIdThreadLocal.getThreadsSessionId();
		}).get());
		assertEquals(Optional.of(thisThreadStartSessionId), SessionIdThreadLocal.getThreadsSessionId());

		executor.submit(() -> {
			// call under test
			SessionIdThreadLocal.clearThreadsSessionId();
		}).get();
		assertEquals(Optional.empty(), executor.submit(() -> {
			// call under test
			return SessionIdThreadLocal.getThreadsSessionId();
		}).get());
		// clear of the other thread should not have changed the current thread.
		assertEquals(Optional.of(thisThreadStartSessionId), SessionIdThreadLocal.getThreadsSessionId());

		// call under test
		SessionIdThreadLocal.clearThreadsSessionId();
		assertEquals(Optional.empty(), SessionIdThreadLocal.getThreadsSessionId());

	}

}
