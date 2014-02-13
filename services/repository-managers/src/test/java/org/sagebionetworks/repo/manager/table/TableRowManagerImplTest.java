package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

public class TableRowManagerImplTest {
	
	TableRowTruthDAO mockTruthDao;
	AuthorizationManager mockAuthManager;
	TableRowManagerImpl manager;
	List<ColumnModel> models;
	UserInfo user;
	String tableId;
	RowSet set;
	RowReferenceSet refSet;
	
	@Before
	public void before(){
		mockTruthDao = Mockito.mock(TableRowTruthDAO.class);
		mockAuthManager = Mockito.mock(AuthorizationManager.class);
		manager = new TableRowManagerImpl();
		user = new UserInfo(false, 7L);
		models = TableModelUtils.createOneOfEachType();
		tableId = "syn123";
		List<Row> rows = TableModelUtils.createRows(models, 10);
		set = new RowSet();
		set.setTableId(tableId);
		set.setHeaders(TableModelUtils.getHeaders(models));
		set.setRows(rows);
		
		refSet = new RowReferenceSet();
		refSet.setTableId(tableId);
		refSet.setHeaders(TableModelUtils.getHeaders(models));
		
		ReflectionTestUtils.setField(manager, "tableRowTruthDao", mockTruthDao);
		ReflectionTestUtils.setField(manager, "authorizationManager", mockAuthManager);
		
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testAppendRowsUnauthroized() throws DatastoreException, NotFoundException, IOException{
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(false);
		manager.appendRows(user, tableId, models, set);
	}
	
	@Test
	public void testAppendRowsHappy() throws DatastoreException, NotFoundException, IOException{
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(true);
		when(mockTruthDao.appendRowSetToTable(user.getId().toString(), tableId, models, set)).thenReturn(refSet);
		RowReferenceSet results = manager.appendRows(user, tableId, models, set);
		assertEquals(refSet, results);
	}

}
