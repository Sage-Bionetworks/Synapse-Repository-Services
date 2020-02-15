package org.sagebionetworks.repo.web.service.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Folder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.events.EventsCollector;
import org.sagebionetworks.repo.manager.statistics.StatisticsFileEvent;
import org.sagebionetworks.repo.manager.sts.StsManager;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.UserInfo;

@ExtendWith(MockitoExtension.class)
public class FileEntityMetadataProviderTest {
	private static final String FILE_HANDLE_ID = "file-handle-id";
	private static final String PARENT_ENTITY_ID = "parent-entity-id";

	@Mock
	private EventsCollector mockStatisticsCollector;

	@Mock
	private StsManager mockStsManager;

	@InjectMocks
	private FileEntityMetadataProvider provider;

	private FileEntity fileEntity;
	private UserInfo userInfo;
	private List<EntityHeader> path;

	@BeforeEach
	public void before() {

		fileEntity = new FileEntity();
		fileEntity.setId("syn789");
		fileEntity.setDataFileHandleId(FILE_HANDLE_ID);
		fileEntity.setParentId(PARENT_ENTITY_ID);

		userInfo = new UserInfo(false, 55L);

		// root
		EntityHeader grandparentHeader = new EntityHeader();
		grandparentHeader.setId("123");
		grandparentHeader.setName("gp");
		grandparentHeader.setType(Folder.class.getName());
		path = new ArrayList<>();
		path.add(grandparentHeader);

		// This is our direct parent header
		EntityHeader parentHeader = new EntityHeader();
		parentHeader.setId("456");
		parentHeader.setName("p");
		parentHeader.setType(Folder.class.getName());
		path.add(parentHeader);
	}

	@Test
	public void testValidateCreateWithoutDataFileHandleId() {
		fileEntity.setDataFileHandleId(null);
		Exception ex = assertThrows(IllegalArgumentException.class, () -> provider.validateEntity(fileEntity,
				new EntityEvent(EventType.CREATE, path, userInfo)));
		assertEquals("FileEntity.dataFileHandleId cannot be null", ex.getMessage());
	}

	@Test
	public void testValidateCreateWithFileNameOverride() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			fileEntity.setDataFileHandleId("1");
			fileEntity.setFileNameOverride("fileNameOverride");
			provider.validateEntity(fileEntity, new EntityEvent(EventType.CREATE, path, userInfo));
		});
	}

	@Test
	public void testValidateCreate() {
		// Method under test - Does not throw.
		provider.validateEntity(fileEntity, new EntityEvent(EventType.CREATE, path, userInfo));

		// Validate that we call the STS validator.
		verify(mockStsManager).validateCanAddFile(userInfo, FILE_HANDLE_ID, PARENT_ENTITY_ID);
	}

	@Test
	public void testValidateUpdateWithoutDataFileHandleId() {
		fileEntity.setDataFileHandleId(null);
		Exception ex = assertThrows(IllegalArgumentException.class, () -> provider.validateEntity(fileEntity,
				new EntityEvent(EventType.UPDATE, path, userInfo)));
		assertEquals("FileEntity.dataFileHandleId cannot be null", ex.getMessage());
	}

	@Test
	public void testValidateUpdateWithNullOriginalFileNameOverride() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			fileEntity.setDataFileHandleId("1");
			fileEntity.setFileNameOverride("fileNameOverride");
			provider.validateEntity(fileEntity, new EntityEvent(EventType.UPDATE, path, userInfo));
		});
	}

	@Test
	public void testValidateUpdateWithNewFileNameOverride() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			fileEntity.setDataFileHandleId("1");
			fileEntity.setFileNameOverride("fileNameOverride");
			provider.validateEntity(fileEntity, new EntityEvent(EventType.UPDATE, path, userInfo));
		});
	}

	@Test
	public void testValidateUpdateWithoutFileNameOverride() {
		// Method under test - Does not throw.
		provider.validateEntity(fileEntity, new EntityEvent(EventType.UPDATE, path, userInfo));

		// Validate that we call the STS validator.
		verify(mockStsManager).validateCanAddFile(userInfo, FILE_HANDLE_ID, PARENT_ENTITY_ID);
	}

	@Test
	public void testEntityCreated() {
		fileEntity.setDataFileHandleId("1");
		provider.entityCreated(userInfo, fileEntity);
		verify(mockStatisticsCollector, times(1)).collectEvent(any(StatisticsFileEvent.class));
	}

	@Test
	public void testEntityUpdatedWithNewVersion() {
		fileEntity.setDataFileHandleId("1");
		provider.entityUpdated(userInfo, fileEntity, true);
		verify(mockStatisticsCollector, times(1)).collectEvent(any(StatisticsFileEvent.class));
	}

	@Test
	public void testEntityUpdatedWithoutNewVersion() {
		provider.entityUpdated(userInfo, fileEntity, false);
		verify(mockStatisticsCollector, never()).collectEvent(any());
	}
}
