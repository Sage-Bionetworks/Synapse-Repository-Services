package org.sagebionetworks.report.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.report.DownloadStorageReportRequest;
import org.sagebionetworks.repo.model.report.DownloadStorageReportResponse;
import org.sagebionetworks.repo.model.report.StorageReportType;
import org.sagebionetworks.repo.model.table.EntityDTO;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.utils.ContentTypeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class StorageReportCSVDownloadWorkerIntegrationTest {
	
	public static final long MAX_WAIT_MS = 1000 * 60;

	@Autowired
	private UserManager userManager;
	@Autowired
	private AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private FileHandleManager fileHandleManager;
	@Autowired
	private ConnectionFactory tableConnectionFactory;


	Long adminUser;
	UserInfo adminUserInfo;
	Project project1;
	Project project2;
	S3FileHandle fileHandle1;
	S3FileHandle fileHandle2;
	S3FileHandle fileHandle3;
	FileEntity file1;
	FileEntity file2;
	FileEntity file3;
	String file1Id;
	String file2Id;
	String file3Id;
	String project1Id;
	String project2Id;

	@Before
	public void before() throws Exception {
		adminUser = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		adminUserInfo = userManager.getUserInfo(adminUser);
		project1 = new Project();
		project1.setName("Storage Report IT Project 1");
		project2 = new Project();
		project2.setName("Storage Report IT Project 2");
		project1Id = entityManager.createEntity(adminUserInfo, project1, null);
		project2Id = entityManager.createEntity(adminUserInfo, project2, null);

		// Content size of 0 bytes
		fileHandle1 = fileHandleManager.createFileFromByteArray(adminUserInfo
						.getId().toString(), new Date(), "".getBytes(StandardCharsets.UTF_8), "foo1.txt",
				ContentTypeUtil.TEXT_PLAIN_UTF8, null);

		// Content size of 4 bytes
		fileHandle2 = fileHandleManager.createFileFromByteArray(adminUserInfo
						.getId().toString(), new Date(), "abcd".getBytes(StandardCharsets.UTF_8), "foo2.txt",
				ContentTypeUtil.TEXT_PLAIN_UTF8, null);

		// Content size of 8 bytes
		fileHandle3 = fileHandleManager.createFileFromByteArray(adminUserInfo
						.getId().toString(), new Date(), "abcdefgh".getBytes(StandardCharsets.UTF_8), "foo3.txt",
				ContentTypeUtil.TEXT_PLAIN_UTF8, null);

		// Files in project 1, content size total should be 0 + 4 = 4
		file1 = new FileEntity();
		file1.setName("file1.txt");
		file1.setParentId(project1Id);
		file1.setDataFileHandleId(fileHandle1.getId());
		file1Id = entityManager.createEntity(adminUserInfo, file1, null);

		file2 = new FileEntity();
		file2.setName("file2.txt");
		file2.setParentId(project1Id);
		file2.setDataFileHandleId(fileHandle2.getId());
		file2Id = entityManager.createEntity(adminUserInfo, file2, null);

		// File in project 2, content size total should be 8
		file3 = new FileEntity();
		file3.setName("file3.txt");
		file3.setParentId(project2Id);
		file3.setDataFileHandleId(fileHandle3.getId());
		file3Id = entityManager.createEntity(adminUserInfo, file3, null);
		waitForEntityReplication(project1Id);
		waitForEntityReplication(project2Id);
		waitForEntityReplication(file1Id);
		waitForEntityReplication(file2Id);
		waitForEntityReplication(file3Id);
	}
	
	@After
	public void after(){
		entityManager.deleteEntity(adminUserInfo, file1Id);
		entityManager.deleteEntity(adminUserInfo, file2Id);
		entityManager.deleteEntity(adminUserInfo, file3Id);
		entityManager.deleteEntity(adminUserInfo, project1Id);
		entityManager.deleteEntity(adminUserInfo, project2Id);
	}

	@Test
	public void testCreateReport() throws Exception {
		DownloadStorageReportRequest request = new DownloadStorageReportRequest();
		request.setReportType(StorageReportType.ALL_PROJECTS);
		DownloadStorageReportResponse response = startAndWaitForJob(adminUserInfo, request, DownloadStorageReportResponse.class);
		assertNotNull(response);
		assertNotNull(response.getResultsFileHandleId());

		// Verify that the CSV can be downloaded via the filehandle, and the contents are as expected
		// (A CSV with project ID, project name, size, ordered descending)
		String csvContents = fileHandleManager.downloadFileToString(response.getResultsFileHandleId());
		String expectedContents = "\"projectId\",\"projectName\",\"sizeInBytes\"\n" +
				"\"" + project2Id + "\",\"" + project2.getName() + "\",\"8\"\n" +
				"\"" + project1Id + "\",\"" + project1.getName() + "\",\"4\"\n";
		assertEquals(expectedContents, csvContents);
	}


	/**
	 * Start an asynchronous job and wait for the results.
	 * @param user
	 * @param body
	 * @return
	 * @throws InterruptedException 
	 */
	@SuppressWarnings("unchecked")
	public <T extends AsynchronousResponseBody> T  startAndWaitForJob(UserInfo user, AsynchronousRequestBody body, Class<? extends T> clazz) throws InterruptedException{
		long startTime = System.currentTimeMillis();
		AsynchronousJobStatus status = asynchJobStatusManager.startJob(user, body);
		while(true){
			status = asynchJobStatusManager.getJobStatus(user, status.getJobId());
			switch(status.getJobState()){
			case FAILED:
				assertTrue("Job failed: "+status.getErrorDetails(), false);
			case PROCESSING:
				assertTrue("Timed out waiting for job to complete",(System.currentTimeMillis()-startTime) < MAX_WAIT_MS);
				System.out.println("Waiting for job: "+status.getProgressMessage());
				Thread.sleep(1000);
				break;
			case COMPLETE:
				return (T)status.getResponseBody();
			}
		}
	}

	/**
	 * Wait for EntityReplication to show the given etag for the given entityId.
	 *
	 * @param entityId
	 * @return
	 * @throws InterruptedException
	 */
	private EntityDTO waitForEntityReplication(String entityId) throws InterruptedException{
		Entity entity = entityManager.getEntity(adminUserInfo, entityId);
		TableIndexDAO indexDao = tableConnectionFactory.getFirstConnection();
		while(true){
			EntityDTO dto = indexDao.getEntityData(KeyFactory.stringToKey(entityId));
			if(dto == null || !dto.getEtag().equals(entity.getEtag())){
				System.out.println("Waiting for entity replication. id: "+entityId+" etag: "+entity.getEtag());
				Thread.sleep(1000);
			}else{
				return dto;
			}
		}
	}
}
