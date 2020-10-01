package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.AsynchJobType;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.entity.FileHandleUpdateRequest;
import org.sagebionetworks.repo.model.file.AddFileToDownloadListRequest;
import org.sagebionetworks.repo.model.file.AddFileToDownloadListResponse;
import org.sagebionetworks.repo.model.file.BatchFileHandleCopyRequest;
import org.sagebionetworks.repo.model.file.BatchFileHandleCopyResult;
import org.sagebionetworks.repo.model.file.BatchFileRequest;
import org.sagebionetworks.repo.model.file.BatchFileResult;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.DownloadList;
import org.sagebionetworks.repo.model.file.DownloadOrder;
import org.sagebionetworks.repo.model.file.DownloadOrderSummaryRequest;
import org.sagebionetworks.repo.model.file.DownloadOrderSummaryResponse;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.FileHandleCopyRequest;
import org.sagebionetworks.repo.model.file.FileHandleCopyResult;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.FileResult;
import org.sagebionetworks.repo.model.file.FileResultFailureCode;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.utils.MD5ChecksumHelper;

import com.google.common.collect.Lists;

public class IT054FileEntityTest {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userToDelete;
	
	private static final long MAX_WAIT_MS = 1000*10; // 10 sec
	private static final String FILE_NAME = "LittleImage.png";

	private File imageFile;
	private CloudProviderFileHandleInterface fileHandle;
	private Project project;
	private Folder folder;
	private FileEntity file;
	private FileHandleAssociation association;
	private List<String> fileHandlesToDelete = Lists.newArrayList();
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(StackConfigurationSingleton.singleton().getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
	}
	
	@Before
	public void before() throws SynapseException, FileNotFoundException, IOException {
		adminSynapse.clearAllLocks();
		// Create a project, this will own the file entity
		project = new Project();
		project = synapse.createEntity(project);
		// Get the image file from the classpath.
		URL url = IT054FileEntityTest.class.getClassLoader().getResource("images/"+FILE_NAME);
		imageFile = new File(url.getFile().replaceAll("%20", " "));
		assertNotNull(imageFile);
		assertTrue(imageFile.exists());

		fileHandle = synapse.multipartUpload(imageFile, null, true, true);
		fileHandlesToDelete.add(fileHandle.getId());
		
		// create a folder
		folder = new Folder();
		folder.setName("someFolder");
		folder.setParentId(project.getId());
		folder = this.synapse.createEntity(folder);
		// Add a file to the folder
		file = new FileEntity();
		file.setName("someFile");
		file.setParentId(folder.getId());
		file.setDataFileHandleId(this.fileHandle.getId());
		file = this.synapse.createEntity(file);
		
		// Association for this file.
		association = new FileHandleAssociation();
		association.setAssociateObjectId(file.getId());
		association.setAssociateObjectType(FileHandleAssociateType.FileEntity);
		association.setFileHandleId(file.getDataFileHandleId());
		
		synapse.clearDownloadList();
	}

	@After
	public void after() throws Exception {
		if(project != null){
			synapse.deleteEntity(project, true);
		}
		for (String handle : fileHandlesToDelete) {
			try {
				synapse.deleteFileHandle(handle);
			} catch (Exception e) {}
		}
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		try {
			adminSynapse.deleteUser(userToDelete);
		} catch (SynapseException e) { }
	}
	
