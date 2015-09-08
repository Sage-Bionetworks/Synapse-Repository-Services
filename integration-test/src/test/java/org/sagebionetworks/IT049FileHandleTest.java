package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
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
import org.sagebionetworks.repo.model.file.ExternalUploadDestination;
import org.sagebionetworks.repo.model.file.FileDownloadStatus;
import org.sagebionetworks.repo.model.file.FileDownloadSummary;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.S3UploadDestination;
import org.sagebionetworks.repo.model.file.UploadDestination;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalSyncSetting;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.util.TimedAssert;
import org.sagebionetworks.utils.MD5ChecksumHelper;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.util.BinaryUtils;
import com.google.common.collect.Lists;

public class IT049FileHandleTest {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userToDelete;
	private static AmazonS3Client s3Client;
	
	private static final String LARGE_FILE_PATH_PROP_KEY = "org.sagebionetworks.test.large.file.path";
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
		adminSynapse.setUserName(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		
		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
		s3Client = new AmazonS3Client(new BasicAWSCredentials(StackConfiguration.getIAMUserId(), StackConfiguration.getIAMUserKey()));
		s3Client.createBucket(StackConfiguration.singleton().getExternalS3TestBucketName());
	}
	
	@Before
	public void before() throws SynapseException {
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
		S3TestUtils.doDeleteAfter(s3Client);
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
		// Create the image
		List<File> list = new LinkedList<File>();
		list.add(imageFile);
		FileHandleResults results = synapse.createFileHandles(list, project.getId());
		assertNotNull(results);
		// We should have one image on the list
		assertNotNull(results.getList());
		assertEquals(1, results.getList().size());
		S3FileHandle handle = (S3FileHandle) results.getList().get(0);
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
	public void testSingleFileRoundTrip() throws SynapseException, IOException, InterruptedException{
		assertNotNull(imageFile);
		assertTrue(imageFile.exists());
		String expectedMD5 = MD5ChecksumHelper.getMD5Checksum(imageFile);
		// Create the image
		String myContentType = "test/content-type";
		FileHandle result = synapse.createFileHandle(imageFile, myContentType, project.getId());
		assertNotNull(result);
		S3FileHandle handle = (S3FileHandle) result;
		toDelete.add(handle);
		System.out.println(handle);
		assertEquals(myContentType, handle.getContentType());
		assertEquals(FILE_NAME, handle.getFileName());
		assertEquals(new Long(imageFile.length()), handle.getContentSize());
		assertEquals(expectedMD5, handle.getContentMd5());
		
		//preview will not be created for our test content type

		// Now delete the root file handle.
		synapse.deleteFileHandle(handle.getId());
		// The main handle and the preview should get deleted.
		try{
			synapse.getRawFileHandle(handle.getId());
			fail("The handle should be deleted.");
		}catch(SynapseNotFoundException e){
			// expected.
		}
	}
	
	@Test
	public void testSingleFileDeprecatedRoundTrip() throws SynapseException, IOException, InterruptedException {
		assertNotNull(imageFile);
		assertTrue(imageFile.exists());
		String expectedMD5 = MD5ChecksumHelper.getMD5Checksum(imageFile);
		// Create the image
		String myContentType = "test/content-type";
		@SuppressWarnings("deprecation")
		FileHandle result = synapse.createFileHandle(imageFile, myContentType);
		assertNotNull(result);
		S3FileHandle handle = (S3FileHandle) result;
		toDelete.add(handle);
		System.out.println(handle);
		assertEquals(myContentType, handle.getContentType());
		assertEquals(FILE_NAME, handle.getFileName());
		assertEquals(new Long(imageFile.length()), handle.getContentSize());
		assertEquals(expectedMD5, handle.getContentMd5());

		// preview will not be created for our test content type

		// Now delete the root file handle.
		synapse.deleteFileHandle(handle.getId());
		// The main handle and the preview should get deleted.
		try {
			synapse.getRawFileHandle(handle.getId());
			fail("The handle should be deleted.");
		} catch (SynapseNotFoundException e) {
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
	public void testExternalUploadDestinationUploadAndModifyRoundTrip() throws Exception {
		String baseKey = "test-" + UUID.randomUUID();

		// we need to create a authentication object
		String username = synapse.getUserSessionData().getProfile().getUserName();
		S3TestUtils.createObjectFromString(StackConfiguration.singleton().getExternalS3TestBucketName(), baseKey + "owner.txt", username,
				s3Client);

		// create setting
		ExternalS3StorageLocationSetting externalS3Destination = new ExternalS3StorageLocationSetting();
		externalS3Destination.setUploadType(UploadType.S3);
		externalS3Destination.setEndpointUrl(null);
		externalS3Destination.setBucket(StackConfiguration.singleton().getExternalS3TestBucketName());
		externalS3Destination.setBaseKey(baseKey);
		externalS3Destination.setBanner("warning, at institute");
		externalS3Destination.setDescription("not in synapse, this is");
		externalS3Destination = synapse.createStorageLocationSetting(externalS3Destination);

		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		projectSetting.setProjectId(project.getId());
		projectSetting.setSettingsType(ProjectSettingsType.upload);
		projectSetting.setLocations(Lists.newArrayList(externalS3Destination.getStorageLocationId()));
		synapse.createProjectSetting(projectSetting);

		String myContentType = "test/content-type";
		FileHandle result = synapse.createFileHandle(imageFile, myContentType, project.getId());
		toDelete.add(result);

		assertEquals(S3FileHandle.class, result.getClass());
		assertEquals(externalS3Destination.getStorageLocationId(), result.getStorageLocationId());

		File tmpFile = File.createTempFile(imageFile.getName(), ".tmp");
		synapse.downloadFromFileHandleTemporaryUrl(result.getId(), tmpFile);

		FileHandle result2 = synapse.createFileHandle(imageFile, myContentType, false, project.getId(), result.getStorageLocationId());
		toDelete.add(result2);


		assertEquals(S3FileHandle.class, result2.getClass());
		assertEquals(externalS3Destination.getStorageLocationId(), result2.getStorageLocationId());
		assertTrue(result2 instanceof S3FileHandle);
		S3FileHandle result2S3 = (S3FileHandle) result2;
		
		// Create an external file handle using the external location.
		S3FileHandle externalS3 = new S3FileHandle();
		externalS3.setBucketName(result2S3.getBucketName());
		externalS3.setKey(result2S3.getKey());
		externalS3.setFileName(result2S3.getFileName());
		externalS3.setStorageLocationId(result.getStorageLocationId());
		// create it
		externalS3 = synapse.createExternalS3FileHandle(externalS3);
		assertNotNull(externalS3);
		assertNotNull(externalS3.getId());
	}

	@Ignore //PLFM-3543
	@Test
	public void testAutoSyncRoundTrip() throws Exception {
		String baseKey = "test-" + UUID.randomUUID();

		// we need to create a authentication object
		String username = synapse.getUserSessionData().getProfile().getUserName();
		S3TestUtils.createObjectFromString(StackConfiguration.singleton().getExternalS3TestBucketName(), baseKey + "owner.txt", username,
				s3Client);

		final String md5 = S3TestUtils.createObjectFromString(StackConfiguration.singleton().getExternalS3TestBucketName(), baseKey
				+ "file1.txt", UUID.randomUUID().toString(), s3Client);

		// create setting
		ExternalS3StorageLocationSetting externalS3Destination = new ExternalS3StorageLocationSetting();
		externalS3Destination.setUploadType(UploadType.S3);
		externalS3Destination.setEndpointUrl(null);
		externalS3Destination.setBucket(StackConfiguration.singleton().getExternalS3TestBucketName());
		externalS3Destination.setBaseKey(baseKey);
		externalS3Destination.setBanner("warning, at institute");
		externalS3Destination.setDescription("not in synapse, this is");
		externalS3Destination = synapse.createStorageLocationSetting(externalS3Destination);

		ExternalSyncSetting externalSyncSetting = new ExternalSyncSetting();
		externalSyncSetting.setLocationId(externalS3Destination.getStorageLocationId());
		externalSyncSetting.setAutoSync(true);
		externalSyncSetting.setProjectId(project.getId());
		externalSyncSetting.setSettingsType(ProjectSettingsType.external_sync);
		synapse.createProjectSetting(externalSyncSetting);

		TimedAssert.waitForAssert(30000, 500, new Runnable() {
			@Override
			public void run() {
				try {
					JSONObject query = synapse.query("select name from entity where parentId == '" + project.getId() + "' LIMIT_1_OFFSET_1");
					assertEquals(1L, query.getInt("totalNumberOfResults"));
					assertEquals("file1.txt", query.getJSONArray("results").getJSONObject(0).getString("entity.name"));
					String hexMD5 = BinaryUtils.toHex(BinaryUtils.fromBase64(md5));
					assertEquals(1, synapse.getEntityHeaderByMd5(hexMD5).size());
				} catch (SynapseNotFoundException e) {
					fail();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
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
	 * This test uploads files that are too large to include in the build.
	 * To run this test, set the property to point to a large file: org.sagebionetworks.test.large.file.path=<path to large file>
	 * @throws IOException 
	 * @throws SynapseException 
	 */
	@Test
	public void testLargeFileUplaod() throws SynapseException, IOException{
		String largeFileName = System.getProperty(LARGE_FILE_PATH_PROP_KEY);
		if(largeFileName != null){
			// Run the test
			File largeFile = new File(largeFileName);
			assertTrue(largeFile.exists());
			System.out.println("Attempting to upload a file of size: "+largeFile.length());
			float fileSize = largeFile.length();
			float bytesPerMB = (float) Math.pow(2, 20);
			float fileSizeMB = fileSize/bytesPerMB;
			System.out.println(String.format("Attempting to upload file: %1$s of size %2$.2f",  largeFile.getName(), fileSizeMB));
			String contentType = SynapseClientImpl.guessContentTypeFromStream(largeFile);
			long start = System.currentTimeMillis();
			FileHandle handle = synapse.createFileHandle(largeFile, contentType, project.getId());
			long elapse = System.currentTimeMillis()-start;
			float elapseSecs = elapse/1000;
			float mbPerSec = fileSizeMB/elapseSecs;
			System.out.println(String.format("Upload file: %1$s of size %2$.2f in %3$.2f secs with rate %4$.2f MB/Sec",  largeFile.getName(), fileSizeMB, elapseSecs, mbPerSec));
			assertNotNull(handle);
			toDelete.add(handle);
		}else{
			System.out.println("The property: '"+LARGE_FILE_PATH_PROP_KEY+"' was not set.  The testLargeFileUplaod() test was not run");
		}
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
}
