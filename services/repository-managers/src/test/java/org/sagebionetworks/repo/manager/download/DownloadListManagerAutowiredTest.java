package org.sagebionetworks.repo.manager.download;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sagebionetworks.repo.model.util.AccessControlListUtil.createResourceAccess;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.LocalFileUploadRequest;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.download.v2.DownloadListDAO;
import org.sagebionetworks.repo.model.download.DownloadListItem;
import org.sagebionetworks.repo.model.download.DownloadListPackageRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.helper.AccessControlListObjectHelper;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.sagebionetworks.repo.model.jdo.NameValidation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DownloadListManagerAutowiredTest {

	@Autowired
	private DownloadListManager downloadListManager;
	@Autowired
	private DownloadListDAO downloadListDao;
	@Autowired
	private DaoObjectHelper<Node> nodeDaoHelper;
	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private AccessControlListObjectHelper aclHelper;
	@Autowired
	private AccessControlListDAO aclDao;
	@Autowired
	private FileHandleManager fileUploadManager;
	@Autowired
	private UserManager userManager;
	
	private UserInfo userInfo;
	private UserInfo adminUserInfo;
	private List<String> fileHandleIdsToDelete;
	
	
	@BeforeEach
	public void beforeEach() {
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		String userName = UUID.randomUUID().toString();
		userInfo = userManager.createOrGetTestUser(adminUserInfo,
				new NewUser().setUserName(userName).setEmail(userName + "@foo.org"), true);
		
		aclDao.truncateAll();
		nodeDao.truncateAll();
		downloadListDao.truncateAllData();
		
		fileHandleIdsToDelete = new ArrayList<>();
	}
	
	@AfterEach
	public void after() {
		aclDao.truncateAll();
		nodeDao.truncateAll();
		downloadListDao.truncateAllData();
		
		if (fileHandleIdsToDelete != null && userInfo != null) {
			for (String fileHandleId : fileHandleIdsToDelete) {
				fileUploadManager.deleteFileHandle(userInfo, fileHandleId);
			}
		}
		if (userInfo != null) {
			userManager.deletePrincipal(adminUserInfo, userInfo.getId());
		}

	}
	
	// Test to reproduce: https://sagebionetworks.jira.com/browse/PLFM-7692
	@Test
	public void testPackageFilesWithFileNameTooLong() throws IOException {
		Node project = nodeDaoHelper.create((n) -> {
			n.setNodeType(EntityType.project);
			n.setName("project");
		});
		
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userInfo.getId(), ACCESS_TYPE.DOWNLOAD));
			a.getResourceAccess().add(createResourceAccess(userInfo.getId(), ACCESS_TYPE.READ));
		});
		
		Node file1 = createS3File("one.txt", "contents of one", project.getId());
		Node file2 = createS3File("two.txt", "contents of two is bigger", project.getId());
		List<DownloadListItem> batchToAdd = Arrays.asList(
				new DownloadListItem().setFileEntityId(file1.getId()),
				new DownloadListItem().setFileEntityId(file2.getId()));
		downloadListDao.addBatchOfFilesToDownloadList(userInfo.getId(), batchToAdd);
		
		DownloadListPackageRequest request = new DownloadListPackageRequest()
				.setIncludeManifest(true)
				.setZipFileName("a".repeat(NameValidation.MAX_NAME_CHARS + 1));
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			downloadListManager.packageFiles(null, userInfo, request);
		}).getMessage();
		
		assertEquals(NameValidation.NAME_LENGTH_TOO_LONG, errorMessage);
	}
	
	public Node createS3File(String fileName, String fileText, String parentId) throws IOException {
		S3FileHandle fileHandle = uploadFileToS3(userInfo, fileName, fileText);
		fileHandleIdsToDelete.add(fileHandle.getId());
		Node file = nodeDaoHelper.create((n) -> {
			n.setParentId(parentId);
			n.setNodeType(EntityType.file);
			n.setName(fileName);
			n.setFileHandleId(fileHandle.getId());
		});
		return file;
	}
	
	public S3FileHandle uploadFileToS3(UserInfo userInfo, String fileName, String fileBody) throws IOException {
		File temp = File.createTempFile("fileName", ".txt");
		try {
			FileUtils.writeStringToFile(temp, fileBody, StandardCharsets.UTF_8);
			return fileUploadManager.uploadLocalFile(new LocalFileUploadRequest().withFileName(fileName)
					.withUserId(userInfo.getId().toString()).withFileToUpload(temp).withContentType("text/plain")
					.withStorageLocationId(StorageLocationDAO.DEFAULT_STORAGE_LOCATION_ID));
		} finally {
			temp.delete();
		}
	}
	
}
