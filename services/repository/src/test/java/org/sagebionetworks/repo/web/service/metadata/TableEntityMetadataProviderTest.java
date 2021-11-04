package org.sagebionetworks.repo.web.service.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.TableEntity;

import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
public class TableEntityMetadataProviderTest  {
	
	@Mock
	private NodeManager mockNodeManager;
	
	@Mock
	private TableEntityManager tableEntityManager;
	
	@InjectMocks
	private TableEntityMetadataProvider provider;
	
	String entityId;
	IdAndVersion idAndVersion;
	TableEntity table;
	List<String> columnIds;
	
	UserInfo userInfo;	
	EntityEvent event;
	
	@BeforeEach
	public void before() {
		
		columnIds = Lists.newArrayList("123");
		
		entityId = "syn123";
		idAndVersion = IdAndVersion.parse(entityId);
		table = new TableEntity();
		table.setId(entityId);
		table.setColumnIds(columnIds);
		
		userInfo = new UserInfo(false, 55L);
		event = new EntityEvent();
		event.setType(EventType.CREATE);
	}
	
	@Test
	public void testDeleteEntity(){
		// call under test
		provider.entityDeleted(entityId);
		verify(tableEntityManager).setTableAsDeleted(entityId);
	}
	
	@Test
	public void testCreate(){
		// call under test
		provider.entityCreated(userInfo, table);
		verify(tableEntityManager).tableUpdated(userInfo, columnIds, entityId, null);
		verifyNoMoreInteractions(tableEntityManager);
	}
	
	@Test
	public void testCreateWithSearchEnabled(){
		// call under test
		table.setIsSearchEnabled(true);
		provider.entityCreated(userInfo, table);
		verify(tableEntityManager).tableUpdated(userInfo, columnIds, entityId, true);
	}
	
	@Test
	public void testUpdateNoNewVersion(){
		boolean wasNewVersionCreated = false;
		// call under test
		provider.entityUpdated(userInfo, table, wasNewVersionCreated);
		verify(tableEntityManager).tableUpdated(userInfo, columnIds, entityId, null);
	}
	
	@Test
	public void testUpdateNoNewVersionWithSearchEnabled(){
		boolean wasNewVersionCreated = false;
		table.setIsSearchEnabled(true);
		// call under test
		provider.entityUpdated(userInfo, table, wasNewVersionCreated);
		verify(tableEntityManager).tableUpdated(userInfo, columnIds, entityId, true);
	}
	
	@Test
	public void testUpdateWithNewVersion(){
		boolean wasNewVersionCreated = true;
		assertThrows(IllegalArgumentException.class, () -> {			
			// call under test
			provider.entityUpdated(userInfo, table, wasNewVersionCreated);
		});
		
		verifyZeroInteractions(tableEntityManager);
	}
	
	@Test
	public void testAddTypeSpecificMetadata(){
		TableEntity testEntity = new TableEntity();
		testEntity.setId(entityId);
		testEntity.setVersionNumber(33L);
		IdAndVersion expectedId = IdAndVersion.parse("syn123.33");
		when(tableEntityManager.getTableSchema(expectedId)).thenReturn(columnIds);
		provider.addTypeSpecificMetadata(testEntity, null, null); //the other parameters are not used at all
		verify(tableEntityManager).getTableSchema(expectedId);
		assertEquals(columnIds, testEntity.getColumnIds());
	}
	
	@Test
	public void testValidateEntityWithNullVersionLabel() {
		table.setVersionLabel(null);
		
		// Call under test
		provider.validateEntity(table, event);
		
		assertEquals(TableConstants.IN_PROGRESS, table.getVersionLabel());
	}
	
	@Test
	public void testValidateEntityWithNullVersionComment() {
		table.setVersionComment(null);
		
		// Call under test
		provider.validateEntity(table, event);
		
		assertEquals(TableConstants.IN_PROGRESS, table.getVersionComment());
	}

	@Test
	public void testValidateEntityForCreateWithNullSearchEnabled() {
		table.setIsSearchEnabled(null);
		
		event.setType(EventType.CREATE);
		
		// Call under test
		provider.validateEntity(table, event);

		assertNull(table.getIsSearchEnabled());
	}
	
	@Test
	public void testValidateEntityForUpdateWithNullSearchEnabled() {
		table.setIsSearchEnabled(null);
		event.setType(EventType.UPDATE);
		
		Node node = new Node()
			.setIsSearchEnabled(true);
		
		when(mockNodeManager.getNode(anyString())).thenReturn(node);
		
		// Call under test
		provider.validateEntity(table, event);

		assertTrue(table.getIsSearchEnabled());
	}
	
}
