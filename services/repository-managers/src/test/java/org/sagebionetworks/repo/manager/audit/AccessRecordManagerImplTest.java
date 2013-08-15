package org.sagebionetworks.repo.manager.audit;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.sagebionetworks.audit.dao.AccessRecordDAO;
import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * This is a unit test for the AccessManagerImpl
 * 
 * @author John
 *
 */
public class AccessRecordManagerImplTest {

	private AccessRecordDAO mockAccessRecordDAO;
	private AccessRecordManagerImpl manager;
	
	@Before
	public void before(){
		mockAccessRecordDAO = Mockito.mock(AccessRecordDAO.class);
		manager = new AccessRecordManagerImpl();
		ReflectionTestUtils.setField(manager, "accessRecordDAO", mockAccessRecordDAO);
	}
	
	@Test
	public void test() throws IOException{
		List<AccessRecord> list = AuditTestUtils.createList(3, 123);
		when(mockAccessRecordDAO.saveBatch(list)).thenReturn("thekey!");
		String key = manager.saveBatch(list);
		assertEquals("thekey!", key);
	}
}

