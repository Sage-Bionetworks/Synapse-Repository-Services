package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.ObjectType;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class TableFileHandleAssociationProviderTest {
	
	TableFileHandleAssociationProvider provider;
	TableRowManager mockTableRowManger;
	
	@Before
	public void before(){
		mockTableRowManger = Mockito.mock(TableRowManager.class);
		provider = new TableFileHandleAssociationProvider();
		ReflectionTestUtils.setField(provider, "tableRowManger", mockTableRowManger);
	}
	
	@Test
	public void testGetAuthorizationObjectTypeForAssociatedObjectType(){
		assertEquals(ObjectType.ENTITY, provider.getAuthorizationObjectTypeForAssociatedObjectType());
	}
	
	@Test
	public void testGetFileHandleIdsAssociatedWithObject(){
		List<String> fileHandleIds = Lists.newArrayList("1","2");
		String tableId = "123";
		Set<String> results = Sets.newHashSet("2");
		when(mockTableRowManger.getFileHandleIdsAssociatedWithTable(tableId, fileHandleIds)).thenReturn(results);
		//call under test
		Set<String> out = provider.getFileHandleIdsAssociatedWithObject(fileHandleIds, tableId);
		assertEquals(results, out);
	}

}
