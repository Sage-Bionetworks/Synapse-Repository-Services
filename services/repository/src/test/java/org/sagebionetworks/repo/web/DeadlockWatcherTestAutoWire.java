package org.sagebionetworks.repo.web;

import static org.junit.Assert.*;

import java.sql.BatchUpdateException;

import org.apache.commons.logging.Log;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Autowired version of the DeadlockWatcherTest
 * @author jmhill
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DeadlockWatcherTestAutoWire {
	
	@Autowired
	DeadlockWatcher deadlockWatcher;
	
	@Autowired
	GenericEntityController entityController;
	
	@Test
	public void testDeadlock(){
		Log mockLog = Mockito.mock(Log.class);
		deadlockWatcher.setLog(mockLog);
		DeadlockLoserDataAccessException e = new DeadlockLoserDataAccessException("Some deadlock message", new BatchUpdateException());
		try{
			entityController.throwDeadlockException(e);
			fail("Should have thrown a DeadlockLoserDataAccessException");
		}catch(DeadlockLoserDataAccessException e2){
			
		}
		// The log should have been hit at least 4 times
		verify(mockLog, atLeast(4)).debug(any(String.class));
	}

}
