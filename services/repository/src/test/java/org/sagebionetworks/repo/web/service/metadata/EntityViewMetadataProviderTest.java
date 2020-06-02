package org.sagebionetworks.repo.web.service.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.repo.model.table.ViewType;

import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
public class EntityViewMetadataProviderTest {
	
	@Mock
	private TableViewManager mockFileViewManager;
	
	@InjectMocks
	private EntityViewMetadataProvider provider;
	
	private String entityId;
	private EntityView table;
	private List<String> columnIds;
	private List<String> scopeIds;
	
	private UserInfo userInfo;
	private ViewType viewType;
	private ViewScope scope;
	
	@BeforeEach
	public void setUp() throws Exception {
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
		
		scope = provider.createViewScope(userInfo, table);
	}

	@Test
	public void testCreate(){
		// call under test
		provider.entityCreated(userInfo, table);
		verify(mockFileViewManager).setViewSchemaAndScope(userInfo, columnIds, scope, entityId);
	}
	
	@Test
	public void testUpdate(){
		boolean wasNewVersionCreated = false;
		// call under test
		provider.entityUpdated(userInfo, table, wasNewVersionCreated);
		verify(mockFileViewManager).setViewSchemaAndScope(userInfo, columnIds, scope, entityId);
	}
	
	@Test
	public void testAddTypeSpecificMetadata(){
		EntityView testEntity = new EntityView();
		testEntity.setId(entityId);
		testEntity.setVersionNumber(11L);
		IdAndVersion idAndVersion = KeyFactory.idAndVersion(testEntity.getId(), testEntity.getVersionNumber());
		when(mockFileViewManager.getViewSchemaIds(idAndVersion)).thenReturn(columnIds);
		provider.addTypeSpecificMetadata(testEntity, null, null); //the other parameters are not used at all
		verify(mockFileViewManager).getViewSchemaIds(idAndVersion);
		assertEquals(columnIds, testEntity.getColumnIds());
	}
	
	@Test
	public void testCreateViewScopeWithType() {
		ViewScope scope = provider.createViewScope(userInfo, table);
		assertNotNull(scope);
		assertEquals(viewType, scope.getViewType());
		assertEquals(scopeIds, scope.getScope());
		assertEquals(null, scope.getViewTypeMask());
		assertEquals(ViewEntityType.entityview, scope.getViewEntityType());
	}
	
	@Test
	public void testCreateViewScopeWithMask() {
		Long mask = new Long(0x10);
		table.setViewTypeMask(mask);
		table.setType(null);
		ViewScope scope = provider.createViewScope(userInfo, table);
		assertNotNull(scope);
		assertEquals(null, scope.getViewType());
		assertEquals(scopeIds, scope.getScope());
		assertEquals(mask, scope.getViewTypeMask());
		assertEquals(ViewEntityType.entityview, scope.getViewEntityType());
	}
	
	@Test
	public void testUpdateViewWithNewVersion() {
		boolean wasNewVersionCreated = true;
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			provider.entityUpdated(userInfo, table, wasNewVersionCreated);
		});
		
		assertEquals("A view version can only be created by creating a view snapshot.", ex.getMessage());
	}

}
