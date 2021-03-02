package org.sagebionetworks.repo.manager.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
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
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;

@ExtendWith(MockitoExtension.class)
public class FileHandleAuthorizationManagerImplTest {

	@Mock
	private AuthorizationManager mockAuthManager;
	
	@InjectMocks
	private FileHandleAuthorizationManagerImpl manager;
	
	private UserInfo user;

	private FileHandleAssociation fha1;
	private FileHandleAssociation fha2;
	private FileHandleAssociation fha3;

	@BeforeEach
	public void before() {
		user = new UserInfo(false);
		user.setId(7L);

		fha1 = new FileHandleAssociation();
		fha1.setFileHandleId("1");
		fha1.setAssociateObjectId("123");
		fha1.setAssociateObjectType(FileHandleAssociateType.TableEntity);

		fha2 = new FileHandleAssociation();
		fha2.setFileHandleId("1");
		fha2.setAssociateObjectId("123");
		fha2.setAssociateObjectType(FileHandleAssociateType.FileEntity);

		fha3 = new FileHandleAssociation();
		fha3.setFileHandleId("2");
		fha3.setAssociateObjectId("123");
		fha3.setAssociateObjectType(FileHandleAssociateType.FileEntity);
	}

	@Test
	public void testCanDownloadSameFileDifferntAssociateObject() {
		List<String> fileHandleIds = Arrays.asList(fha1.getFileHandleId());
		// can download 1
		when(
				mockAuthManager.canDownloadFile(user, fileHandleIds,
						fha1.getAssociateObjectId(),
						fha1.getAssociateObjectType())).thenReturn(
				Arrays.asList(new FileHandleAuthorizationStatus(fha1
						.getFileHandleId(), AuthorizationStatus.authorized())));
		// cannot download 2
		when(
				mockAuthManager.canDownloadFile(user, fileHandleIds,
						fha2.getAssociateObjectId(),
						fha2.getAssociateObjectType())).thenReturn(
				Arrays.asList(new FileHandleAuthorizationStatus(fha2
						.getFileHandleId(), AuthorizationStatus.accessDenied(""))));
		// call under test.
		List<FileHandleAssociationAuthorizationStatus> resutls = manager
				.canDownLoadFile(user, Arrays.asList(fha1, fha2));
		// 1 authorized and 2 denied.
		List<FileHandleAssociationAuthorizationStatus> expected = Arrays
				.asList(new FileHandleAssociationAuthorizationStatus(fha1,
						AuthorizationStatus.authorized()),
						new FileHandleAssociationAuthorizationStatus(fha2,
								AuthorizationStatus.accessDenied("")));
		assertEquals(expected, resutls);
		// auth manager should be called once for each object.
		verify(mockAuthManager, times(2)).canDownloadFile(any(UserInfo.class),
				anyList(), anyString(), any(FileHandleAssociateType.class));
	}
	
	@Test
	public void testCanDownloadDifferentFilesSameObject() {
		List<String> fileHandleIds = Arrays.asList(fha2.getFileHandleId(), fha3.getFileHandleId());
		// 2 and 3 have the same associate object. Allow 3 and deny 2.
		when(
				mockAuthManager.canDownloadFile(user, fileHandleIds,
						fha2.getAssociateObjectId(),
						fha2.getAssociateObjectType())).thenReturn(
				Arrays.asList(
						new FileHandleAuthorizationStatus(fha2.getFileHandleId(), AuthorizationStatus.accessDenied("")),
						new FileHandleAuthorizationStatus(fha3.getFileHandleId(), AuthorizationStatus.authorized())));

		// call under test.
		List<FileHandleAssociationAuthorizationStatus> resutls = manager
				.canDownLoadFile(user, Arrays.asList(fha2, fha3));
		// 3 authorized and 2 denied.
		List<FileHandleAssociationAuthorizationStatus> expected = Arrays
				.asList(new FileHandleAssociationAuthorizationStatus(fha2,
						AuthorizationStatus.accessDenied("")),
						new FileHandleAssociationAuthorizationStatus(fha3,
								AuthorizationStatus.authorized()));
		assertEquals(expected, resutls);
		// auth manager should be called once for this case.
		verify(mockAuthManager, times(1)).canDownloadFile(any(UserInfo.class),
				anyList(), anyString(), any(FileHandleAssociateType.class));
	}
	
	@Test
	public void testValidateNullList(){
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.canDownLoadFile(user, null);
		});
	}
	
	@Test
	public void testValidateNulItem(){
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.canDownLoadFile(user, Arrays.asList(fha2, null));
		});
	}
	
	@Test
	public void testValidateObjectIdNull(){
		fha1.setAssociateObjectId(null);
		
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.canDownLoadFile(user, Arrays.asList(fha1));
		});
	}
	
	@Test
	public void testValidateObjectTypeNull(){
		fha1.setAssociateObjectType(null);
		
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.canDownLoadFile(user, Arrays.asList(fha1));
		});
	}
	
	@Test
	public void testValidateFileHandleIdNull(){
		fha1.setFileHandleId(null);
		
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.canDownLoadFile(user, Arrays.asList(fha1));
		});
	}
}
