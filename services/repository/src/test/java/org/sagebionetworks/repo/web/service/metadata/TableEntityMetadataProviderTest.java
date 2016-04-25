package org.sagebionetworks.repo.web.service.metadata;

import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;

public class TableEntityMetadataProviderTest  {
	
	@Mock
	TableRowManager tableRowManager;
	
	TableEntityMetadataProvider provider;
	
	String entityId;
	TableEntity table;
	List<String> columnIds;
	
	UserInfo userInfo;	
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		
		provider = new TableEntityMetadataProvider();
		ReflectionTestUtils.setField(provider, "tableRowManager", tableRowManager);
		
		columnIds = Lists.newArrayList("123");
		
		entityId = "syn123";
		table = new TableEntity();
		table.setId(entityId);
		table.setColumnIds(columnIds);
		
		userInfo = new UserInfo(false, 55L);
	}
	
	@Test
	public void testDeleteEntity(){
		// call under test
		provider.entityDeleted(entityId);
		verify(tableRowManager).deleteTable(entityId);
	}
	
	@Test
	public void testCreate(){
		// call under test
		provider.entityCreated(userInfo, table);
		verify(tableRowManager).setTableSchema(userInfo, columnIds, entityId);
	}
	
	@Test
	public void testUpdate(){
		// call under test
		provider.entityUpdated(userInfo, table);
		verify(tableRowManager).setTableSchema(userInfo, columnIds, entityId);
	}

}
