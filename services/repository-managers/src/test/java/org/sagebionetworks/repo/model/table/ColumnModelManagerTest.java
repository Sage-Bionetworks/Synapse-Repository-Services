package org.sagebionetworks.repo.model.table;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.table.ColumnModelManagerImpl;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit test for the ColumnManager.
 * @author John
 *
 */
public class ColumnModelManagerTest {
	
	ColumnModelDAO mockColumnModelDAO;
	ColumnModelManagerImpl columnModelManager;
	UserInfo user;
	
	@Before
	public void before(){
		mockColumnModelDAO = Mockito.mock(ColumnModelDAO.class);
		columnModelManager = new ColumnModelManagerImpl();
		user = new UserInfo(false);
		user.setIndividualGroup(new UserGroup());
		ReflectionTestUtils.setField(columnModelManager, "columnModelDao", mockColumnModelDAO);
	}
	
	@Test
	public void testListColumnModels(){
		String prefix = "A";
		List<ColumnModel> results = new LinkedList<ColumnModel>();
		ColumnModel cm = new ColumnModel();
		cm.setName("abb");
		results.add(cm);
		when(mockColumnModelDAO.listColumnModels(prefix, Long.MAX_VALUE, 0)).thenReturn(results);
		when(mockColumnModelDAO.listColumnModelsCount(prefix)).thenReturn(1l);
		
		// make the call
		PaginatedResults<ColumnModel> page = columnModelManager.listColumnModels(user, prefix, Long.MAX_VALUE, 0);
		assertNotNull(page);
		assertEquals(1l, page.getTotalNumberOfResults());
		assertEquals(results, page.getResults());
	}
}
