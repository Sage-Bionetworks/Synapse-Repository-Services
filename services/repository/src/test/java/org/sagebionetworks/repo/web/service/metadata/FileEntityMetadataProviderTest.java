package org.sagebionetworks.repo.web.service.metadata;

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
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.UserInfo;

@ExtendWith(MockitoExtension.class)
public class FileEntityMetadataProviderTest {

	@Mock
	EventsCollector mockStatisticsCollector;

	@InjectMocks
	FileEntityMetadataProvider provider;

	FileEntity fileEntity;
	UserInfo userInfo;
	List<EntityHeader> path;

	@BeforeEach
	public void before() {

		fileEntity = new FileEntity();
		fileEntity.setId("syn789");

		userInfo = new UserInfo(false, 55L);

		// root
		EntityHeader grandparentHeader = new EntityHeader();
		grandparentHeader.setId("123");
		grandparentHeader.setName("gp");
		grandparentHeader.setType(Folder.class.getName());
		path = new ArrayList<EntityHeader>();
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
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			provider.validateEntity(fileEntity, new EntityEvent(EventType.CREATE, path, userInfo));
		});
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
		fileEntity.setDataFileHandleId("1");
		provider.validateEntity(fileEntity, new EntityEvent(EventType.CREATE, path, userInfo));
	}

	@Test
	public void testValidateUpdateWithoutDataFileHandleId() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			provider.validateEntity(fileEntity, new EntityEvent(EventType.UPDATE, path, userInfo));
		});
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
		fileEntity.setDataFileHandleId("1");
		provider.validateEntity(fileEntity, new EntityEvent(EventType.UPDATE, path, userInfo));
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
		boolean wasNewVersionCreated = true;
		provider.entityUpdated(userInfo, fileEntity, wasNewVersionCreated);
		verify(mockStatisticsCollector, times(1)).collectEvent(any(StatisticsFileEvent.class));
	}

	@Test
	public void testEntityUpdatedWithoutNewVersion() {
		boolean wasNewVersionCreated = false;
		provider.entityUpdated(userInfo, fileEntity, wasNewVersionCreated);
		verify(mockStatisticsCollector, never()).collectEvent(any());
	}
}