	@Test
	public void testFileEntityRoundTrip() throws SynapseException, IOException, InterruptedException, JSONObjectAdapterException{
		// Before we start the test wait for the preview to be created
		CloudProviderFileHandleInterface previewFileHandle = waitForPreviewToBeCreated(fileHandle);
		// Get the file handles
		FileHandleResults fhr = synapse.getEntityFileHandlesForCurrentVersion(file.getId());
		assertNotNull(fhr);
		assertNotNull(fhr.getList());
		assertEquals(2, fhr.getList().size());
		assertEquals(fileHandle.getId(), fhr.getList().get(0).getId());
		assertEquals(previewFileHandle.getId(), fhr.getList().get(1).getId());
		// Repeat the test for version
		fhr = synapse.getEntityFileHandlesForVersion(file.getId(), file.getVersionNumber());
		assertNotNull(fhr);
		assertNotNull(fhr.getList());
		assertEquals(2, fhr.getList().size());
		assertEquals(fileHandle.getId(), fhr.getList().get(0).getId());
		assertEquals(previewFileHandle.getId(), fhr.getList().get(1).getId());

		// Make sure we can get the URLs for this file
		URL tempUrl = synapse.getFileEntityTemporaryUrlForCurrentVersion(file.getId());
		assertNotNull(tempUrl);
		assertTrue("The temporary URL did not contain the expected file handle key",tempUrl.toString().contains(fileHandle.getKey()));
		// now check that the redirect-based download works correctly
		File tempfile = File.createTempFile("test", null);
		tempfile.deleteOnExit();
		synapse.downloadFromFileEntityCurrentVersion(file.getId(), tempfile);
		assertEquals(fileHandle.getContentMd5(),  MD5ChecksumHelper.getMD5Checksum(tempfile));

		// Get the url using the version number
		tempUrl = synapse.getFileEntityTemporaryUrlForVersion(file.getId(), file.getVersionNumber());
		assertNotNull(tempUrl);
		assertTrue("The temporary URL did not contain the expected file handle key",tempUrl.toString().contains(fileHandle.getKey()));
		synapse.downloadFromFileEntityForVersion(file.getId(), file.getVersionNumber(), tempfile);
		assertEquals(fileHandle.getContentMd5(),  MD5ChecksumHelper.getMD5Checksum(tempfile));

		// Now get the preview URLs
		tempUrl = synapse.getFileEntityPreviewTemporaryUrlForCurrentVersion(file.getId());
		assertNotNull(tempUrl);
		assertTrue("The temporary URL did not contain the expected file handle key",tempUrl.toString().contains(previewFileHandle.getKey()));
		synapse.downloadFromFileEntityPreviewCurrentVersion(file.getId(), tempfile);
		assertTrue(tempfile.length()>0);

		// Get the preview using the version number
		tempUrl = synapse.getFileEntityPreviewTemporaryUrlForVersion(file.getId(), file.getVersionNumber());
		assertNotNull(tempUrl);
		assertTrue("The temporary URL did not contain the expected file handle key",tempUrl.toString().contains(previewFileHandle.getKey()));
		synapse.downloadFromFileEntityPreviewForVersion(file.getId(), file.getVersionNumber(), tempfile);
		assertTrue(tempfile.length()>0);
		
		FileHandleAssociation association = new FileHandleAssociation();
		association.setAssociateObjectType(FileHandleAssociateType.FileEntity);
		association.setAssociateObjectId(file.getId());
		association.setFileHandleId(fileHandle.getId());
		BatchFileRequest request = new BatchFileRequest();
		request.setIncludeFileHandles(true);
		request.setIncludePreSignedURLs(true);
		request.setRequestedFiles(Lists.newArrayList(association));
		
		BatchFileResult results = synapse.getFileHandleAndUrlBatch(request);
		assertNotNull(results);
		assertNotNull(results.getRequestedFiles());
		assertEquals(1, results.getRequestedFiles().size());
		FileResult result = results.getRequestedFiles().get(0);
		assertEquals(fileHandle.getId(), result.getFileHandleId());
		assertNotNull(result.getFileHandle());
		assertEquals(fileHandle.getId(), result.getFileHandle().getId());
		assertNotNull(result.getPreSignedURL());
		assertNull(result.getFailureCode());

		/*
		 * test copy FileHandles
		 */

		BatchFileHandleCopyRequest batch = new BatchFileHandleCopyRequest();
		List<FileHandleCopyRequest> requests = new ArrayList<FileHandleCopyRequest>(2);
		batch.setCopyRequests(requests);

		FileHandleAssociation fha1 = association;
		FileHandleAssociation fha2 = new FileHandleAssociation();
		fha2.setAssociateObjectId(fha1.getAssociateObjectId());
		fha2.setAssociateObjectType(fha1.getAssociateObjectType());
		fha2.setFileHandleId("-1");

		FileHandleCopyRequest request1 = new FileHandleCopyRequest();
		request1.setOriginalFile(fha1);
		String newFileName = "newFileName";
		request1.setNewFileName(newFileName);
		FileHandleCopyRequest request2 = new FileHandleCopyRequest();
		request2.setOriginalFile(fha2);

		requests.add(request1);
		requests.add(request2);

		BatchFileHandleCopyResult copyResult = synapse.copyFileHandles(batch);
		assertNotNull(copyResult);
		List<FileHandleCopyResult> copyResults = copyResult.getCopyResults();
		assertNotNull(copyResults);
		assertEquals(2, copyResults.size());
		FileHandleCopyResult first = copyResults.get(0);
		FileHandleCopyResult second = copyResults.get(1);
		assertEquals(fha1.getFileHandleId(), first.getOriginalFileHandleId());
		assertNull(first.getFailureCode());
		assertEquals(fha2.getFileHandleId(), second.getOriginalFileHandleId());
		assertEquals(FileResultFailureCode.UNAUTHORIZED, second.getFailureCode());
		assertNull(second.getNewFileHandle());

		FileHandle newFileHandle = first.getNewFileHandle();
		assertNotNull(newFileHandle);
		assertFalse(newFileHandle.getId().equals(fileHandle.getId()));
		assertEquals(userToDelete.toString(), newFileHandle.getCreatedBy());
		assertEquals(newFileName, newFileHandle.getFileName());
		assertFalse(newFileHandle.getEtag().equals(fileHandle.getEtag()));
		assertFalse(newFileHandle.getCreatedOn().equals(fileHandle.getCreatedOn()));
		assertEquals(fileHandle.getContentMd5(), newFileHandle.getContentMd5());
		assertEquals(fileHandle.getContentType(), newFileHandle.getContentType());
		assertEquals(fileHandle.getConcreteType(), newFileHandle.getConcreteType());
		assertEquals(fileHandle.getContentSize(), newFileHandle.getContentSize());
		assertEquals(fileHandle.getStorageLocationId(), newFileHandle.getStorageLocationId());
		assertEquals(fileHandle.getBucketName(), ((S3FileHandle) newFileHandle).getBucketName());
		assertEquals(fileHandle.getKey(), ((S3FileHandle) newFileHandle).getKey());
	}

