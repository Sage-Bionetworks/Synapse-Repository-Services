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
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
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
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.dao.WikiPageKeyHelper;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiOrderHint;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.utils.MD5ChecksumHelper;

public class ITV2WikiPageTest {

	private static String FILE_NAME = "LittleImage.png";
	private static String FILE_NAME2 = "profile_pic.png";
	private static String MARKDOWN_NAME = "previewtest.txt.gz";
	public static final long MAX_WAIT_MS = 1000*20; // 20 sec

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userToDelete;
	
	private List<WikiPageKey> toDelete = null;
	private List<String> fileHandlesToDelete = null;
	private S3FileHandle fileHandle;
	private S3FileHandle fileHandleTwo;
	private S3FileHandle markdownHandle;
	private File imageFile;
	private String imageFileMD5;
	private File imageFileTwo;
	private File markdownFile;
	private Project project;
	private TermsOfUseAccessRequirement accessRequirement;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(StackConfigurationSingleton.singleton().getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
	}
	
	@Before
	public void before() throws SynapseException, IOException {
		adminSynapse.clearAllLocks();
		toDelete = new ArrayList<WikiPageKey>();
		fileHandlesToDelete = new ArrayList<String>();
		// Get image and markdown files from the classpath.
		URL url = ITV2WikiPageTest.class.getClassLoader().getResource("images/"+FILE_NAME);
		URL url2 = ITV2WikiPageTest.class.getClassLoader().getResource("images/" + FILE_NAME2);
		URL markdownUrl = ITV2WikiPageTest.class.getClassLoader().getResource("images/"+MARKDOWN_NAME);
		imageFile = new File(url.getFile().replaceAll("%20", " "));
		imageFileMD5 = MD5ChecksumHelper.getMD5Checksum(imageFile);
		imageFileTwo = new File(url2.getFile().replaceAll("%20", " "));
		markdownFile = new File(markdownUrl.getFile().replaceAll("%20", " "));
		
		assertNotNull(imageFile);
		assertTrue(imageFile.exists());
		assertNotNull(imageFileTwo);
		assertTrue(imageFileTwo.exists());
		assertNotNull(markdownFile);
		assertTrue(markdownFile.exists());
		
		// Create a project, this will own the wiki page.
		project = new Project();
		project = synapse.createEntity(project);

		// Create the file handles
		List<File> list = new LinkedList<File>();
		list.add(imageFile);
		list.add(imageFileTwo);
		list.add(markdownFile);
		fileHandle = synapse.multipartUpload(imageFile, null, true, false);
		fileHandleTwo = synapse.multipartUpload(imageFileTwo, null, true, false);
		markdownHandle = synapse.multipartUpload(markdownFile, null, false, false);
		
		// create the access requirement
		accessRequirement = new TermsOfUseAccessRequirement();
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(project.getId());
		rod.setType(RestrictableObjectType.ENTITY);
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD);
		accessRequirement.setSubjectIds(Collections.singletonList(rod));
		accessRequirement = adminSynapse.createAccessRequirement(accessRequirement);
	}
	
	@After
	public void after() throws Exception {
		if(project != null){
			synapse.deleteAndPurgeEntity(project);
		}
		for(WikiPageKey key: toDelete){
			adminSynapse.deleteV2WikiPage(key);
		}
		if (accessRequirement != null) {
			try {
				adminSynapse.deleteAccessRequirement(accessRequirement.getId());
			} catch (Exception e) {
				// continue
			}
		}
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
		for(String id: fileHandlesToDelete) {
			synapse.deleteFileHandle(id);
		}
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		adminSynapse.deleteUser(userToDelete);
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
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(project.getId(), ObjectType.ENTITY, wiki.getId());
		toDelete.add(key);
		Date firstModifiedOn = wiki.getModifiedOn();
		
		// Add another file attachment and change the title and update the wiki
		wiki.getAttachmentFileHandleIds().add(fileHandleTwo.getId());
		wiki.setTitle("Updated ITV2WikiPageTest");
		wiki = synapse.updateV2WikiPage(key.getOwnerObjectId(), key.getOwnerObjectType(), wiki);
		assertNotNull(wiki);
		assertNotNull(wiki.getAttachmentFileHandleIds());
		assertEquals(2, wiki.getAttachmentFileHandleIds().size());
		assertEquals(fileHandle.getId(), wiki.getAttachmentFileHandleIds().get(0));
		assertEquals(fileHandleTwo.getId(), wiki.getAttachmentFileHandleIds().get(1));
		assertEquals(markdownHandle.getId(), wiki.getMarkdownFileHandleId());
		assertEquals("Updated ITV2WikiPageTest", wiki.getTitle());
		assertTrue(!wiki.getModifiedOn().equals(firstModifiedOn));
		
		// test get
		wiki = synapse.getV2WikiPage(key);
		V2WikiPage root = synapse.getV2RootWikiPage(project.getId(), ObjectType.ENTITY);
		assertEquals(wiki, root);
		
		// get markdown file
		String markdown = synapse.downloadV2WikiMarkdown(key);
		assertNotNull(markdown);
		String oldMarkdown = synapse.downloadVersionOfV2WikiMarkdown(key, new Long(0));
		assertNotNull(oldMarkdown);
		
		// Get the tree
		PaginatedResults<V2WikiHeader> tree = synapse.getV2WikiHeaderTree(key.getOwnerObjectId(), key.getOwnerObjectType(), 50L, 0L);
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
		wiki = synapse.restoreV2WikiPage(key.getOwnerObjectId(), key.getOwnerObjectType(), key.getWikiPageId(), new Long(versionToRestore));
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
		assertEquals("ITV2WikiPageTest.testWikiRoundTrip", wiki.getTitle());
		
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
	public void testAccessRequirementV1WikiRoundTrip() throws SynapseException, IOException, InterruptedException, JSONObjectAdapterException{
		WikiPage wiki = new WikiPage();
		wiki.setAttachmentFileHandleIds(new ArrayList<String>());
		wiki.getAttachmentFileHandleIds().add(fileHandle.getId());
		String markdownText1 = "markdown text one";
		wiki.setMarkdown(markdownText1);
		wiki.setTitle("ITV2WikiPageTest.testAccessRequirementWikiRoundTrip");
		// Create a V2WikiPage
		wiki = adminSynapse.createWikiPage(accessRequirement.getId().toString(), ObjectType.ACCESS_REQUIREMENT, wiki);
		assertNotNull(wiki);
		WikiPageKey key = adminSynapse.getRootWikiPageKey(accessRequirement.getId().toString(), ObjectType.ACCESS_REQUIREMENT);
		toDelete.add(key);
		assertEquals(markdownText1, wiki.getMarkdown());
		
		// update
		String markdownText2 = "markdown text two";
		wiki.setMarkdown(markdownText2);
		wiki = adminSynapse.updateWikiPage(accessRequirement.getId().toString(), ObjectType.ACCESS_REQUIREMENT, wiki);
		assertNotNull(wiki);
		
		// Get a version of the wiki
		WikiPage current = adminSynapse.getWikiPage(key);
		assertEquals(markdownText2, current.getMarkdown());
		
		// get the previous version
		WikiPage old = adminSynapse.getWikiPageForVersion(key, 0L);
		assertEquals(markdownText1, old.getMarkdown());
	}
	
	@Test
	public void testAccessRequirementV2WikiRoundTrip() throws SynapseException, IOException, InterruptedException, JSONObjectAdapterException{
		V2WikiPage wiki = new V2WikiPage();
		wiki.setAttachmentFileHandleIds(new ArrayList<String>());
		wiki.getAttachmentFileHandleIds().add(fileHandle.getId());
		wiki.setMarkdownFileHandleId(markdownHandle.getId());
		wiki.setTitle("ITV2WikiPageTest.testAccessRequirementWikiRoundTrip");
		// Create a V2WikiPage
		wiki = adminSynapse.createV2WikiPage(accessRequirement.getId().toString(), ObjectType.ACCESS_REQUIREMENT, wiki);
		assertNotNull(wiki);
		assertNotNull(wiki.getAttachmentFileHandleIds());
		assertEquals(1, wiki.getAttachmentFileHandleIds().size());
		assertEquals(fileHandle.getId(), wiki.getAttachmentFileHandleIds().get(0));
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(accessRequirement.getId().toString(), ObjectType.ACCESS_REQUIREMENT, wiki.getId());
		toDelete.add(key);
		Date firstModifiedOn = wiki.getModifiedOn();
		
		// Add another file attachment and change the title and update the wiki
		wiki.getAttachmentFileHandleIds().add(fileHandleTwo.getId());
		wiki.setTitle("Updated ITV2AccessRequirementWikiPageTest");
		wiki = adminSynapse.updateV2WikiPage(key.getOwnerObjectId(), key.getOwnerObjectType(), wiki);
		assertNotNull(wiki);
		assertNotNull(wiki.getAttachmentFileHandleIds());
		assertEquals(2, wiki.getAttachmentFileHandleIds().size());
		assertEquals(fileHandle.getId(), wiki.getAttachmentFileHandleIds().get(0));
		assertEquals(fileHandleTwo.getId(), wiki.getAttachmentFileHandleIds().get(1));
		assertEquals(markdownHandle.getId(), wiki.getMarkdownFileHandleId());
		assertEquals("Updated ITV2AccessRequirementWikiPageTest", wiki.getTitle());
		assertTrue(!wiki.getModifiedOn().equals(firstModifiedOn));
		
		// test get
		wiki = synapse.getV2WikiPage(key);
		V2WikiPage root = synapse.getV2RootWikiPage(accessRequirement.getId().toString(), ObjectType.ACCESS_REQUIREMENT);
		assertEquals(wiki, root);
		
		// get markdown file
		String markdown = synapse.downloadV2WikiMarkdown(key);
		assertNotNull(markdown);
		String oldMarkdown = synapse.downloadVersionOfV2WikiMarkdown(key, new Long(0));
		assertNotNull(oldMarkdown);
		
		// Get the tree
		PaginatedResults<V2WikiHeader> tree = synapse.getV2WikiHeaderTree(key.getOwnerObjectId(), key.getOwnerObjectType(), 50L, 0L);
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
		wiki = adminSynapse.restoreV2WikiPage(key.getOwnerObjectId(), key.getOwnerObjectType(), key.getWikiPageId(), new Long(versionToRestore));
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
		assertEquals("ITV2WikiPageTest.testAccessRequirementWikiRoundTrip", wiki.getTitle());
		
		// Delete the wiki
		adminSynapse.deleteV2WikiPage(key);
		// Now try to get it
		try{
			synapse.getV2WikiPage(key);
			fail("This should have failed as the wiki was deleted");
		}catch(SynapseException e){
			// expected;
		}
	}
	
	@Test
	public void testV2MethodsWithV1Models() throws IOException, SynapseException, JSONObjectAdapterException {
		// Create V1 Wiki
		WikiPage wiki = new WikiPage();
		wiki.setAttachmentFileHandleIds(new ArrayList<String>());
		wiki.getAttachmentFileHandleIds().add(fileHandle.getId());
		wiki.setMarkdown("markdown");
		wiki.setTitle("ITV2WikiPageTest");
		wiki = synapse.createWikiPage(project.getId(), ObjectType.ENTITY, wiki);
		assertNotNull(wiki);
		assertNotNull(wiki.getAttachmentFileHandleIds());
		assertEquals(1, wiki.getAttachmentFileHandleIds().size());
		assertEquals(fileHandle.getId(), wiki.getAttachmentFileHandleIds().get(0));
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(project.getId(), ObjectType.ENTITY, wiki.getId());
		toDelete.add(key);
		// Store file handle before updating to delete later
		V2WikiPage wikiV2Clone = synapse.getV2WikiPage(key);
		fileHandlesToDelete.add(wikiV2Clone.getMarkdownFileHandleId());
		Date firstModifiedOn = wiki.getModifiedOn();
		
		// Add another file attachment and update the wiki
		wiki.getAttachmentFileHandleIds().add(fileHandleTwo.getId());
		wiki.setMarkdown("updated markdown");
		wiki = synapse.updateWikiPage(key.getOwnerObjectId(), key.getOwnerObjectType(), wiki);
		assertNotNull(wiki);
		assertNotNull(wiki.getAttachmentFileHandleIds());
		assertEquals(2, wiki.getAttachmentFileHandleIds().size());
		assertTrue(!wiki.getModifiedOn().equals(firstModifiedOn));
		assertEquals("updated markdown", wiki.getMarkdown());
		// Store file handle that was created during update for deletion
		V2WikiPage updatedWikiV2Clone = synapse.getV2WikiPage(key);
		fileHandlesToDelete.add(updatedWikiV2Clone.getMarkdownFileHandleId());
		
		// test get
		wiki = synapse.getWikiPage(key);
		V2WikiPage root = synapse.getV2RootWikiPage(project.getId(), ObjectType.ENTITY);
		// this root is in the V2 model, but should have all the same fields as "wiki"
		assertEquals(root.getAttachmentFileHandleIds().size(), wiki.getAttachmentFileHandleIds().size());
		String markdown = synapse.downloadV2WikiMarkdown(key);
		assertEquals(markdown, wiki.getMarkdown());
		// test get first version
		WikiPage firstWiki = synapse.getWikiPageForVersion(key, new Long(0));
		assertNotNull(firstWiki.getAttachmentFileHandleIds());
		assertEquals(1, firstWiki.getAttachmentFileHandleIds().size());
		assertEquals("markdown", firstWiki.getMarkdown());
		
		WikiPageKey keyLookup = synapse.getRootWikiPageKey(project.getId(), ObjectType.ENTITY);
		assertEquals(key, keyLookup);
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
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(project.getId(), ObjectType.ENTITY, wiki.getId());
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
		assertTrue(two instanceof S3FileHandle);
		assertTrue(((S3FileHandle) two).getIsPreview());
		S3FileHandle preview = (S3FileHandle) two;
		assertTrue(handle.getPreviewId().equals(preview.getId()));
		
		URL url = synapse.getV2WikiAttachmentTemporaryUrl(key, handle.getFileName());
		assertNotNull(url);
		File target = File.createTempFile("test", null);
		target.deleteOnExit();
		synapse.downloadV2WikiAttachment(key, handle.getFileName(), target);
		assertEquals(imageFileMD5, MD5ChecksumHelper.getMD5Checksum(target));
		
		url = synapse.getVersionOfV2WikiAttachmentTemporaryUrl(key, handle.getFileName(), 0L);
		assertNotNull(url);
		synapse.downloadVersionOfV2WikiAttachment(key, handle.getFileName(), 0L, target);
		assertEquals(imageFileMD5, MD5ChecksumHelper.getMD5Checksum(target));
		
		url = synapse.getV2WikiAttachmentPreviewTemporaryUrl(key, handle.getFileName());
		assertNotNull(url);
		synapse.downloadV2WikiAttachmentPreview(key, handle.getFileName(), target);
		assertTrue(target.length()>0);
		
		url = synapse.getVersionOfV2WikiAttachmentPreviewTemporaryUrl(key, handle.getFileName(), 0L);
		assertNotNull(url);
		synapse.downloadVersionOfV2WikiAttachmentPreview(key, handle.getFileName(), 0L, target);
		assertTrue(target.length()>0);
		
		// Update wiki with another file handle
		wiki.getAttachmentFileHandleIds().add(fileHandleTwo.getId());
		wiki = synapse.updateV2WikiPage(key.getOwnerObjectId(), key.getOwnerObjectType(), wiki);
		
		// Since we expect the preview file handle to be returned we need to wait for it.
		waitForPreviewToBeCreated(fileHandleTwo);
		
		assertNotNull(wiki);
		assertNotNull(wiki.getAttachmentFileHandleIds());
		assertEquals(2, wiki.getAttachmentFileHandleIds().size());
		FileHandleResults resultsUpdated = synapse.getV2WikiAttachmentHandles(key);
		assertEquals(4, resultsUpdated.getList().size());
		// Getting first version file handles should return two.
		FileHandleResults oldResults = synapse.getVersionOfV2WikiAttachmentHandles(key, new Long(0));
		assertEquals(2, oldResults.getList().size());
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
	
	@Test
	public void testGetV2WikiOrderHint() throws Exception{
		V2WikiPage wiki = new V2WikiPage();
		wiki.setAttachmentFileHandleIds(new ArrayList<String>());
		wiki.getAttachmentFileHandleIds().add(fileHandle.getId());
		wiki.setMarkdownFileHandleId(markdownHandle.getId());
		wiki.setTitle("ITV2WikiPageTest.testGetV2WikiOrderHint");
		// Create a V2WikiPage
		wiki = synapse.createV2WikiPage(project.getId(), ObjectType.ENTITY, wiki);
		assertNotNull(wiki);
		assertNotNull(wiki.getAttachmentFileHandleIds());
		assertEquals(1, wiki.getAttachmentFileHandleIds().size());
		assertEquals(fileHandle.getId(), wiki.getAttachmentFileHandleIds().get(0));
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(project.getId(), ObjectType.ENTITY, wiki.getId());
		toDelete.add(key);
		
		// test get order hint
		V2WikiOrderHint hint = synapse.getV2OrderHint(key);
		assertNotNull(hint);
		assertNotNull(hint.getOwnerId().equals(project.getId()));
		assertTrue(hint.getOwnerObjectType().equals(ObjectType.ENTITY));
		assertNull(hint.getIdList());	// Should be null by default
	}
	
	@Test
	public void testUpdateV2WikiOrderHintRoundTrip() throws Exception{
		V2WikiPage wiki = new V2WikiPage();
		wiki.setAttachmentFileHandleIds(new ArrayList<String>());
		wiki.getAttachmentFileHandleIds().add(fileHandle.getId());
		wiki.setMarkdownFileHandleId(markdownHandle.getId());
		wiki.setTitle("ITV2WikiPageTest.testUpdateV2WikiOrderHint");
		// Create a V2WikiPage
		wiki = synapse.createV2WikiPage(project.getId(), ObjectType.ENTITY, wiki);
		assertNotNull(wiki);
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(project.getId(), ObjectType.ENTITY, wiki.getId());
		toDelete.add(key);
		
		V2WikiOrderHint hint = synapse.getV2OrderHint(key);
		
		// Update order hint
		List<String> hintIdList = Arrays.asList(new String[] {"A", "X", "B", "Y", "C", "Z"});
		hint.setIdList(hintIdList);
		
		V2WikiOrderHint updatedHint = synapse.updateV2WikiOrderHint(hint);
		
		assertNotNull(updatedHint);
		assertNotNull(updatedHint.getOwnerId().equals(project.getId()));
		assertFalse(updatedHint.getEtag().equals(hint.getEtag()));
		assertTrue(updatedHint.getOwnerObjectType().equals(ObjectType.ENTITY));
		assertTrue(Arrays.equals(updatedHint.getIdList().toArray(), hintIdList.toArray()));
		
		V2WikiOrderHint postUpdateHint = synapse.getV2OrderHint(key);
		assertTrue(updatedHint.getOwnerId().equals(postUpdateHint.getOwnerId()));
		assertTrue(updatedHint.getOwnerObjectType().equals(postUpdateHint.getOwnerObjectType()));
		assertTrue(Arrays.equals(updatedHint.getIdList().toArray(), postUpdateHint.getIdList().toArray()));
	}
}
