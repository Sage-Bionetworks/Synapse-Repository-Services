package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class TableFileHandleAssociationProviderTest {
	
	@Mock
	private TableEntityManager tableEntityManager;
	
	@InjectMocks
	private TableFileHandleAssociationProvider provider;
	
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
	
	@Test
	public void testGetAssociateType() {
		assertEquals(FileHandleAssociateType.TableEntity, provider.getAssociateType());
	}

}
