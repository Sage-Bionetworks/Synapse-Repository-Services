package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseClientException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalUploadDestinationSetting;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationSetting;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.utils.MD5ChecksumHelper;

public class IT049FileHandleTest {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userToDelete;
	
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
		projectSetting.setSettingsType("upload");
		ExternalUploadDestinationSetting destination = new ExternalUploadDestinationSetting();
		destination.setUploadType(UploadType.HTTPS);
		destination.setUrl("https://not-valid");
		projectSetting.setDestinations(Collections.<UploadDestinationSetting> singletonList(destination));

		ProjectSetting created = synapse.createProjectSetting(projectSetting);
		assertEquals(project.getId(), created.getProjectId());
		assertEquals("upload", created.getSettingsType());
		assertEquals(UploadDestinationListSetting.class, created.getClass());
		assertEquals(projectSetting.getDestinations(), ((UploadDestinationListSetting) created).getDestinations());

		ProjectSetting clone = synapse.getProjectSetting(project.getId(), "upload");
		assertEquals(created, clone);

		try {
			ProjectSetting clone2 = synapse.getProjectSetting(project.getId(), "upload");
			clone2.setSettingsType("notupload");
			synapse.updateProjectSetting(clone2);
			fail("Should not have succeeded, you cannot change the settings type");
		} catch (SynapseBadRequestException e) {
		}
		((ExternalUploadDestinationSetting) ((UploadDestinationListSetting) clone).getDestinations().get(0)).setUrl("sftp://not-valid");
		synapse.updateProjectSetting(clone);

		ProjectSetting newClone = synapse.getProjectSetting(project.getId(), "upload");
		assertEquals("sftp://not-valid",
				((ExternalUploadDestinationSetting) ((UploadDestinationListSetting) newClone).getDestinations().get(0)).getUrl());

		synapse.deleteProjectSetting(created.getId());

		assertNull(synapse.getProjectSetting(project.getId(), "upload"));
	}

	@Test(expected = NotImplementedException.class)
	public void testExternalUploadDestinationSingleFileRoundTrip() throws SynapseException, IOException, InterruptedException {
		// create an project setting
		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		projectSetting.setProjectId(project.getId());
		projectSetting.setSettingsType("upload");
		ExternalUploadDestinationSetting destination = new ExternalUploadDestinationSetting();
		destination.setUploadType(UploadType.HTTPS);
		destination.setUrl("https://not-valid");
		projectSetting.setDestinations(Collections.<UploadDestinationSetting> singletonList(destination));
		synapse.createProjectSetting(projectSetting);

		assertNotNull(imageFile);
		assertTrue(imageFile.exists());
		String expectedMD5 = MD5ChecksumHelper.getMD5Checksum(imageFile);
		// Create the image
		String myContentType = "test/content-type";
		FileHandle result = synapse.createFileHandle(imageFile, myContentType, project.getId());
		// assertNotNull(result);
		// S3FileHandle handle = (S3FileHandle) result;
		// toDelete.add(handle);
		// System.out.println(handle);
		// assertEquals(myContentType, handle.getContentType());
		// assertEquals(FILE_NAME, handle.getFileName());
		// assertEquals(new Long(imageFile.length()), handle.getContentSize());
		// assertEquals(expectedMD5, handle.getContentMd5());
		//
		// // preview will not be created for our test content type
		//
		// // Now delete the root file handle.
		// synapse.deleteFileHandle(handle.getId());
		// // The main handle and the preview should get deleted.
		// try {
		// synapse.getRawFileHandle(handle.getId());
		// fail("The handle should be deleted.");
		// } catch (SynapseNotFoundException e) {
		// // expected.
		// }
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
}
