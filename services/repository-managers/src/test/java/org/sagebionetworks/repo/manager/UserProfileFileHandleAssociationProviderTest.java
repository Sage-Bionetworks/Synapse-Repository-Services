package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
public class UserProfileFileHandleAssociationProviderTest {

	@Mock
	private UserProfileDAO mockUserProfileDAO;

	@Mock
	private FileHandleDao mockFileHandleDao;

	@Mock
	private JdbcTemplate mockJdbcTemplate;

	@Mock
	private NamedParameterJdbcTemplate mockNamedJdbcTemplate;

	@InjectMocks
	private UserProfileFileHandleAssociationProvider provider;

	@Test
	public void testGetFileHandleIdsAssociatedWithObject() {
		String userId = "1";
		String fileHandleId = "2";
		String previewFileHandleId = "3";
		when(mockUserProfileDAO.getPictureFileHandleId(userId)).thenReturn(fileHandleId);
		when(mockFileHandleDao.getPreviewFileHandleId(fileHandleId)).thenReturn(previewFileHandleId);
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(Arrays.asList(previewFileHandleId, "4"), userId);
		assertEquals(Collections.singleton(previewFileHandleId), associated);
	}

	@Test
	public void testGetObjectTypeForAssociatedType() {
		assertEquals(ObjectType.USER_PROFILE, provider.getAuthorizationObjectTypeForAssociatedObjectType());
	}

	@Test
	public void testGetAssociateType() {
		assertEquals(FileHandleAssociateType.UserProfileAttachment, provider.getAssociateType());
	}

}
