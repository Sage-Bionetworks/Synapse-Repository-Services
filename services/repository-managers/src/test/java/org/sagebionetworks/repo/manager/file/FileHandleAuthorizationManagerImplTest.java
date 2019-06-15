package org.sagebionetworks.repo.manager.file;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationStatus;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.springframework.test.util.ReflectionTestUtils;

public class FileHandleAuthorizationManagerImplTest {

	AuthorizationManager mockAuthManager;
	FileHandleAuthorizationManager manager;
	UserInfo user;

	FileHandleAssociation fha1;
	FileHandleAssociation fha2;
	FileHandleAssociation fha3;

	@Before
	public void before() {
		mockAuthManager = Mockito.mock(AuthorizationManager.class);
		manager = new FileHandleAuthorizationManagerImpl();
		ReflectionTestUtils.setField(manager, "authorizationManager",
				mockAuthManager);

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
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateNullList(){
		manager.canDownLoadFile(user, null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateNulItem(){
		manager.canDownLoadFile(user, Arrays.asList(fha2, null));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateObjectIdNull(){
		fha1.setAssociateObjectId(null);
		manager.canDownLoadFile(user, Arrays.asList(fha1));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateObjectTypeNull(){
		fha1.setAssociateObjectType(null);
		manager.canDownLoadFile(user, Arrays.asList(fha1));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateFileHandleIdNull(){
		fha1.setFileHandleId(null);
		manager.canDownLoadFile(user, Arrays.asList(fha1));
	}
}
