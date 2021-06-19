package org.sagebionetworks.download.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.sagebionetworks.repo.model.util.AccessControlListUtil.createResourceAccess;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.LocalFileUploadRequest;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.file.download.v2.DownloadListDAO;
import org.sagebionetworks.repo.model.download.DownloadListItem;
import org.sagebionetworks.repo.model.download.DownloadListPackageRequest;
import org.sagebionetworks.repo.model.download.DownloadListPackageResponse;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.helper.AccessControlListObjectHelper;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.amazonaws.services.s3.model.GetObjectRequest;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DownloadListPackageWorkerIntegrationTest {

	public static final long MAX_WAIT_MS = 1000 * 30;

	public static final int MAX_RETRIES = 25;

	@Autowired
	private UserManager userManager;
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
	private SynapseS3Client s3Client;

	@Autowired
	AsynchronousJobWorkerHelper asynchronousJobWorkerHelper;

	UserInfo adminUser;
	UserInfo user;

	List<String> fileHandleIdsToDelete;

	@BeforeEach
	public void before() {
		aclDao.truncateAll();
		nodeDao.truncateAll();
		downloadListDao.truncateAllData();

		adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		boolean acceptsTermsOfUse = true;
		String userName = UUID.randomUUID().toString();
		user = userManager.createOrGetTestUser(adminUser,
				new NewUser().setUserName(userName).setEmail(userName + "@foo.org"), acceptsTermsOfUse);
		fileHandleIdsToDelete = new ArrayList<>();
	}

	@AfterEach
	public void after() {
		aclDao.truncateAll();
		nodeDao.truncateAll();
		downloadListDao.truncateAllData();
		if (fileHandleIdsToDelete != null && user != null) {
			for (String fileHandleId : fileHandleIdsToDelete) {
				fileUploadManager.deleteFileHandle(user, fileHandleId);
			}
		}
		if (user != null) {
			userManager.deletePrincipal(adminUser, user.getId());
		}

	}

	@Test
	public void testPackage() throws Exception {
		Node project = nodeDaoHelper.create((n) -> {
			n.setNodeType(EntityType.project);
			n.setName("project");
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(user.getId(), ACCESS_TYPE.DOWNLOAD));
			a.getResourceAccess().add(createResourceAccess(user.getId(), ACCESS_TYPE.READ));
		});

		S3FileHandle fh1 = uploadFileToS3(user, "one.txt", "contents of one");
		fileHandleIdsToDelete.add(fh1.getId());
		Node file1 = nodeDaoHelper.create((n) -> {
			n.setParentId(project.getId());
			n.setNodeType(EntityType.file);
			n.setName("one.txt");
			n.setFileHandleId(fh1.getId());
		});

		S3FileHandle fh2 = uploadFileToS3(user, "two.txt", "contents of two is bigger");
		fileHandleIdsToDelete.add(fh2.getId());
		Node file2 = nodeDaoHelper.create((n) -> {
			n.setParentId(project.getId());
			n.setNodeType(EntityType.file);
			n.setName("two.txt");
			n.setFileHandleId(fh2.getId());
		});

		long addedCount = downloadListDao.addBatchOfFilesToDownloadList(user.getId(),
				Arrays.asList(new DownloadListItem().setFileEntityId(file1.getId()),
						new DownloadListItem().setFileEntityId(file2.getId())));
		assertEquals(2L, addedCount);
		assertEquals(2L, downloadListDao.getTotalNumberOfFilesOnDownloadList(user.getId()));

		DownloadListPackageRequest request = new DownloadListPackageRequest();
		// call under test
		DownloadListPackageResponse respone = asynchronousJobWorkerHelper.assertJobResponse(user, request, (DownloadListPackageResponse response) -> {
			assertNotNull(response.getResultFileHandleId());
			fileHandleIdsToDelete.add(response.getResultFileHandleId());
		}, MAX_WAIT_MS, MAX_RETRIES).getResponse();
		
		assertEquals(0L, downloadListDao.getTotalNumberOfFilesOnDownloadList(user.getId()));
		
		S3FileHandle zipHandle = (S3FileHandle)fileUploadManager.getRawFileHandle(user, respone.getResultFileHandleId());
		
		File temp = File.createTempFile("package", ".zip");
		try {
			s3Client.getObject(new GetObjectRequest(zipHandle.getBucketName(), zipHandle.getKey()), temp);
			try(ZipInputStream in = new ZipInputStream(new FileInputStream(temp))){
				ZipEntry entry = in.getNextEntry();
				assertEquals("one.txt", entry.getName());
				entry = in.getNextEntry();
				assertEquals("two.txt", entry.getName());
			}
		}finally {
			temp.delete();
		}

	}

	/**
	 * Upload an actual file to S3.
	 * 
	 * @param userInfo
	 * @param fileName
	 * @param fileBody
	 * @return
	 * @throws IOException
	 */
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
