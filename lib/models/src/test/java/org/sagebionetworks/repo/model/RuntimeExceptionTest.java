package org.sagebionetworks.repo.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.web.ForbiddenException;

/**
 * This test exists because many of the exceptions we throw were extending Exception.  When an Exception is thrown,
 * it does not trigger a database roll back of the current transaction.  See: PLFM-1483.
 * 
 * Since we could not get Spring to roll back on Exceptions we converted all of these formally checked exceptions to unchecked exceptions.
 * 
 */
public class RuntimeExceptionTest {
	
	@Test
	public void testDataStoreException(){
		Exception exception = new DatastoreException();
		assertTrue((exception instanceof RuntimeException),"Throwing a DatastoreException will not roll back a transaction because it is not a RuntimeException. See: PLFM-1483");
	}
	
	@Test
	public void testInvalidModelException(){
		Exception exception = new InvalidModelException();
		assertTrue((exception instanceof RuntimeException),"Throwing a InvalidModelException will not roll back a transaction because it is not a RuntimeException. See: PLFM-1483");
	}
	
	@Test
	public void testConflictingUpdateException(){
		Exception exception = new ConflictingUpdateException();
		assertTrue((exception instanceof RuntimeException),"Throwing a ConflictingUpdateException will not roll back a transaction because it is not a RuntimeException. See: PLFM-1483");
	}
	
	@Test
	public void testNameConflictException(){
		Exception exception = new NameConflictException();
		assertTrue((exception instanceof RuntimeException),"Throwing a NameConflictException will not roll back a transaction because it is not a RuntimeException. See: PLFM-1483");
	}
	
	@Test
	public void testUnauthorizedException(){
		Exception exception = new UnauthorizedException();
		assertTrue((exception instanceof RuntimeException),"Throwing a UnauthorizedException will not roll back a transaction because it is not a RuntimeException. See: PLFM-1483");
	}
	
	@Test
	public void testForbiddenException(){
		Exception exception = new ForbiddenException();
		assertTrue((exception instanceof RuntimeException),"Throwing a ForbiddenException will not roll back a transaction because it is not a RuntimeException. See: PLFM-1483");
	}


}
