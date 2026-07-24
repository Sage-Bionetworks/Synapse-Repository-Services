package org.sagebionetworks.repo.service.metadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.sts.StsManager;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.service.metadata.EntityEvent;
import org.sagebionetworks.repo.service.metadata.EventType;
import org.sagebionetworks.repo.service.metadata.FolderMetadataProvider;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;

@ExtendWith(MockitoExtension.class)
public class FolderMetadataProviderTest {
	private static final String FOLDER_ID = "syn123";
	private static final String NEW_PARENT_ID = "syn456";
	private static final String OLD_PARENT_ID = "syn879";
	private static final UserInfo USER_INFO = new UserInfo(false);

	@Mock
	private EntityManager mockEntityManager;

	@Mock
	private StsManager mockStsManager;

	@InjectMocks
	private FolderMetadataProvider provider;

	private EntityEvent event;
	private Folder newFolder;
	private Folder oldFolder;

	@BeforeEach
	public void beforeEach() {
		event = new EntityEvent(EventType.UPDATE, Collections.emptyList(), USER_INFO);

		newFolder = new Folder();
		newFolder.setId(FOLDER_ID);
		newFolder.setParentId(NEW_PARENT_ID);

		oldFolder = new Folder();
		oldFolder.setId(FOLDER_ID);
		oldFolder.setParentId(OLD_PARENT_ID);
	}

	@Test
	public void create() {
		event = new EntityEvent(EventType.CREATE, Collections.emptyList(), USER_INFO);
		// Method under test - Does not call StsManager.
		provider.validateEntity(newFolder, event);
		verifyNoMoreInteractions(mockStsManager);
	}

	@Test
	public void updateWithoutMove() {
		when(mockEntityManager.getEntity(USER_INFO, FOLDER_ID, Folder.class)).thenReturn(oldFolder);
		newFolder.setParentId(OLD_PARENT_ID);
		// Method under test - Does not call StsManager.
		provider.validateEntity(newFolder, event);
		verifyNoMoreInteractions(mockStsManager);
	}

	@Test
	public void updateAndMove() {
		when(mockEntityManager.getEntity(USER_INFO, FOLDER_ID, Folder.class)).thenReturn(oldFolder);
		// Method under test - Does not call StsManager.
		provider.validateEntity(newFolder, event);
		verify(mockStsManager).validateCanMoveFolder(USER_INFO, FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID);
	}
}
