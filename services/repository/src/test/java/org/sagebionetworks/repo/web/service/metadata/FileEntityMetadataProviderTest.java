package org.sagebionetworks.repo.web.service.metadata;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Folder;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.test.util.ReflectionTestUtils;

public class FileEntityMetadataProviderTest  {
	@Mock
	EntityManager mockEntityManager;
	
	FileEntityMetadataProvider provider;
	FileEntity fileEntity;
	UserInfo userInfo;
	List<EntityHeader> path;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);

		provider = new FileEntityMetadataProvider();
		ReflectionTestUtils.setField(provider, "manager", mockEntityManager);

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
	public void testCreateWithoutDataFileHandleId(){
		provider.validateEntity(fileEntity, new EntityEvent(EventType.CREATE, path, userInfo));
	}

	@Test (expected=IllegalArgumentException.class)
	public void testCreateWithFileNameOverride(){
		fileEntity.setDataFileHandleId("1");
		fileEntity.setFileNameOverride("fileNameOverride");
		provider.validateEntity(fileEntity, new EntityEvent(EventType.CREATE, path, userInfo));
	}

	@Test
	public void testCreate(){
		fileEntity.setDataFileHandleId("1");
		provider.validateEntity(fileEntity, new EntityEvent(EventType.CREATE, path, userInfo));
	}

	@Test (expected=IllegalArgumentException.class)
	public void testUpdateWithoutDataFileHandleId(){
		provider.validateEntity(fileEntity, new EntityEvent(EventType.UPDATE, path, userInfo));
	}

	@Test (expected=IllegalArgumentException.class)
	public void testUpdateWithNullOriginalFileNameOverride(){
		when(mockEntityManager.getEntity(userInfo, fileEntity.getId())).thenReturn(new FileEntity());
		fileEntity.setDataFileHandleId("1");
		fileEntity.setFileNameOverride("fileNameOverride");
		provider.validateEntity(fileEntity, new EntityEvent(EventType.UPDATE, path, userInfo));
	}

	@Test (expected=IllegalArgumentException.class)
	public void testUpdateWithNewFileNameOverride(){
		FileEntity original = new FileEntity();
		original.setFileNameOverride("originalFileNameOverride");
		when(mockEntityManager.getEntity(userInfo, fileEntity.getId())).thenReturn(original);
		fileEntity.setDataFileHandleId("1");
		fileEntity.setFileNameOverride("fileNameOverride");
		provider.validateEntity(fileEntity, new EntityEvent(EventType.UPDATE, path, userInfo));
	}

	@Test
	public void testUpdateWithoutFileNameOverride(){
		fileEntity.setDataFileHandleId("1");
		provider.validateEntity(fileEntity, new EntityEvent(EventType.UPDATE, path, userInfo));
	}
}
