package org.sagebionetworks.repo.web.service.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityRef;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.table.Dataset;
import org.sagebionetworks.repo.model.table.DatasetCollection;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.repo.model.table.ViewTypeMask;

@ExtendWith(MockitoExtension.class)
public class DatasetCollectionMetadataProviderTest {

	@Mock
	private NodeDAO mockNodeDao;

	@Mock
	private TableViewManager mockTableViewManger;

	@InjectMocks
	private DatasetCollectionMetadataProvider provider;

	private DatasetCollection datasetCollection;
	private EntityEvent event;

	@BeforeEach
	public void before() {
		datasetCollection = new DatasetCollection()
			.setItems(List.of(
				new EntityRef().setEntityId("syn111").setVersionNumber(2L),
				new EntityRef().setEntityId("syn222").setVersionNumber(4L)
			))
			.setColumnIds(List.of("1", "2", "3"));
		event = new EntityEvent();
	}

	@Test
	public void testValidateEntity() {
		String datasetType = Dataset.class.getName();
		
		List<EntityHeader> header = List.of(
			new EntityHeader().setId("syn111").setType(datasetType),
			new EntityHeader().setId("syn222").setType(datasetType)
		);
		
		when(mockNodeDao.getEntityHeader(anySet())).thenReturn(header);
		
		// call under test
		provider.validateEntity(datasetCollection, event);

		verify(mockTableViewManger).validateViewSchemaAndScope(
			List.of("1", "2", "3"), 
			new ViewScope().setScope(Arrays.asList("syn111", "syn222"))
				.setViewEntityType(ViewEntityType.datasetcollection)
				.setViewTypeMask(ViewTypeMask.Dataset.getMask())
		);
		
		verify(mockNodeDao).getEntityHeader(Set.of(111L, 222L));
	}

	@Test
	public void testValidateEntityWithNullItems() {
		datasetCollection.setItems(null);
		// call under test
		provider.validateEntity(datasetCollection, event);

		verify(mockNodeDao, never()).getEntityHeader(anySet());
	}

	@Test
	public void testValidateEntityWithNonDataset() {
		String datasetType = Dataset.class.getName();
		String folderType = Folder.class.getName();
		
		List<EntityHeader> header = List.of(
			new EntityHeader().setId("syn111").setType(datasetType),
			new EntityHeader().setId("syn222").setType(folderType)
		);
		
		when(mockNodeDao.getEntityHeader(anySet())).thenReturn(header);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			provider.validateEntity(datasetCollection, event);
		}).getMessage();

		assertEquals("Only dataset entities can be included in a dataset collection. syn222 is 'org.sagebionetworks.repo.model.Folder'", message);

		verify(mockNodeDao).getEntityHeader(Set.of(111L, 222L));
	}
	
	@Test
	public void testValidateEntityWithNullEntityId() {
		datasetCollection.getItems().get(0).setEntityId(null);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			provider.validateEntity(datasetCollection, event);
		}).getMessage();

		assertEquals("Each dataset collection item must have a non-null entity ID.", message);

		verify(mockNodeDao, never()).getEntityHeader(anySet());
	}
	
	@Test
	public void testValidateEntityWithNullVersion() {
		datasetCollection.getItems().get(0).setVersionNumber(null);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			provider.validateEntity(datasetCollection, event);
		}).getMessage();

		assertEquals("Each dataset collection item must have a non-null version number", message);

		verify(mockNodeDao, never()).getEntityHeader(anySet());
	}
	
	@Test
	public void testValidateEntityWithDuplicateEntityId() {
		List<EntityRef> items = new ArrayList<>(datasetCollection.getItems());
		
		items.add(new EntityRef().setEntityId("syn111").setVersionNumber(3L));
		
		datasetCollection.setItems(items);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			provider.validateEntity(datasetCollection, event);
		}).getMessage();

		assertEquals("Each dataset collection item must have a unique entity ID.  Duplicate: syn111", message);

		verify(mockNodeDao, never()).getEntityHeader(anySet());
	}
	
	@Test
	public void testValidateEntityWithDuplicateEntityIdNoSyn() {
		List<EntityRef> items = new ArrayList<>(datasetCollection.getItems());
		
		items.add(new EntityRef().setEntityId("111").setVersionNumber(3L));
		
		datasetCollection.setItems(items);
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			provider.validateEntity(datasetCollection, event);
		}).getMessage();

		assertEquals("Each dataset collection item must have a unique entity ID.  Duplicate: 111", message);

		verify(mockNodeDao, never()).getEntityHeader(anySet());
	}

	@Test
	public void testCreateViewScope() {
		// call under test
		ViewScope scope = provider.createViewScope(datasetCollection);
		ViewScope expected = new ViewScope().setScope(Arrays.asList("syn111", "syn222"))
				.setViewEntityType(ViewEntityType.datasetcollection).setViewTypeMask(ViewTypeMask.Dataset.getMask());
		assertEquals(expected, scope);
	}

	@Test
	public void testCreateViewScopeWithNullItems() {
		datasetCollection.setItems(null);
		// call under test
		ViewScope scope = provider.createViewScope(datasetCollection);
		ViewScope expected = new ViewScope().setScope(null).setViewEntityType(ViewEntityType.datasetcollection)
				.setViewTypeMask(ViewTypeMask.Dataset.getMask());
		assertEquals(expected, scope);
	}
}
