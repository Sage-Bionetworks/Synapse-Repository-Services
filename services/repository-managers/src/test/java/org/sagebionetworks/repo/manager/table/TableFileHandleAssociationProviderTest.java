package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.ObjectType;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class TableFileHandleAssociationProviderTest {
	
	TableFileHandleAssociationProvider provider;
	TableEntityManager tableEntityManager;
	
	@Before
	public void before(){
		tableEntityManager = Mockito.mock(TableEntityManager.class);
		provider = new TableFileHandleAssociationProvider();
		ReflectionTestUtils.setField(provider, "tableEntityManager", tableEntityManager);
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
		when(tableEntityManager.getFileHandleIdsAssociatedWithTable(tableId, fileHandleIds)).thenReturn(results);
		//call under test
		Set<String> out = provider.getFileHandleIdsDirectlyAssociatedWithObject(fileHandleIds, tableId);
		assertEquals(results, out);
	}

}
