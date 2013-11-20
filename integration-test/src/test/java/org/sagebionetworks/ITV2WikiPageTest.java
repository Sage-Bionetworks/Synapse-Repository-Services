package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.utils.MD5ChecksumHelper;

public class ITV2WikiPageTest {
	private static String FILE_NAME = "LittleImage.png";
	private static String FILE_NAME2 = "profile_pic.png";
	private static String MARKDOWN_NAME = "previewtest.txt";
	public static final long MAX_WAIT_MS = 1000*20; // 10 sec
	
	private List<WikiPageKey> toDelete = null;
	private static SynapseClientImpl synapse = null;
	S3FileHandle fileHandle;
	S3FileHandle fileHandleTwo;
	S3FileHandle markdownHandle;
	File imageFile;
	File imageFileTwo;
	File markdownFile;
	Project project;
	
	private static SynapseClientImpl createSynapseClient(String user, String pw) throws SynapseException {
		SynapseClientImpl synapse = new SynapseClientImpl();
		synapse.setAuthEndpoint(StackConfiguration
				.getAuthenticationServicePrivateEndpoint());
		synapse.setRepositoryEndpoint(StackConfiguration
				.getRepositoryServiceEndpoint());
		synapse.setFileEndpoint(StackConfiguration.getFileServiceEndpoint());
		synapse.login(user, pw);
		
		return synapse;
	}
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		synapse = createSynapseClient(StackConfiguration.getIntegrationTestUserOneName(),
				StackConfiguration.getIntegrationTestUserOnePassword());
	}
	
	@Before
	public void before() throws SynapseException {
		toDelete = new ArrayList<WikiPageKey>();
		// Get image and markdown files from the classpath.
		URL url = ITV2WikiPageTest.class.getClassLoader().getResource("images/"+FILE_NAME);
		URL url2 = ITV2WikiPageTest.class.getClassLoader().getResource("images/" + FILE_NAME2);
		URL markdownUrl = ITV2WikiPageTest.class.getClassLoader().getResource("images/"+MARKDOWN_NAME);
		imageFile = new File(url.getFile().replaceAll("%20", " "));
		imageFileTwo = new File(url2.getFile().replaceAll("%20", " "));
		markdownFile = new File(markdownUrl.getFile().replaceAll("%20", " "));
		
		assertNotNull(imageFile);
		assertTrue(imageFile.exists());
		assertNotNull(imageFileTwo);
		assertTrue(imageFileTwo.exists());
		assertNotNull(markdownFile);
		assertTrue(markdownFile.exists());
		
		// Create the file handles
		List<File> list = new LinkedList<File>();
		list.add(imageFile);
		list.add(imageFileTwo);
		list.add(markdownFile);
		FileHandleResults results = synapse.createFileHandles(list);
		assertNotNull(results);
		assertNotNull(results.getList());
		assertEquals(3, results.getList().size());
		fileHandle = (S3FileHandle) results.getList().get(0);
		fileHandleTwo = (S3FileHandle) results.getList().get(1);
		markdownHandle = (S3FileHandle) results.getList().get(2);
		// Create a project, this will own the wiki page.
		project = new Project();
		project = synapse.createEntity(project);
	}
	
	@After
	public void after() throws Exception {
		if(fileHandle != null){
			try {
				synapse.deleteFileHandle(fileHandle.getId());
			} catch (Exception e) {}
		}
		if(fileHandleTwo != null){
			try {
				synapse.deleteFileHandle(fileHandleTwo.getId());
			} catch (Exception e) {}
		}
		if(markdownHandle != null) {
			try {
				synapse.deleteFileHandle(markdownHandle.getId());
			} catch (Exception e) {}
		}
		if(project != null){
			synapse.deleteAndPurgeEntity(project);
		}
		for(WikiPageKey key: toDelete){
			synapse.deleteWikiPage(key);
		}
	}
	
	@Test
	public void testV2WikiRoundTrip() throws SynapseException, IOException, InterruptedException, JSONObjectAdapterException{
		V2WikiPage wiki = new V2WikiPage();
		wiki.setAttachmentFileHandleIds(new ArrayList<String>());
		wiki.getAttachmentFileHandleIds().add(fileHandle.getId());
		wiki.setMarkdownFileHandleId(markdownHandle.getId());
		wiki.setTitle("ITV2WikiPageTest.testWikiRoundTrip");
		// Create a V2WikiPage
		wiki = synapse.createV2WikiPage(project.getId(), ObjectType.ENTITY, wiki);
		assertNotNull(wiki);
		assertNotNull(wiki.getAttachmentFileHandleIds());
		assertEquals(1, wiki.getAttachmentFileHandleIds().size());
		assertEquals(fileHandle.getId(), wiki.getAttachmentFileHandleIds().get(0));
		WikiPageKey key = new WikiPageKey(project.getId(), ObjectType.ENTITY, wiki.getId());
		toDelete.add(key);
		Date firstModifiedOn = wiki.getModifiedOn();
		
		// Add another file attachment and update the wiki
		wiki.getAttachmentFileHandleIds().add(fileHandleTwo.getId());
		wiki = synapse.updateV2WikiPage(key.getOwnerObjectId(), key.getOwnerObjectType(), wiki);
		assertNotNull(wiki);
		assertNotNull(wiki.getAttachmentFileHandleIds());
		assertEquals(2, wiki.getAttachmentFileHandleIds().size());
		assertEquals(fileHandle.getId(), wiki.getAttachmentFileHandleIds().get(0));
		assertEquals(fileHandleTwo.getId(), wiki.getAttachmentFileHandleIds().get(1));
		assertEquals(markdownHandle.getId(), wiki.getMarkdownFileHandleId());
		assertTrue(!wiki.getModifiedOn().equals(firstModifiedOn));
		
		// test get
		wiki = synapse.getV2WikiPage(key);
		V2WikiPage root = synapse.getV2RootWikiPage(project.getId(), ObjectType.ENTITY);
		assertEquals(wiki, root);
		
		// Get the tree
		PaginatedResults<V2WikiHeader> tree = synapse.getV2WikiHeaderTree(key.getOwnerObjectId(), key.getOwnerObjectType());
		assertNotNull(tree);
		assertNotNull(tree.getResults());
		assertEquals(1, tree.getResults().size());
		assertEquals(1, tree.getTotalNumberOfResults());
		
		// Get history
		PaginatedResults<V2WikiHistorySnapshot> history = synapse.getV2WikiHistory(key, new Long(10), new Long(0));
		assertNotNull(history);
		assertNotNull(history.getResults());
		assertTrue(history.getResults().size() == 2);
		// First snapshot is most recent, so we want the last snapshot, version 0 (the first entry in history)
		String versionToRestore = history.getResults().get(1).getVersion();
		// Get the version first
		V2WikiPage firstVersion = synapse.getVersionOfV2WikiPage(key, new Long(versionToRestore));
		assertEquals(1, firstVersion.getAttachmentFileHandleIds().size());
		assertEquals(fileHandle.getId(), firstVersion.getAttachmentFileHandleIds().get(0));
		assertEquals(firstModifiedOn, firstVersion.getModifiedOn());
		
		// Restore wiki to first state before update
		wiki = synapse.restoreV2WikiPage(key.getOwnerObjectId(), key.getOwnerObjectType(), wiki, new Long(versionToRestore));
		assertNotNull(wiki);
		assertNotNull(wiki.getAttachmentFileHandleIds());
		assertNotNull(wiki.getMarkdownFileHandleId());
		// Get history again
		PaginatedResults<V2WikiHistorySnapshot> history2 = synapse.getV2WikiHistory(key, new Long(10), new Long(0));
		assertNotNull(history2);
		assertNotNull(history2.getResults());
		assertTrue(history2.getResults().size() == 3);

		assertTrue(wiki.getAttachmentFileHandleIds().size() == 1);
		assertEquals(fileHandle.getId(), wiki.getAttachmentFileHandleIds().get(0));
		assertEquals(markdownHandle.getId(), wiki.getMarkdownFileHandleId());
		
		// Delete the wiki
		synapse.deleteV2WikiPage(key);
		// Now try to get it
		try{
			synapse.getV2WikiPage(key);
			fail("This should have failed as the wiki was deleted");
		}catch(SynapseException e){
			// expected;
		}
	}
	
	@Test
	public void testGetWikiPageAttachmentsAndMarkdown() throws Exception{
		V2WikiPage wiki = new V2WikiPage();
		wiki.setTitle("IT055WikiPageTest.testGetWikiPageAttachmentFileHandles");
		wiki.setAttachmentFileHandleIds(new LinkedList<String>());
		wiki.getAttachmentFileHandleIds().add(fileHandle.getId());
		wiki.setMarkdownFileHandleId(markdownHandle.getId());	
		// Create it
		wiki = synapse.createV2WikiPage(project.getId(), ObjectType.ENTITY, wiki);
		assertNotNull(wiki);
		WikiPageKey key = new WikiPageKey(project.getId(), ObjectType.ENTITY, wiki.getId());
		toDelete.add(key);
		
		// Since we expect the preview file handle to be returned we need to wait for it.
		waitForPreviewToBeCreated(fileHandle);
		// There should be two handles for this WikiPage, one for the origin file
		// and the other for the preview.
		FileHandleResults results = synapse.getV2WikiAttachmentHandles(key);
		assertNotNull(results);
		assertNotNull(results.getList());
		
		assertEquals(markdownHandle.getId(), wiki.getMarkdownFileHandleId());
		// there should be two things on the list, the original file and its preview.
		assertEquals(2, results.getList().size());
		FileHandle one = results.getList().get(0);
		assertTrue(one instanceof S3FileHandle);
		S3FileHandle handle = (S3FileHandle) one;
		FileHandle two = results.getList().get(1);
		assertTrue(two instanceof PreviewFileHandle);
		PreviewFileHandle preview = (PreviewFileHandle) two;
		assertTrue(handle.getPreviewId().equals(preview.getId()));
		// Make sure we can download
		File mainFile = null;
		File previewFile = null;
		try{
			// Download the files from Synapse:
			mainFile = synapse.downloadV2WikiAttachment(key, handle.getFileName());
			assertNotNull(mainFile);
			// Make sure we can also just get the temporary URL
			URL tempUrl = synapse.getV2WikiAttachmentTemporaryUrl(key, handle.getFileName());
			assertNotNull(tempUrl);
			// the file should be the expected size
			assertEquals(handle.getContentSize().longValue(), mainFile.length());
			// Check the MD5
			String md5 = MD5ChecksumHelper.getMD5Checksum(mainFile);
			assertEquals(handle.getContentMd5(), md5);
			// Download the preview
			previewFile = synapse.downloadV2WikiAttachmentPreview(key, handle.getFileName());
			assertNotNull(previewFile);
			// the file should be the expected size
			assertEquals(preview.getContentSize().longValue(), previewFile.length());
			// Make sure we can also just get the temporary URL
			tempUrl = synapse.getV2WikiAttachmentPreviewTemporaryUrl(key, handle.getFileName());
			assertNotNull(tempUrl);
		}finally{
			if(mainFile != null){
				mainFile.delete();
			}
			if(previewFile != null){
				previewFile.delete();
			}
		}
	}
	
	/**
	 * Wait for a preview to be generated for the given file handle.
	 * @throws InterruptedException
	 * @throws SynapseException
	 */
	private void waitForPreviewToBeCreated(S3FileHandle fileHandle) throws InterruptedException,
			SynapseException {
		long start = System.currentTimeMillis();
		while(fileHandle.getPreviewId() == null){
			System.out.println("Waiting for a preview file to be created");
			Thread.sleep(1000);
			assertTrue("Timed out waiting for a preview to be created",(System.currentTimeMillis()-start) < MAX_WAIT_MS);
			fileHandle = (S3FileHandle) synapse.getRawFileHandle(fileHandle.getId());
		}
	}
}
