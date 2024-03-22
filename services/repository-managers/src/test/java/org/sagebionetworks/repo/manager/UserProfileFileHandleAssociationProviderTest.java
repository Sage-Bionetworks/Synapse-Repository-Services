package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.web.NotFoundException;
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
		when(mockFileHandleDao.getPreviewFileHandleId(fileHandleId)).thenReturn(Optional.of(previewFileHandleId));
		
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(List.of(previewFileHandleId, "4"), userId);
		
		assertEquals(Collections.singleton(previewFileHandleId), associated);
	}
	
	@Test
	public void testGetFileHandleIdsAssociatedWithObjectWithMissingPreview() {
		String userId = "1";
		String fileHandleId = "2";
		
		when(mockUserProfileDAO.getPictureFileHandleId(userId)).thenReturn(fileHandleId);
		when(mockFileHandleDao.getPreviewFileHandleId(fileHandleId)).thenReturn(Optional.empty());
		
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(List.of(fileHandleId), userId);
		
		assertEquals(Collections.singleton(fileHandleId), associated);
	}
	
	@Test
	public void testGetFileHandleIdsAssociatedWithObjectWithFileNotFound() {
		String userId = "1";
		String fileHandleId = "2";
		
		when(mockUserProfileDAO.getPictureFileHandleId(userId)).thenThrow(NotFoundException.class);
		
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(List.of(fileHandleId), userId);
		
		assertEquals(Collections.emptySet(), associated);
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
