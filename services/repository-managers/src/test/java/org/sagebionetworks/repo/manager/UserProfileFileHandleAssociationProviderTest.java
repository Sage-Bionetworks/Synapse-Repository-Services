package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.springframework.test.util.ReflectionTestUtils;

public class UserProfileFileHandleAssociationProviderTest {

	@Mock
	private UserProfileDAO mockUserProfileDAO;
	@Mock
	private FileHandleManager mockFileHandleManager;
	private UserProfileFileHandleAssociationProvider provider;
	
	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		provider = new UserProfileFileHandleAssociationProvider();
		ReflectionTestUtils.setField(provider, "userProfileDAO", mockUserProfileDAO);
		ReflectionTestUtils.setField(provider, "fileHandleManager", mockFileHandleManager);
	}

	@Test
	public void testGetFileHandleIdsAssociatedWithObject() {
		String userId = "1";
		String fileHandleId = "2";
		String previewFileHandleId = "3";
		when(mockUserProfileDAO.getPictureFileHandleId(userId ))
				.thenReturn(fileHandleId);
		when(mockFileHandleManager.getPreviewFileHandleId(fileHandleId))
				.thenReturn(previewFileHandleId);
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(
				Arrays.asList(previewFileHandleId, "4"), userId);
		assertEquals(Collections.singleton(previewFileHandleId), associated);
	}

	@Test
	public void testGetObjectTypeForAssociatedType() {
		assertEquals(ObjectType.USER_PROFILE, provider.getAuthorizationObjectTypeForAssociatedObjectType());
	}
}
