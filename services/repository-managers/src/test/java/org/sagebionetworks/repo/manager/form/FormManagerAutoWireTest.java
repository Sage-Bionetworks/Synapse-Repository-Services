package org.sagebionetworks.repo.manager.form;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.AuthorizationManagerImpl;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleAuthorizationStatus;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.form.FormChangeRequest;
import org.sagebionetworks.repo.model.form.FormData;
import org.sagebionetworks.repo.model.form.FormGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class FormManagerAutoWireTest {

	@Autowired
	FormManager formManager;
	@Autowired
	UserManager userManager;
	@Autowired
	AuthorizationManagerImpl authorizationManager;
	@Autowired
	UserGroupDAO userGroupDao;
	@Autowired
	FileHandleDao fileDao;
	@Autowired
	IdGenerator idGenerator;

	UserInfo adminUserInfo;
	UserInfo userInfo;
	
	List<S3FileHandle> files;

	@BeforeEach
	public void beforeEach() {
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		UserGroup ug = new UserGroup();
		ug.setIsIndividual(true);
		long userId = userGroupDao.create(ug);
		userInfo = userManager.getUserInfo(userId);
		files = new LinkedList<>();
	}

	@AfterEach
	public void afterEach() {
		formManager.truncateAll();
		for(S3FileHandle file: files) {
			fileDao.delete(file.getId());
		}
		userGroupDao.delete(userInfo.getId().toString());
	}

	/**
	 * Test for PLFM-5821.
	 */
	@Test
	public void testCreateGroup() {
		String groupName = "Group Name";
		FormGroup group = formManager.createGroup(adminUserInfo, groupName);
		assertNotNull(group);
		AccessControlList acl = formManager.getGroupAcl(adminUserInfo, group.getGroupId());
		assertNotNull(acl);
	}

	@Test
	public void testCanDownloadForm() {
		String groupName = "Group Name";
		FormGroup group = formManager.createGroup(adminUserInfo, groupName);
		assertNotNull(group);
		AccessControlList acl = formManager.getGroupAcl(adminUserInfo, group.getGroupId());
		assertNotNull(acl);
		// grant the user submit on the group
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(userInfo.getId());
		ra.setAccessType(Sets.newHashSet(ACCESS_TYPE.SUBMIT));
		acl.getResourceAccess().add(ra);
		formManager.updateGroupAcl(adminUserInfo, group.getGroupId(), acl);

		S3FileHandle fileHandle = createFileHandle(userInfo);
		FormChangeRequest request = new FormChangeRequest();
		String formName = "formName";
		request.setName(formName);
		request.setFileHandleId(fileHandle.getId());

		FormData form = formManager.createFormData(userInfo, group.getGroupId(), request);
		// call under test
		AuthorizationStatus status = formManager.canUserDownloadFormData(userInfo, form.getFormDataId());
		assertNotNull(status);
		assertTrue(status.isAuthorized());

		List<String> fileHandleIds = Lists.newArrayList(form.getDataFileHandleId());
		// second call under test
		List<FileHandleAuthorizationStatus> statusList = authorizationManager.canDownloadFile(userInfo, fileHandleIds,
				form.getFormDataId(), FileHandleAssociateType.FormData);
		assertNotNull(statusList);
		assertEquals(1, statusList.size());
		FileHandleAuthorizationStatus fileStatus = statusList.get(0);
		assertEquals(form.getDataFileHandleId(), fileStatus.getFileHandleId());
		assertNotNull(fileStatus.getStatus());
		assertTrue(fileStatus.getStatus().isAuthorized());
	}

	/**
	 * Helper to create a new FileHandle
	 * 
	 * @return
	 */
	public S3FileHandle createFileHandle(UserInfo user) {
		S3FileHandle handle = TestUtils.createS3FileHandle(user.getId().toString(),
				idGenerator.generateNewId(IdType.FILE_IDS).toString());
		S3FileHandle file = (S3FileHandle) fileDao.createFile(handle);
		files.add(file);
		return file;
	}
}
