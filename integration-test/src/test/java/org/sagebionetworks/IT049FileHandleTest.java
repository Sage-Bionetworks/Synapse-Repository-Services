package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseClientException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseResultNotReadyException;
import org.sagebionetworks.repo.manager.S3TestUtils;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.file.BulkFileDownloadResponse;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.ExternalObjectStoreFileHandle;
import org.sagebionetworks.repo.model.file.ExternalObjectStoreUploadDestination;
import org.sagebionetworks.repo.model.file.ExternalUploadDestination;
import org.sagebionetworks.repo.model.file.FileDownloadStatus;
import org.sagebionetworks.repo.model.file.FileDownloadSummary;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.ProxyFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.S3UploadDestination;
import org.sagebionetworks.repo.model.file.UploadDestination;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalObjectStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.ProxyStorageLocationSettings;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.utils.MD5ChecksumHelper;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.google.common.collect.Lists;

public class IT049FileHandleTest {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userToDelete;
	
	private static final long MAX_WAIT_MS = 1000*10; // 10 sec
	private static final String FILE_NAME = "LittleImage.png";

	private List<FileHandle> toDelete = null;
	private File imageFile;
	private Project project;

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
	public void before() throws SynapseException {
		adminSynapse.clearAllLocks();
		toDelete = new ArrayList<FileHandle>();
		// Get the image file from the classpath.
		URL url = IT049FileHandleTest.class.getClassLoader().getResource("images/"+FILE_NAME);
		imageFile = new File(url.getFile().replaceAll("%20", " "));
		project = new Project();
		project = synapse.createEntity(project);
	}

	@After
	public void after() throws Exception {
		for (FileHandle handle: toDelete) {
			try {
				synapse.deleteFileHandle(handle.getId());
			} catch (SynapseNotFoundException e) {
			} catch (SynapseClientException e) { }
		}
		synapse.deleteEntity(project, true);
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		try {
			adminSynapse.deleteUser(userToDelete);
		} catch (SynapseException e) { }
	}
	
	@Test
	public void testImageFileRoundTrip() throws SynapseException, IOException, InterruptedException{
		assertNotNull(imageFile);
		assertTrue(imageFile.exists());
		String expectedMD5 = MD5ChecksumHelper.getMD5Checksum(imageFile);

		S3FileHandle handle = synapse.multipartUpload(imageFile, null, true, false);
		toDelete.add(handle);
		System.out.println(handle);
		assertEquals("image/png", handle.getContentType());
		assertEquals(FILE_NAME, handle.getFileName());
		assertEquals(new Long(imageFile.length()), handle.getContentSize());
		assertEquals(expectedMD5, handle.getContentMd5());
		
		// Now wait for the preview to be created.
		long start = System.currentTimeMillis();
		while(handle.getPreviewId() == null){
			System.out.println("Waiting for a preview to be created...");
			Thread.sleep(1000);
			assertTrue("Timed out waiting for a preview image to be created.", (System.currentTimeMillis()-start) < MAX_WAIT_MS);
			handle = (S3FileHandle) synapse.getRawFileHandle(handle.getId());
		}
		// Get the preview file handle.
		PreviewFileHandle preview = (PreviewFileHandle) synapse.getRawFileHandle(handle.getPreviewId());
		assertNotNull(preview);
		System.out.println(preview);
		toDelete.add(preview);
		
		//clear the preview and wait for it to be recreated
		synapse.clearPreview(handle.getId());
		handle = (S3FileHandle) synapse.getRawFileHandle(handle.getId());
		while(handle.getPreviewId() == null){
			System.out.println("Waiting for a preview to be recreated...");
			Thread.sleep(1000);
			assertTrue("Timed out waiting for a preview image to be created.", (System.currentTimeMillis()-start) < MAX_WAIT_MS);
			handle = (S3FileHandle) synapse.getRawFileHandle(handle.getId());
		}
		preview = (PreviewFileHandle) synapse.getRawFileHandle(handle.getPreviewId());
		assertNotNull(preview);
		toDelete.add(preview);
		
		// Now delete the root file handle.
		synapse.deleteFileHandle(handle.getId());
		// The main handle and the preview should get deleted.
		try{
			synapse.getRawFileHandle(handle.getId());
			fail("The handle should be deleted.");
		}catch(SynapseNotFoundException e){
			// expected.
		}
		try{
			synapse.getRawFileHandle(handle.getPreviewId());
			fail("The handle should be deleted.");
		}catch(SynapseNotFoundException e){
			// expected.
		}
	}
	
