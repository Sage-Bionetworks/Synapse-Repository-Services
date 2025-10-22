package org.sagebionetworks.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.worker.utils.StackStatusGate;

@ExtendWith(MockitoExtension.class)
public class StackStatusGateTest {
	
	@Mock
	private StackStatusDao mockStackStatusDao;
	
	@InjectMocks
	private StackStatusGate gate;

	@Test
	public void testCanRunReadOnly(){
		when(mockStackStatusDao.isStackReadWrite()).thenReturn(false);
		assertFalse(gate.canRun());
	}
	
	@Test
	public void testCanRunReadWrite(){
		when(mockStackStatusDao.isStackReadWrite()).thenReturn(true);
		assertTrue(gate.canRun());
	}
}
