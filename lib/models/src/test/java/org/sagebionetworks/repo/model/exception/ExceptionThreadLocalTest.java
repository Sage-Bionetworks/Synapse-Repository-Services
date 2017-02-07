package org.sagebionetworks.repo.model.exception;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.repo.model.ConflictingUpdateException;

public class ExceptionThreadLocalTest {

	@Test
	public void testPopEmptyStack() {
		assertNull(ExceptionThreadLocal.pop(Throwable.class));
	}

	@Test
	public void testPushAndPop() {
		Throwable ex = new ConflictingUpdateException();
		ExceptionThreadLocal.push(ex);
		ExceptionThreadLocal.push(ex);
		ExceptionThreadLocal.push(new IllegalArgumentException());
		ExceptionThreadLocal.push(new IllegalArgumentException());
		assertEquals(ex, ExceptionThreadLocal.pop(IllegalArgumentException.class));
		// stack has been emptied
		assertNull(ExceptionThreadLocal.pop(IllegalArgumentException.class));
	}

	@Test
	public void testOnlyIgnoreType() {
		ExceptionThreadLocal.push(new IllegalArgumentException());
		ExceptionThreadLocal.push(new IllegalArgumentException());
		assertNull(ExceptionThreadLocal.pop(IllegalArgumentException.class));
	}
}
