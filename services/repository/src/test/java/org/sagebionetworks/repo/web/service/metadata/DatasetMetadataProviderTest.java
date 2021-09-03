package org.sagebionetworks.repo.web.service.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.Dataset;
import org.sagebionetworks.repo.model.table.DatasetItem;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewScope;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class DatasetMetadataProviderTest {

	@Mock
	private NodeDAO mockNodeDao;

	@Mock
	private TableViewManager mockTableViewManger;

	@InjectMocks
	private DatasetMetadataProvider provider;

	private Dataset dataset;
	private EntityEvent event;
	private UserInfo user;

	@BeforeEach
	public void before() {
		dataset = new Dataset().setItems(Lists.newArrayList(new DatasetItem().setEntityId("syn111").setVersionNumber(2L),
				new DatasetItem().setEntityId("syn222").setVersionNumber(4L)));
		event = new EntityEvent();
		user = new UserInfo(false, 44L);
	}

	@Test
	public void testValidateEntity() {
		String fileType = FileEntity.class.getName();
		List<EntityHeader> header = Arrays.asList(new EntityHeader().setId("syn111").setType(fileType),
				new EntityHeader().setId("syn222").setType(fileType));
		when(mockNodeDao.getEntityHeader(anySet())).thenReturn(header);
		// call under test
		provider.validateEntity(dataset, event);

		verify(mockNodeDao).getEntityHeader(Sets.newHashSet(111L, 222L));
	}

	@Test
	public void testValidateEntityWithNullItems() {
		dataset.setItems(null);
		// call under test
		provider.validateEntity(dataset, event);

		verify(mockNodeDao, never()).getEntityHeader(anySet());
	}

	@Test
	public void testValidateEntityWithNonFile() {
		String fileType = FileEntity.class.getName();
		String folderType = Folder.class.getName();
		List<EntityHeader> header = Arrays.asList(new EntityHeader().setId("syn111").setType(fileType),
				new EntityHeader().setId("syn222").setType(folderType));
		when(mockNodeDao.getEntityHeader(anySet())).thenReturn(header);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			provider.validateEntity(dataset, event);
		}).getMessage();

		assertEquals(
				"Currently, only files can be included in a dataset. syn222 is 'org.sagebionetworks.repo.model.Folder'",
				message);

		verify(mockNodeDao).getEntityHeader(Sets.newHashSet(111L, 222L));
	}
	
	@Test
	public void testValidateEntityWithNullEntityId() {
		dataset.getItems().get(0).setEntityId(null);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			provider.validateEntity(dataset, event);
		}).getMessage();

		assertEquals("Each dataset item must have a non-null entity ID.", message);

		verify(mockNodeDao, never()).getEntityHeader(anySet());
	}
	
	@Test
	public void testValidateEntityWithNullVersion() {
		dataset.getItems().get(0).setVersionNumber(null);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			provider.validateEntity(dataset, event);
		}).getMessage();

		assertEquals("Each dataset item must have a non-null version number", message);

		verify(mockNodeDao, never()).getEntityHeader(anySet());
	}
	
	@Test
	public void testValidateEntityWithDuplicateEntityId() {
		dataset.getItems().add(new DatasetItem().setEntityId("syn111").setVersionNumber(3L));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			provider.validateEntity(dataset, event);
		}).getMessage();

		assertEquals("Each dataset item must have a unique entity ID.  Duplicate: syn111", message);

		verify(mockNodeDao, never()).getEntityHeader(anySet());
	}
	
	@Test
	public void testValidateEntityWithDuplicateEntityIdNoSyn() {
		dataset.getItems().add(new DatasetItem().setEntityId("111").setVersionNumber(3L));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			provider.validateEntity(dataset, event);
		}).getMessage();

		assertEquals("Each dataset item must have a unique entity ID.  Duplicate: 111", message);

		verify(mockNodeDao, never()).getEntityHeader(anySet());
	}

	@Test
	public void testCreateViewScope() {
		// call under test
		ViewScope scope = provider.createViewScope(user, dataset);
		ViewScope expected = new ViewScope().setScope(Arrays.asList("syn111", "syn222"))
				.setViewEntityType(ViewEntityType.dataset).setViewTypeMask(0L);
		assertEquals(expected, scope);
	}

	@Test
	public void testCreateViewScopeWithNullItems() {
		dataset.setItems(null);
		// call under test
		ViewScope scope = provider.createViewScope(user, dataset);
		ViewScope expected = new ViewScope().setScope(null).setViewEntityType(ViewEntityType.dataset)
				.setViewTypeMask(0L);
		assertEquals(expected, scope);
	}
}
