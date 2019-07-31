package org.sagebionetworks.repo.web.service.metadata;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;

public class TableEntityMetadataProviderTest  {
	
	@Mock
	TableEntityManager tableEntityManager;
	
	TableEntityMetadataProvider provider;
	
	String entityId;
	IdAndVersion idAndVersion;
	TableEntity table;
	List<String> columnIds;
	
	UserInfo userInfo;	
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		
		provider = new TableEntityMetadataProvider();
		ReflectionTestUtils.setField(provider, "tableEntityManager", tableEntityManager);
		
		columnIds = Lists.newArrayList("123");
		
		entityId = "syn123";
		idAndVersion = IdAndVersion.parse(entityId);
		table = new TableEntity();
		table.setId(entityId);
		table.setColumnIds(columnIds);
		
		userInfo = new UserInfo(false, 55L);
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
		verify(tableEntityManager).setTableSchema(userInfo, columnIds, entityId);
	}
	
	@Test
	public void testUpdateNoNewVersion(){
		boolean wasNewVersionCreated = false;
		// call under test
		provider.entityUpdated(userInfo, table, wasNewVersionCreated);
		verify(tableEntityManager).setTableSchema(userInfo, columnIds, entityId);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateWithNewVersion(){
		boolean wasNewVersionCreated = true;
		// call under test
		provider.entityUpdated(userInfo, table, wasNewVersionCreated);
		verify(tableEntityManager).setTableSchema(userInfo, columnIds, entityId);
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

}