	@Test
	public void testExternalRoundTrip() throws JSONObjectAdapterException, SynapseException{
		ExternalFileHandle efh = new ExternalFileHandle();
		efh.setContentType("text/plain");
		efh.setFileName("foo.bar");
		efh.setExternalURL("http://google.com");
		// Save it
		ExternalFileHandle clone = synapse.createExternalFileHandle(efh);
		assertNotNull(clone);
		toDelete.add(clone);
		assertNotNull(clone.getId());
		assertNotNull(clone.getCreatedBy());
		assertNotNull(clone.getCreatedOn());
		assertNotNull(clone.getEtag());
		assertEquals(efh.getFileName(), clone.getFileName());
		assertEquals(efh.getExternalURL(), clone.getExternalURL());
		assertEquals(efh.getContentType(), clone.getContentType());
	}
	
	@Test
	public void testExternalRoundTripWithNulls() throws JSONObjectAdapterException, SynapseException{
		// Us a null name and content type
		ExternalFileHandle efh = new ExternalFileHandle();
		efh.setContentType(null);
		efh.setFileName(null);
		efh.setExternalURL("http://google.com");
		// Save it
		ExternalFileHandle clone = synapse.createExternalFileHandle(efh);
		assertNotNull(clone);
		toDelete.add(clone);
		assertNotNull(clone.getId());
		assertNotNull(clone.getCreatedBy());
		assertNotNull(clone.getCreatedOn());
		assertNotNull(clone.getEtag());
		assertEquals("NOT_SET", clone.getFileName());
		assertEquals(efh.getExternalURL(), clone.getExternalURL());
		assertEquals("NOT_SET", clone.getContentType());
	}
	
	@Test
	public void testProjectSettingsCrud() throws SynapseException, IOException, InterruptedException {
		// create an project setting
		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		projectSetting.setProjectId(project.getId());
		projectSetting.setSettingsType(ProjectSettingsType.upload);

		ExternalStorageLocationSetting externalDestination = new ExternalStorageLocationSetting();
		externalDestination.setUploadType(UploadType.HTTPS);
		externalDestination.setUrl("https://notvalid.com");
		externalDestination.setBanner("warning, at institute");
		externalDestination.setDescription("not in synapse, this is");

		List<StorageLocationSetting> settings = synapse.getMyStorageLocationSettings();
		assertFalse(settings.contains(externalDestination));

		externalDestination = synapse.createStorageLocationSetting(externalDestination);

		settings = synapse.getMyStorageLocationSettings();
		assertTrue(settings.contains(externalDestination));
		StorageLocationSetting settingsClone = synapse.getMyStorageLocationSetting(externalDestination.getStorageLocationId());
		assertEquals(externalDestination, settingsClone);

		projectSetting.setLocations(Lists.newArrayList(externalDestination.getStorageLocationId()));
		ProjectSetting created = synapse.createProjectSetting(projectSetting);
		assertEquals(project.getId(), created.getProjectId());
		assertEquals(ProjectSettingsType.upload, created.getSettingsType());
		assertEquals(UploadDestinationListSetting.class, created.getClass());
		assertEquals(projectSetting.getLocations(), ((UploadDestinationListSetting) created).getLocations());

		ProjectSetting clone = synapse.getProjectSetting(project.getId(), ProjectSettingsType.upload);
		assertEquals(created, clone);

		UploadDestination uploadDestination = synapse.getUploadDestination(project.getId(), externalDestination.getStorageLocationId());
		assertEquals(externalDestination.getUploadType(), uploadDestination.getUploadType());
		assertEquals(externalDestination.getBanner(), uploadDestination.getBanner());
		assertEquals(externalDestination.getStorageLocationId(), uploadDestination.getStorageLocationId());

		synapse.deleteProjectSetting(created.getId());

		assertNull(synapse.getProjectSetting(project.getId(), ProjectSettingsType.upload));
	}

