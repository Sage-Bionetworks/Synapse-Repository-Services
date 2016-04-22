package org.sagebionetworks.repo.manager.table;

import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewScopeDao;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class TableViewManagerImplTest {

	@Mock
	ViewScopeDao viewScopeDao;
	@Mock
	ColumnModelManager columnModelManager;
	@Mock
	TableStatusManager mockTableStatusManager;
	
	TableViewManagerImpl manager;
	
	UserInfo userInfo;
	List<String> schema;
	List<String> scope;
	String viewId;

	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		
		manager = new TableViewManagerImpl();
		ReflectionTestUtils.setField(manager, "viewScopeDao", viewScopeDao);
		ReflectionTestUtils.setField(manager, "columModelManager", columnModelManager);
		ReflectionTestUtils.setField(manager, "tableStatusManager", mockTableStatusManager);
		
		userInfo = new UserInfo(false, 888L);
		schema = Lists.newArrayList("1","2","3");
		scope = Lists.newArrayList("syn123", "syn456");
		viewId = "syn555";
		
	}
	
	@Test
	public void testSetViewSchemaAndScope(){
		// call under test
		manager.setViewSchemaAndScope(userInfo, schema, scope, viewId);
		verify(viewScopeDao).setViewScope(555L, Sets.newHashSet(123L, 456L));
		verify(columnModelManager).bindColumnToObject(userInfo, schema, viewId);
		verify(mockTableStatusManager).setTableToProcessingAndTriggerUpdate(viewId);
	}
	
	@Test
	public void testSetViewSchemaAndScopeWithNullSchema(){
		schema = null;
		// call under test
		manager.setViewSchemaAndScope(userInfo, schema, scope, viewId);
		verify(viewScopeDao).setViewScope(555L, Sets.newHashSet(123L, 456L));
		verify(columnModelManager).bindColumnToObject(userInfo, null, viewId);
		verify(mockTableStatusManager).setTableToProcessingAndTriggerUpdate(viewId);
	}
	
	@Test
	public void testSetViewSchemaAndScopeWithNullScope(){
		scope = null;
		// call under test
		manager.setViewSchemaAndScope(userInfo, schema, scope, viewId);
		verify(viewScopeDao).setViewScope(555L, null);
		verify(columnModelManager).bindColumnToObject(userInfo, schema, viewId);
		verify(mockTableStatusManager).setTableToProcessingAndTriggerUpdate(viewId);
	}

}
