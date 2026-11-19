package org.sagebionetworks.repo.manager.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_REALM_ID;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class FileHandleAuthorizationManagerImplTest {

	@Mock
	private FileHandleDao fileHandleDao;

	@InjectMocks
	private FileHandleAuthorizationManagerImpl manager;

	private UserInfo userInfo;
	private UserInfo adminUser;
	private FileHandle owned;
	private FileHandle notOwnedOne;
	private FileHandle notOwnedTwo;

	@BeforeEach
	public void before() {
		userInfo = new UserInfo(false, 123L, DEFAULT_REALM_ID);
		adminUser = new UserInfo(true, 455L, DEFAULT_REALM_ID);
		owned = handle("1", "123");
		notOwnedOne = handle("2", "999");
		notOwnedTwo = handle("3", "999");
	}

	private static FileHandle handle(String id, String createdBy) {
		FileHandle handle = new S3FileHandle();
		handle.setId(id);
		handle.setCreatedBy(createdBy);
		return handle;
	}

	private String denied(String ids) {
		return String.format(AuthorizationUtils.FILE_HANDLE_UNAUTHORIZED_TEMPLATE, ids, userInfo.getId().toString());
	}

	@Test
	public void testCanAccessRawFileHandleByIdWithCreator() {
		when(fileHandleDao.getFileHandlesOrThrow(List.of("1"))).thenReturn(Map.of("1", owned));
		// call under test
		AuthorizationStatus status = manager.canAccessRawFileHandleById(userInfo, "1");
		assertEquals(AuthorizationStatus.authorized(), status);
	}

	@Test
	public void testCanAccessRawFileHandleByIdWithNonCreator() {
		when(fileHandleDao.getFileHandlesOrThrow(List.of("2"))).thenReturn(Map.of("2", notOwnedOne));
		// call under test
		AuthorizationStatus status = manager.canAccessRawFileHandleById(userInfo, "2");
		assertEquals(AuthorizationStatus.accessDenied(
				"Only the creator of a FileHandle can access it directly by its ID.  FileHandleId = '2', UserId = '123'"), status);
	}

	@Test
	public void testCanAccessRawFileHandleByIdWithAdmin() {
		when(fileHandleDao.getFileHandlesOrThrow(List.of("2"))).thenReturn(Map.of("2", notOwnedOne));
		// call under test
		AuthorizationStatus status = manager.canAccessRawFileHandleById(adminUser, "2");
		assertEquals(AuthorizationStatus.authorized(), status);
	}

	@Test
	public void testCanAccessRawFileHandleByIdWithAdminAndMissingFile() {
		when(fileHandleDao.getFileHandlesOrThrow(List.of("2")))
				.thenThrow(new NotFoundException("The FileHandle does not exist: 2"));
		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			manager.canAccessRawFileHandleById(adminUser, "2");
		}).getMessage();
		assertEquals("The FileHandle does not exist: 2", message);
	}

	@Test
	public void testCanAccessRawFileHandlesByIdsWithEmpty() {
		// call under test
		AuthorizationStatus status = manager.canAccessRawFileHandlesByIds(userInfo, Collections.emptyList());
		assertEquals(AuthorizationStatus.authorized(), status);
		verifyNoInteractions(fileHandleDao);
	}

	@Test
	public void testCanAccessRawFileHandlesByIdsWithMultipleDenied() {
		List<String> ids = List.of("3", "1", "2", "3");
		when(fileHandleDao.getFileHandlesOrThrow(ids))
				.thenReturn(Map.of("1", owned, "2", notOwnedOne, "3", notOwnedTwo));
		// call under test
		AuthorizationStatus status = manager.canAccessRawFileHandlesByIds(userInfo, ids);
		assertEquals(AuthorizationStatus.accessDenied(denied("3, 2")), status);
		verify(fileHandleDao).getFileHandlesOrThrow(ids);
	}

	@Test
	public void testCanAccessRawFileHandlesWithOwned() {
		// call under test
		AuthorizationStatus status = manager.canAccessRawFileHandles(userInfo, List.of(owned));
		assertEquals(AuthorizationStatus.authorized(), status);
		verifyNoInteractions(fileHandleDao);
	}

	@Test
	public void testCanAccessRawFileHandlesWithNotOwned() {
		// call under test
		AuthorizationStatus status = manager.canAccessRawFileHandles(userInfo, List.of(notOwnedOne, owned, notOwnedTwo));
		assertEquals(AuthorizationStatus.accessDenied(denied("2, 3")), status);
		verifyNoInteractions(fileHandleDao);
	}
}