	@Test
	public void testProxyFileHandleRoundTrip() throws SynapseException, JSONObjectAdapterException, IOException{
		ProxyStorageLocationSettings storageLocation = new ProxyStorageLocationSettings();
		storageLocation.setSecretKey("Super secret key that must be fairly long");
		storageLocation.setProxyUrl("https://host.org");
		storageLocation.setUploadType(UploadType.SFTP);
		// create the storage location
		storageLocation = synapse.createStorageLocationSetting(storageLocation);
		
		// Create a ProxyFileHandle
		ProxyFileHandle handle = new ProxyFileHandle();
		handle.setContentMd5("md5");
		handle.setContentSize(123L);
		handle.setContentType("text/plain");
		handle.setFileName("barFoo.txt");
		handle.setFilePath("pathParent/pathChild");
		handle.setStorageLocationId(storageLocation.getStorageLocationId());
		handle = synapse.createExternalProxyFileHandle(handle);
		
		// get a pre-signed url for this object
		URL preSigned = synapse.getFileHandleTemporaryUrl(handle.getId());
		assertNotNull(preSigned);
		assertEquals("host.org", preSigned.getHost());
		String expectedPath = "/sftp/"+handle.getFilePath();
		assertEquals(expectedPath, preSigned.getPath());
	}
	
	@Test
	public void testProxyLocalFileHandleRoundTrip() throws SynapseException, JSONObjectAdapterException, IOException{
		ProxyStorageLocationSettings storageLocation = new ProxyStorageLocationSettings();
		storageLocation.setSecretKey("Super secret key that must be fairly long");
		storageLocation.setProxyUrl("https://host.org");
		storageLocation.setUploadType(UploadType.PROXYLOCAL);
		// create the storage location
		storageLocation = synapse.createStorageLocationSetting(storageLocation);
		
		// Create a ProxyFileHandle
		ProxyFileHandle handle = new ProxyFileHandle();
		handle.setContentMd5("md5");
		handle.setContentSize(123L);
		handle.setContentType("text/plain");
		handle.setFileName("barFoo.txt");
		handle.setFilePath("pathParent/pathChild");
		handle.setStorageLocationId(storageLocation.getStorageLocationId());
		handle = synapse.createExternalProxyFileHandle(handle);
		
		// get a pre-signed url for this object
		URL preSigned = synapse.getFileHandleTemporaryUrl(handle.getId());
		assertNotNull(preSigned);
		assertEquals("host.org", preSigned.getHost());
		String expectedPath = "/proxylocal/"+handle.getFilePath();
		assertEquals(expectedPath, preSigned.getPath());
	}

	@Test
	public void testExternalObjectStoreFileHandleRoundTrip() throws SynapseException {
		//create a new StorageLocationSetting
		ExternalObjectStorageLocationSetting storageLocationSetting = new ExternalObjectStorageLocationSetting();
		String bucket = "some bucket";
		String endpoint = "https://someurl.com";
		storageLocationSetting.setBucket(bucket);
		storageLocationSetting.setEndpointUrl(endpoint);
		storageLocationSetting.setUploadType(UploadType.S3);
		storageLocationSetting = synapse.createStorageLocationSetting(storageLocationSetting);

		//make sure the StorageLocationSetting exists
		List<StorageLocationSetting> settings = synapse.getMyStorageLocationSettings();
		assertTrue(settings.contains(storageLocationSetting));

		//change the project setting to use the newly created storage location
		UploadDestinationListSetting projectUploadSetting = new UploadDestinationListSetting();
		projectUploadSetting.setProjectId(project.getId());
		projectUploadSetting.setSettingsType(ProjectSettingsType.upload);
		projectUploadSetting.setLocations(Lists.newArrayList(storageLocationSetting.getStorageLocationId()));
		UploadDestinationListSetting createdUploadSetting = (UploadDestinationListSetting) synapse.createProjectSetting(projectUploadSetting);
		assertEquals(project.getId(), createdUploadSetting.getProjectId());
		assertEquals(ProjectSettingsType.upload, createdUploadSetting.getSettingsType());
		assertEquals(projectUploadSetting.getLocations(), createdUploadSetting.getLocations());

		//retrieve a upload destination for that project
		ExternalObjectStoreUploadDestination uploadDestination = (ExternalObjectStoreUploadDestination) synapse.getDefaultUploadDestination(project.getId());
		assertNotNull(uploadDestination.getKeyPrefixUUID());
		assertEquals(endpoint, uploadDestination.getEndpointUrl());
		assertEquals(bucket, uploadDestination.getBucket());

		//create the filehandle based off of the upload destination information
		ExternalObjectStoreFileHandle fileHandle = new ExternalObjectStoreFileHandle();
		fileHandle.setFileKey(uploadDestination.getKeyPrefixUUID() + "/asdf.txt");
		fileHandle.setStorageLocationId(storageLocationSetting.getStorageLocationId());
		fileHandle.setContentMd5("md5");
		fileHandle.setContentSize(1234L);
		fileHandle.setContentType("text/plain");
		ExternalObjectStoreFileHandle createdFileHandle = synapse.createExternalObjectStoreFileHandle(fileHandle);

		//Assert file handle has mirrored information about the bucket and endpointUrl from the storageLocationSetting
		assertEquals(endpoint, createdFileHandle.getEndpointUrl());
		assertEquals(bucket, createdFileHandle.getBucket());

		//Assert created file handle has same metadata as the file handle given as argument
		assertEquals(fileHandle.getFileKey(), createdFileHandle.getFileKey());
		assertEquals(fileHandle.getStorageLocationId(), createdFileHandle.getStorageLocationId());
		assertEquals(fileHandle.getContentMd5(), createdFileHandle.getContentMd5());
		assertEquals(fileHandle.getContentSize(), createdFileHandle.getContentSize());
		assertEquals(fileHandle.getContentType(), createdFileHandle.getContentType());

		//make sure created file handle is same as the ones retrieved by id
		ExternalObjectStoreFileHandle retrievedByIdFileHandle = (ExternalObjectStoreFileHandle) synapse.getRawFileHandle(createdFileHandle.getId());
		assertEquals(createdFileHandle, retrievedByIdFileHandle);
	}

