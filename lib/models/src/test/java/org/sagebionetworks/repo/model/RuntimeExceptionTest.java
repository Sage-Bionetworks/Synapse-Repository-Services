package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * This test exists because many of the exceptions we throw were extending Exception.  When an Exception is thrown,
 * it does not trigger a database roll back of the current transaction.  See: PLFM-1483.
 * 
 * Since we could not get Spring to roll back on Exceptions we converted all of these formally checked exceptions to unchecked exceptions.
 * 
 * 
 * @author John
 *
 */
public class RuntimeExceptionTest {
	
	@Test
	public void testDataStoreException(){
		Exception exception = new DatastoreException();
		assertTrue("Throwing a DatastoreException will not roll back a transaction because it is not a RuntimeException. See: PLFM-1483",(exception instanceof RuntimeException));
	}
	
	@Test
	public void testInvalidModelException(){
		Exception exception = new InvalidModelException();
		assertTrue("Throwing a InvalidModelException will not roll back a transaction because it is not a RuntimeException. See: PLFM-1483",(exception instanceof RuntimeException));
	}
	
	@Test
	public void testConflictingUpdateException(){
		Exception exception = new ConflictingUpdateException();
		assertTrue("Throwing a ConflictingUpdateException will not roll back a transaction because it is not a RuntimeException. See: PLFM-1483",(exception instanceof RuntimeException));
	}
	
	@Test
	public void testNameConflictException(){
		Exception exception = new NameConflictException();
		assertTrue("Throwing a NameConflictException will not roll back a transaction because it is not a RuntimeException. See: PLFM-1483",(exception instanceof RuntimeException));
	}
	
	@Test
	public void testNotFoundException(){
		Exception exception = new NotFoundException();
//		assertTrue("Throwing a NotFoundException will not roll back a transaction because it is not a RuntimeException. See: PLFM-1483",(exception instanceof RuntimeException));
	}
	
	@Test
	public void testUnauthorizedException(){
		Exception exception = new UnauthorizedException();
		assertTrue("Throwing a UnauthorizedException will not roll back a transaction because it is not a RuntimeException. See: PLFM-1483",(exception instanceof RuntimeException));
	}
	
	@Test
	public void testForbiddenException(){
		Exception exception = new ForbiddenException();
		assertTrue("Throwing a ForbiddenException will not roll back a transaction because it is not a RuntimeException. See: PLFM-1483",(exception instanceof RuntimeException));
	}


}