	@Test
	public void testGetEntityHeaderByMd5() throws Exception {

		String md5 = "548c050497fb361742b85e0835c0cc96";
		List<EntityHeader> results = synapse.getEntityHeaderByMd5(md5);
		assertNotNull(results);
		assertEquals(0, results.size());

		md5 = fileHandle.getContentMd5();
		results = synapse.getEntityHeaderByMd5(md5);
		assertNotNull(results);
		assertEquals(1, results.size());
	}
	
	@Test
	public void testAddFilesToDownloadListAsynch() throws Exception {
		// Start a job to add this file to the user's download list
		AddFileToDownloadListRequest request = new AddFileToDownloadListRequest();
		request.setFolderId(folder.getId());
		
		AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.AddFileToDownloadList, request, (AddFileToDownloadListResponse response) -> {
			assertNotNull(response);
			assertNotNull(response.getDownloadList());
			DownloadList list = response.getDownloadList();
			assertNotNull(list.getFilesToDownload());
			assertEquals(1, list.getFilesToDownload().size());
		}, MAX_WAIT_MS * 2);
	}
	
	@Test
	public void testDownloadListAddRemoveClearGet() throws Exception {
		// call under test
		DownloadList list = synapse.addFilesToDownloadList(Lists.newArrayList(association));
		assertNotNull(list);
		assertNotNull(list.getFilesToDownload());
		assertEquals(1, list.getFilesToDownload().size());
		// call under test
		DownloadList fromGet = synapse.getDownloadList();
		assertEquals(list, fromGet);
		
		// call under test
		list = synapse.removeFilesFromDownloadList(Lists.newArrayList(association));
		assertNotNull(list);
		assertNotNull(list.getFilesToDownload());
		assertEquals(0, list.getFilesToDownload().size());
		
		// add it back
		list = synapse.addFilesToDownloadList(Lists.newArrayList(association));
		assertNotNull(list);
		assertNotNull(list.getFilesToDownload());
		assertEquals(1, list.getFilesToDownload().size());
		
		// call under test
		synapse.clearDownloadList();
		list = synapse.getDownloadList();
		assertNotNull(list);
		assertNotNull(list.getFilesToDownload());
		assertEquals(0, list.getFilesToDownload().size());
		
	}
	
	@Test
	public void testCreateDownloadOrder() throws Exception {
		DownloadList list = synapse.addFilesToDownloadList(Lists.newArrayList(association));
		String zipName = "theNameOfTheZip.zip";
		// call under test
		DownloadOrder order = synapse.createDownloadOrderFromUsersDownloadList(zipName);
		assertNotNull(order);
		assertNotNull(order.getFiles());
		assertEquals(1, order.getFiles().size());
		// the dowload list should be empty
		list = synapse.getDownloadList();
		assertNotNull(list);
		assertNotNull(list.getFilesToDownload());
		assertEquals(0, list.getFilesToDownload().size());
		
		// call under test
		DownloadOrder fromGet = synapse.getDownloadOrder(order.getOrderId());
		assertEquals(order, fromGet);
		
		// call under test
		DownloadOrderSummaryResponse summary = synapse.getDownloadOrderHistory(new DownloadOrderSummaryRequest());
		assertNotNull(summary);
		assertNotNull(summary.getPage());
		assertEquals(1, summary.getPage().size());
		assertEquals(order.getOrderId(), summary.getPage().get(0).getOrderId());
	}
	
	@Test
	public void testUpdateFileHandle() throws Exception {
		// Make a copy of the file handle
		BatchFileHandleCopyRequest batch = new BatchFileHandleCopyRequest();
		batch.setCopyRequests(new ArrayList<>());


		FileHandleCopyRequest copyRequest = new FileHandleCopyRequest();
		copyRequest.setOriginalFile(association);
		copyRequest.setNewFileName("NewFileName");
		
		batch.getCopyRequests().add(copyRequest);

		FileHandleCopyResult copyResult = synapse.copyFileHandles(batch).getCopyResults().get(0);
		
		String newFileHandleId = copyResult.getNewFileHandle().getId();
		String oldFileHandleId = file.getDataFileHandleId();
		
		FileHandleUpdateRequest request = new FileHandleUpdateRequest();
		
		request.setOldFileHandleId(oldFileHandleId);
		request.setNewFileHandleId(newFileHandleId);
		
		Long currentVersion = file.getVersionNumber();
		
		synapse.updateEntityFileHandle(file.getId(), file.getVersionNumber(), request);
		
		file = synapse.getEntity(file.getId(), FileEntity.class);

		assertEquals(currentVersion, file.getVersionNumber());
		assertEquals(newFileHandleId, file.getDataFileHandleId());

	}
	
	/**
	 * Test for PLFM-6439, which is a request to allow apostrophe in file names.
	 * @throws FileNotFoundException
	 * @throws SynapseException
	 * @throws IOException
	 */
	@Test
	public void testApostropheInName() throws FileNotFoundException, SynapseException, IOException {
		String name = "HasApostrophe'";
		String fileContents = "some data";
		File source = File.createTempFile(name, ".txt");
		File downloaded = File.createTempFile(name, ".txt");
		try {
			FileUtils.write(source, "some data", StandardCharsets.UTF_8);
			fileHandle = synapse.multipartUpload(source, null, true, true);
			fileHandlesToDelete.add(fileHandle.getId());
			
			file = new FileEntity();
			file.setName(name);
			file.setParentId(folder.getId());
			file.setDataFileHandleId(this.fileHandle.getId());
			file = this.synapse.createEntity(file);
			
			FileHandleAssociation fileHandleAssociation = new FileHandleAssociation();
			fileHandleAssociation.setAssociateObjectId(file.getId());
			fileHandleAssociation.setAssociateObjectType(FileHandleAssociateType.FileEntity);
			fileHandleAssociation.setFileHandleId(fileHandle.getId());
			URL url = this.synapse.getFileURL(fileHandleAssociation);
			System.out.println(url.toString());
			// download the file to a temp file using the pre-signed URL.
			FileUtils.copyURLToFile(url, downloaded);
			String resultContent = FileUtils.readFileToString(downloaded, StandardCharsets.UTF_8);
			assertEquals(fileContents, resultContent);
		}finally {
			if(source != null) {
				source.delete();
			}
			if(downloaded != null) {
				downloaded.delete();
			}
		}
	}

	/**
	 * Wait for a preview to be generated for the given file handle.
	 * @throws InterruptedException
	 * @throws SynapseException
	 */
	private CloudProviderFileHandleInterface waitForPreviewToBeCreated(CloudProviderFileHandleInterface fileHandle) throws InterruptedException,
			SynapseException {
		long start = System.currentTimeMillis();
		while(fileHandle.getPreviewId() == null){
			System.out.println("Waiting for a preview file to be created");
			Thread.sleep(1000);
			assertTrue("Timed out waiting for a preview to be created",(System.currentTimeMillis()-start) < MAX_WAIT_MS);
			fileHandle = (S3FileHandle) synapse.getRawFileHandle(fileHandle.getId());
		}
		// Fetch the preview file handle
		CloudProviderFileHandleInterface previewFileHandle = (CloudProviderFileHandleInterface) synapse.getRawFileHandle(fileHandle.getPreviewId());
		fileHandlesToDelete.add(previewFileHandle.getId());
		return previewFileHandle;
	}
}