	@Test
	public void testExternalUploadDestinationChoice() throws SynapseException, IOException, InterruptedException {
		// create an project setting
		ExternalStorageLocationSetting externalDestination = new ExternalStorageLocationSetting();
		externalDestination.setUploadType(UploadType.SFTP);
		externalDestination.setUrl("sftp://somewhere.com");
		externalDestination.setBanner("warning, at institute");
		externalDestination.setDescription("not in synapse, this is");
		externalDestination = synapse.createStorageLocationSetting(externalDestination);

		S3StorageLocationSetting internalS3Destination = new S3StorageLocationSetting();
		internalS3Destination.setUploadType(UploadType.S3);
		internalS3Destination.setBanner("warning, not at institute");
		internalS3Destination = synapse.createStorageLocationSetting(internalS3Destination);

		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		projectSetting.setProjectId(project.getId());
		projectSetting.setSettingsType(ProjectSettingsType.upload);
		projectSetting.setLocations(Lists.newArrayList(externalDestination.getStorageLocationId(),
				internalS3Destination.getStorageLocationId()));
		synapse.createProjectSetting(projectSetting);

		UploadDestinationLocation[] uploadDestinationLocations = synapse.getUploadDestinationLocations(project.getId());
		assertEquals(2, uploadDestinationLocations.length);
		assertEquals(externalDestination.getStorageLocationId(), uploadDestinationLocations[0].getStorageLocationId());
		assertEquals(internalS3Destination.getStorageLocationId(), uploadDestinationLocations[1].getStorageLocationId());

		UploadDestination uploadDestination = synapse.getUploadDestination(project.getId(),
				uploadDestinationLocations[0].getStorageLocationId());
		assertEquals(UploadType.SFTP, uploadDestination.getUploadType());
		assertEquals(externalDestination.getStorageLocationId(), uploadDestination.getStorageLocationId());
		assertEquals(ExternalUploadDestination.class, uploadDestination.getClass());

		uploadDestination = synapse.getUploadDestination(project.getId(), uploadDestinationLocations[1].getStorageLocationId());
		assertEquals(UploadType.S3, uploadDestination.getUploadType());
		assertEquals(internalS3Destination.getStorageLocationId(), uploadDestination.getStorageLocationId());
		assertEquals(S3UploadDestination.class, uploadDestination.getClass());
	}

	@Test
	public void testCreateSFTPExternalFile() throws Exception {
		ExternalFileHandle efh = new ExternalFileHandle();
		efh.setContentType(null);
		efh.setFileName(null);
		efh.setExternalURL("sftp://somewhere.com");
		ExternalFileHandle clone = synapse.createExternalFileHandle(efh);
		toDelete.add(clone);
	}

