package org.sagebionetworks.file.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.file.BulkFileDownloadResponse;
import org.sagebionetworks.repo.model.file.FileDownloadStatus;
import org.sagebionetworks.repo.model.file.FileDownloadSummary;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.utils.ContentTypeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class BulkFileDownloadWorkerIntegrationTest {
	
	public static final int MAX_WAIT_MS = 1000*60*3;

	@Autowired
	FileHandleManager fileUploadManager;
	@Autowired
	UserManager userManager;
	@Autowired
	FileHandleSupport bulkDownloadManager;
	@Autowired
	EntityManager entityManager;
	
	@Autowired
	AsynchronousJobWorkerHelper asynchronousJobWorkerHelper;
	
	S3FileHandle fileHandleOne;
	S3FileHandle fileHandleTwo;
	S3FileHandle resulFileHandle;
	
	String fileOneContents;
	String fileTwoContents;
	
	List<String> fileHandlesToDelete;
	List<String> entitiesToDelete;

	String tableId;
	UserInfo adminUserInfo;

	@Before
	public void before() throws Exception {
		adminUserInfo = userManager
				.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER
						.getPrincipalId());
		fileHandlesToDelete = Lists.newLinkedList();
		entitiesToDelete = Lists.newLinkedList();
		
		Project project = new Project();
		project.setName(UUID.randomUUID().toString());
		String id = entityManager.createEntity(adminUserInfo, project, null);
		entitiesToDelete.add(id);
		
		TableEntity table = new TableEntity();
		table.setName("aTable");
		table.setParentId(id);
		
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		entitiesToDelete.add(tableId);
		
		fileOneContents = "one";
		fileTwoContents = "two";
		
		Date now = new Date(System.currentTimeMillis());
		
		// upload two files.
		fileHandleOne = fileUploadManager.createFileFromByteArray(adminUserInfo
				.getId().toString(), now, fileOneContents.getBytes(StandardCharsets.UTF_8), "foo.txt",
				ContentTypeUtil.TEXT_PLAIN_UTF8, null);
		fileHandlesToDelete.add(fileHandleOne.getId());
		fileHandleTwo = fileUploadManager.createFileFromByteArray(adminUserInfo
				.getId().toString(), now, fileTwoContents.getBytes(StandardCharsets.UTF_8), "bar.txt",
				ContentTypeUtil.TEXT_PLAIN_UTF8, null);
		fileHandlesToDelete.add(fileHandleTwo.getId());
		
	}
	
	@After
	public void after(){
		if(fileHandlesToDelete != null){
			for(String id: fileHandlesToDelete){
				try {
					fileUploadManager.deleteFileHandle(adminUserInfo, id);
				} catch (Exception e) {}
			}
		}
		if(entitiesToDelete != null){
			for(String id: entitiesToDelete){
				try {
					entityManager.deleteEntity(adminUserInfo, id);
				} catch (Exception e) {}
			}
		}
	}
	
	@Test
	public void testRoundTrip() throws Exception {
		// one
		FileHandleAssociation fha1 = new FileHandleAssociation();
		fha1.setFileHandleId(fileHandleOne.getId());
		fha1.setAssociateObjectType(FileHandleAssociateType.TableEntity);
		fha1.setAssociateObjectId(tableId);
		// two
		FileHandleAssociation fha2 = new FileHandleAssociation();
		fha2.setFileHandleId(fileHandleTwo.getId());
		fha2.setAssociateObjectType(FileHandleAssociateType.TableEntity);
		fha2.setAssociateObjectId(tableId);
		
		BulkFileDownloadRequest request = new BulkFileDownloadRequest();
		request.setRequestedFiles(Arrays.asList(fha1, fha2));
		// Start the job to create the zip.
		BulkFileDownloadResponse response = this.asynchronousJobWorkerHelper.startAndWaitForJob(adminUserInfo, request,
				MAX_WAIT_MS, BulkFileDownloadResponse.class);
		assertNotNull(response.getFileSummary());
		assertEquals(2, response.getFileSummary().size());
		// one
		FileDownloadSummary summary = response.getFileSummary().get(0);
		assertEquals(fha1.getFileHandleId(), summary.getFileHandleId());
		assertEquals(FileDownloadStatus.SUCCESS,  summary.getStatus());
		assertEquals(null, summary.getFailureMessage());
		assertEquals(null, summary.getFailureCode());
		// two
		summary = response.getFileSummary().get(1);
		assertEquals(fha2.getFileHandleId(), summary.getFileHandleId());
		assertEquals(FileDownloadStatus.SUCCESS,  summary.getStatus());
		assertEquals(null, summary.getFailureMessage());
		assertEquals(null, summary.getFailureCode());
		// must have a result
		assertNotNull(response.getResultZipFileHandleId());
		resulFileHandle = bulkDownloadManager.getS3FileHandle(response.getResultZipFileHandleId());
		fileHandlesToDelete.add(resulFileHandle.getId());
		// Is the zip as expected?
		validateZipContents(fileOneContents, fileTwoContents);
	}

	/**
	 * Helper to check the resulting zip file.
	 * @param fileOneContents
	 * @param fileTwoContents
	 * @param tempZip
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void validateZipContents(String fileOneContents,
			String fileTwoContents) throws FileNotFoundException,
			IOException {
		File tempZip = bulkDownloadManager.downloadToTempFile(resulFileHandle);
		ZipInputStream zipIn = null;
		try{
			zipIn = new ZipInputStream(new FileInputStream(tempZip));
			// Read the first entry
			ZipEntry entry = zipIn.getNextEntry();
			assertNotNull(entry);
			CommandLineCacheZipEntryNameProvider nameProvider = new CommandLineCacheZipEntryNameProvider();
			String entryName = nameProvider.createZipEntryName(fileHandleOne.getFileName(), Long.parseLong(fileHandleOne.getId()));
			assertEquals(entryName, entry.getName());
			// does the file contents match?
			assertEquals(fileOneContents, IOUtils.toString(zipIn));
			zipIn.closeEntry();
			// next entry
			entry = zipIn.getNextEntry();
			assertNotNull(entry);
			entryName = nameProvider.createZipEntryName(fileHandleTwo.getFileName(), Long.parseLong(fileHandleTwo.getId()));
			assertEquals(entryName, entry.getName());
			// does the file contents match?
			assertEquals(fileTwoContents, IOUtils.toString(zipIn));
			zipIn.closeEntry();
		}finally{
			IOUtils.closeQuietly(zipIn);
			tempZip.delete();
		}
	}
	
}
