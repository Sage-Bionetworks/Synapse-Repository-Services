package org.sagebionetworks.repo.web.service.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.repo.model.table.ViewType;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;

@RunWith(MockitoJUnitRunner.class)
public class EntityViewMetadataProviderTest {
	@Mock
	TableViewManager mockFileViewManager;
	
	EntityViewMetadataProvider provider;
	
	String entityId;
	EntityView table;
	List<String> columnIds;
	List<String> scopeIds;
	
	UserInfo userInfo;
	ViewType viewType;
	ViewScope scope;
	
	@Before
	public void setUp() throws Exception {
		provider = new EntityViewMetadataProvider();
		ReflectionTestUtils.setField(provider, "fileViewManager", mockFileViewManager);
		
		columnIds = Lists.newArrayList("42");
		scopeIds = Lists.newArrayList("456");
		viewType = ViewType.file;
		entityId = "syn123";
		table = new EntityView();
		table.setId(entityId);
		table.setColumnIds(columnIds);
		table.setType(ViewType.file);
		table.setScopeIds(scopeIds);
		
		userInfo = new UserInfo(false, 55L);
		
		scope = EntityViewMetadataProvider.createViewScope(table);
	}

	@Test
	public void testCreate(){
		// call under test
		provider.entityCreated(userInfo, table);
		verify(mockFileViewManager).setViewSchemaAndScope(userInfo, columnIds, scope, entityId);
	}
	
	@Test
	public void testUpdate(){
		// call under test
		provider.entityUpdated(userInfo, table);
		verify(mockFileViewManager).setViewSchemaAndScope(userInfo, columnIds, scope, entityId);
	}
	
	@Test
	public void testAddTypeSpecificMetadata(){
		EntityView testEntity = new EntityView();
		testEntity.setId(entityId);
		when(mockFileViewManager.getTableSchema(entityId)).thenReturn(columnIds);
		provider.addTypeSpecificMetadata(testEntity, null, null, null); //the other parameters are not used at all
		verify(mockFileViewManager).getTableSchema(entityId);
		assertEquals(columnIds, testEntity.getColumnIds());
	}
	
	@Test
	public void testCreateViewScopeWithType() {
		ViewScope scope = EntityViewMetadataProvider.createViewScope(table);
		assertNotNull(scope);
		assertEquals(viewType, scope.getViewType());
		assertEquals(scopeIds, scope.getScope());
		assertEquals(null, scope.getViewTypeMask());
	}
	
	@Test
	public void testCreateViewScopeWithMask() {
		Long mask = new Long(0x10);
		table.setViewTypeMask(mask);
		table.setType(null);
		ViewScope scope = EntityViewMetadataProvider.createViewScope(table);
		assertNotNull(scope);
		assertEquals(null, scope.getViewType());
		assertEquals(scopeIds, scope.getScope());
		assertEquals(mask, scope.getViewTypeMask());
	}

}
