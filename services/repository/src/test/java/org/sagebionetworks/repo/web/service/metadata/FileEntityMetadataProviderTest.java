package org.sagebionetworks.repo.web.service.metadata;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Folder;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.manager.statistics.StatisticsEventsCollector;
import org.sagebionetworks.repo.manager.statistics.events.StatisticsFileEvent;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.UserInfo;

public class FileEntityMetadataProviderTest  {
	
	@Mock
	StatisticsEventsCollector mockStatisticsCollector;
	
	@InjectMocks
	FileEntityMetadataProvider provider;
	
	FileEntity fileEntity;
	UserInfo userInfo;
	List<EntityHeader> path;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);

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

	@Test (expected=IllegalArgumentException.class)
	public void testValidateCreateWithoutDataFileHandleId(){
		provider.validateEntity(fileEntity, new EntityEvent(EventType.CREATE, path, userInfo));
	}

	@Test (expected=IllegalArgumentException.class)
	public void testValidateCreateWithFileNameOverride(){
		fileEntity.setDataFileHandleId("1");
		fileEntity.setFileNameOverride("fileNameOverride");
		provider.validateEntity(fileEntity, new EntityEvent(EventType.CREATE, path, userInfo));
	}

	@Test
	public void testValidateCreate(){
		fileEntity.setDataFileHandleId("1");
		provider.validateEntity(fileEntity, new EntityEvent(EventType.CREATE, path, userInfo));
	}

	@Test (expected=IllegalArgumentException.class)
	public void testValidateUpdateWithoutDataFileHandleId(){
		provider.validateEntity(fileEntity, new EntityEvent(EventType.UPDATE, path, userInfo));
	}

	@Test (expected=IllegalArgumentException.class)
	public void testValidateUpdateWithNullOriginalFileNameOverride(){
		fileEntity.setDataFileHandleId("1");
		fileEntity.setFileNameOverride("fileNameOverride");
		provider.validateEntity(fileEntity, new EntityEvent(EventType.UPDATE, path, userInfo));
	}

	@Test (expected=IllegalArgumentException.class)
	public void testValidateUpdateWithNewFileNameOverride(){
		fileEntity.setDataFileHandleId("1");
		fileEntity.setFileNameOverride("fileNameOverride");
		provider.validateEntity(fileEntity, new EntityEvent(EventType.UPDATE, path, userInfo));
	}

	@Test
	public void testValidateUpdateWithoutFileNameOverride(){
		fileEntity.setDataFileHandleId("1");
		provider.validateEntity(fileEntity, new EntityEvent(EventType.UPDATE, path, userInfo));
	}
	
	@Test
	public void testEntityCreated() {
		provider.entityCreated(userInfo, fileEntity);
		verify(mockStatisticsCollector, times(1)).collectEvent(any(StatisticsFileEvent.class));
	}
	
	@Test
	public void testEntityUpdatedWithNewVersion() {
		provider.entityUpdated(userInfo, fileEntity, true);
		verify(mockStatisticsCollector, times(1)).collectEvent(any(StatisticsFileEvent.class));
	}
	
	@Test
	public void testEntityUpdatedWithoutNewVersion() {
		provider.entityUpdated(userInfo, fileEntity, false);
		verify(mockStatisticsCollector, never()).collectEvent(any());
	}
}