	/**
	 * This test just ensures the web-services are setup correctly.
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 */
	@Test
	public void testBulkFileDownload() throws Exception {
		
		TableEntity table = new TableEntity();
		table.setParentId(project.getId());
		table.setName("BulkDownloadTest");
		table = adminSynapse.createEntity(table);
		
		ExternalFileHandle efh = new ExternalFileHandle();
		efh.setContentType("text/plain");
		efh.setFileName("foo.bar");
		efh.setExternalURL("http://google.com");
		// Save it
		ExternalFileHandle clone = synapse.createExternalFileHandle(efh);
		this.toDelete.add(clone);
		
		FileHandleAssociation fha = new FileHandleAssociation();
		fha.setFileHandleId(clone.getId());
		fha.setAssociateObjectId(table.getId());
		fha.setAssociateObjectType(FileHandleAssociateType.TableEntity);
		
		BulkFileDownloadRequest request = new BulkFileDownloadRequest();
		request.setRequestedFiles(Arrays.asList(fha));
		
		String jobId = adminSynapse.startBulkFileDownload(request);
		BulkFileDownloadResponse respones = waitForJob(jobId);
		assertNotNull(respones);
		assertNotNull(respones.getFileSummary());
		assertEquals(1, respones.getFileSummary().size());
		FileDownloadSummary summary = respones.getFileSummary().get(0);
		// should fail since we attempted to include an external file handle.
		assertEquals(FileDownloadStatus.FAILURE, summary.getStatus());
		// Result file handle should be null since it failed to do the one file
		assertEquals(null, respones.getResultZipFileHandleId());
	}
	
	/**
	 * Wait for a bulk download job to finish.
	 * @param jobId
	 * @return
	 * @throws SynapseException
	 * @throws InterruptedException
	 */
	private BulkFileDownloadResponse waitForJob(String jobId) throws SynapseException, InterruptedException{
		long start = System.currentTimeMillis();
		while(true){
			try {
				return adminSynapse.getBulkFileDownloadResults(jobId);
			} catch (SynapseResultNotReadyException e) {
				System.out.println("Waiting for job: "+e.getJobStatus());
			}
			assertTrue("Timed out waiting for bulk download job.",System.currentTimeMillis() - start < MAX_WAIT_MS);
			Thread.sleep(2000);
		}
	}
	
	@Test
	public void testMultipartUploadV2() throws FileNotFoundException, SynapseException, IOException{
		assertNotNull(imageFile);
		assertTrue(imageFile.exists());
		String expectedMD5 = MD5ChecksumHelper.getMD5Checksum(imageFile);
		// upload the little image using mutli-part upload
		Long storageLocationId = null;
		Boolean generatePreview = false;
		Boolean forceRestart = null;
		S3FileHandle result = synapse.multipartUpload(this.imageFile, storageLocationId, generatePreview, forceRestart);
		assertNotNull(result);
		toDelete.add(result);
		assertNotNull(result.getFileName());
		assertEquals(expectedMD5, result.getContentMd5());
	}
	
	/**
	 * Validate that a forceRestart=true actually restarts the upload.s
	 * @throws FileNotFoundException
	 * @throws SynapseException
	 * @throws IOException
	 */
	@Test
	public void testMultipartUploadV2Reset() throws FileNotFoundException, SynapseException, IOException{
		Boolean forceRestart = false;
		MultipartUploadRequest request = new MultipartUploadRequest();
		request.setContentMD5Hex("47f208f98d738d5ff3330f4a0b358788");
		request.setContentType("plain/text");
		request.setFileName("foo.txt");
		request.setFileSizeBytes(1L);
		request.setGeneratePreview(false);
		request.setStorageLocationId(null);
		request.setPartSizeBytes((long) (5*1024*1024));
		// start this job once.
		MultipartUploadStatus startStatus = synapse.startMultipartUpload(request, forceRestart);
		assertNotNull(startStatus);
		assertNotNull(startStatus.getUploadId());
		// Starting again should yield the same job
		MultipartUploadStatus statusAgain = synapse.startMultipartUpload(request, forceRestart);
		assertNotNull(statusAgain);
		assertEquals(startStatus.getUploadId(), statusAgain.getUploadId());
		// now a force restart should yield a new id.
		forceRestart = true;
		statusAgain = synapse.startMultipartUpload(request, forceRestart);
		assertNotNull(statusAgain);
		assertFalse(startStatus.getUploadId().equals(statusAgain.getUploadId()));
	}
}
