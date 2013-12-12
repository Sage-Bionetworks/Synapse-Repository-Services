package org.sagebionetworks.repo.manager.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.WikiPageDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentFields;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.AdapterFactoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author deflaux
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class SearchDocumentDriverImplAutowireTest {

	@Autowired
	private SearchDocumentDriver searchDocumentDriver;

	@Autowired
	private UserManager userManager;
	
	@Autowired
	private EntityManager entityManager;
	
	@Autowired
	private WikiPageDao wikiPageDao;
	
	private UserInfo adminUserInfo;
	private Project project;
	private WikiPage rootPage;
	private WikiPageKey rootKey;
	private WikiPage subPage;
	private WikiPageKey subPageKey;
	
	@Before
	public void before() throws Exception {
		// To satisfy some FKs
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		// Create a project
		project = new Project();
		project.setName("SearchDocumentDriverImplAutowireTest");
		project.setDescription("projectDescription");
		
		String projectId = entityManager.createEntity(adminUserInfo, project, null);
		project = entityManager.getEntity(adminUserInfo, projectId, Project.class);
		// Add two wikiPages to the entity
		rootPage = createWikiPage();
		rootPage.setTitle("rootTile");
		rootPage.setMarkdown("rootMarkdown");
		rootPage = wikiPageDao.create(rootPage, new HashMap<String, FileHandle>(), project.getId(), ObjectType.ENTITY);
		rootKey = new WikiPageKey(project.getId(), ObjectType.ENTITY, rootPage.getId());
		// Add a sub-page;
		subPage = createWikiPage();
		subPage.setTitle("subTitle");
		subPage.setMarkdown("subMarkdown");
		subPage.setParentWikiId(rootPage.getId());
		subPage = wikiPageDao.create(subPage, new HashMap<String, FileHandle>(), project.getId(), ObjectType.ENTITY);
		subPageKey = new WikiPageKey(project.getId(), ObjectType.ENTITY, subPage.getId());
	}
	
	private WikiPage createWikiPage(){
		WikiPage page = new  WikiPage();
		page.setTitle("rootTile");
		page.setMarkdown("rootMarkdown");
		page.setCreatedBy(adminUserInfo.getIndividualGroup().getId());
		page.setCreatedOn(new Date());
		page.setModifiedBy(page.getCreatedBy());
		page.setModifiedOn(page.getCreatedOn());
		page.setEtag("Etag");
		return page;
	}

	/**
	 * @throws NotFoundException 
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * 
	 */
	@After
	public void after() throws DatastoreException, UnauthorizedException, NotFoundException {
		if(subPageKey != null){
			wikiPageDao.delete(subPageKey);
		}
		if(rootKey != null){
			wikiPageDao.delete(rootKey);
		}
		if(project != null){
			entityManager.deleteEntity(adminUserInfo, project.getId());
		}
	}

	/**
	 * All non-blob annotations belong in the free text annotations field, some
	 * are _also_ facets in the search index
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAnnotations() throws Exception {

		Node node = new Node();
		node.setId("5678");
		node.setParentId("1234");
		node.setETag("0");
		node.setNodeType(EntityType.dataset.name());
		node.setDescription("Test annotations");
		Long nonexistantPrincipalId = 42L;
		node.setCreatedByPrincipalId(nonexistantPrincipalId);
		node.setCreatedOn(new Date());
		node.setModifiedByPrincipalId(nonexistantPrincipalId);
		node.setModifiedOn(new Date());
		node.setVersionLabel("versionLabel");
		NamedAnnotations named = new NamedAnnotations();
		Annotations primaryAnnos = named.getAdditionalAnnotations();
		primaryAnnos.addAnnotation("species", "Dragon");
		primaryAnnos.addAnnotation("numSamples", 999L);
		Annotations additionalAnnos = named.getAdditionalAnnotations();
		additionalAnnos.addAnnotation("Species", "Unicorn");
		additionalAnnos
				.addAnnotation("stringKey",
						"a multi-word annotation gets underscores so we can exact-match find it");
		additionalAnnos.addAnnotation("longKey", 10L);
		additionalAnnos.addAnnotation("number_of_samples", "42");
		additionalAnnos.addAnnotation("Tissue_Tumor", "ear lobe");
		additionalAnnos.addAnnotation("platform", "synapse");
		Date dateValue = new Date();
		additionalAnnos.addAnnotation("dateKey", dateValue);
		additionalAnnos
				.addAnnotation("blobKey", new String("bytes").getBytes());
		Set<Reference> references = new HashSet<Reference>();
		Map<String, Set<Reference>> referenceMap = new HashMap<String, Set<Reference>>();
		referenceMap.put("tooMany", references);
		node.setReferences(referenceMap);
		for (int i = 0; i <= SearchDocumentDriverImpl.FIELD_VALUE_SIZE_LIMIT + 10; i++) {
			Reference ref = new Reference();
			ref.setTargetId("123" + i);
			ref.setTargetVersionNumber(1L);
			references.add(ref);
		}
		
		String wikiPageText = "title\nmarkdown";

		Set<ACCESS_TYPE> rwAccessType = new HashSet<ACCESS_TYPE>();
		rwAccessType.add(ACCESS_TYPE.READ);
		rwAccessType.add(ACCESS_TYPE.UPDATE);
		ResourceAccess rwResourceAccess = new ResourceAccess();
		rwResourceAccess.setPrincipalId(123L); //readWriteTest@sagebase.org
		rwResourceAccess.setAccessType(rwAccessType);
		Set<ACCESS_TYPE> roAccessType = new HashSet<ACCESS_TYPE>();
		roAccessType.add(ACCESS_TYPE.READ);
		ResourceAccess roResourceAccess = new ResourceAccess();
		roResourceAccess.setPrincipalId(456L); // readOnlyTest@sagebase.org
		roResourceAccess.setAccessType(roAccessType);

		Set<ResourceAccess> resourceAccesses = new HashSet<ResourceAccess>();
		resourceAccesses.add(rwResourceAccess);
		resourceAccesses.add(roResourceAccess);
		AccessControlList acl = new AccessControlList();
		acl.setResourceAccess(resourceAccesses);

		EntityPath fakeEntityPath = createFakeEntityPath();
		AdapterFactoryImpl adapterFactoryImpl = new AdapterFactoryImpl();
		JSONObjectAdapter adapter = adapterFactoryImpl.createNew();
		fakeEntityPath.writeToJSONObject(adapter);		
		String fakeEntityPathJSONString = adapter.toJSONString();
		Document document = searchDocumentDriver.formulateSearchDocument(node,
				named, acl, fakeEntityPath, wikiPageText);
		assertEquals(DocumentTypeNames.add, document.getType());
		assertEquals("en", document.getLang());
		assertEquals(node.getId(), document.getId());
		assertTrue(0 < document.getVersion());

		DocumentFields fields = document.getFields();

		// Check entity property fields
		assertEquals(node.getId(), fields.getId());
		assertEquals(node.getETag(), fields.getEtag());
		assertEquals(node.getParentId(), fields.getParent_id());
		assertEquals(node.getName(), fields.getName());
		
		assertEquals("study", fields.getNode_type());
		assertEquals(node.getDescription()+wikiPageText, fields.getDescription());
		// since the Principal doesn't exist, the 'created by' display name defaults to the principal ID
		assertEquals(""+nonexistantPrincipalId, fields.getCreated_by());
		assertEquals(new Long(node.getCreatedOn().getTime() / 1000), fields
				.getCreated_on());
		// since the Principal doesn't exist, the 'modified by' display name defaults to the principal ID
		assertEquals(""+nonexistantPrincipalId, fields.getModified_by());
		assertEquals(new Long(node.getModifiedOn().getTime() / 1000), fields
				.getModified_on());

		// Check boost field
		assertTrue(fields.getBoost().contains(node.getId()));
		assertTrue(fields.getBoost().contains(node.getName()));

		// Check the faceted fields
		assertEquals(2, fields.getNum_samples().size());
		assertEquals(new Long(42), fields.getNum_samples().get(0));
		assertEquals(new Long(999), fields.getNum_samples().get(1));
		assertEquals(2, fields.getSpecies().size());
		assertEquals("Dragon", fields.getSpecies().get(0));
		assertEquals("Unicorn", fields.getSpecies().get(1));
		assertEquals("ear lobe", fields.getTissue().get(0));
		assertEquals("synapse", fields.getPlatform().get(0));

		// Check ACL fields
		assertEquals(2, fields.getAcl().size());
		assertEquals(1, fields.getUpdate_acl().size());

		// Make sure our references were trimmed
		assertTrue(10 < fields.getReferences().size());
		assertEquals(SearchDocumentDriverImpl.FIELD_VALUE_SIZE_LIMIT, fields
				.getReferences().size());
	}

	private EntityPath createFakeEntityPath() {
		List<EntityHeader> fakePath = new ArrayList<EntityHeader>();
		EntityHeader eh1 = new EntityHeader();
		eh1.setId("1");
		eh1.setName("One");
		eh1.setType("type");
		fakePath.add(eh1);
		eh1 = new EntityHeader();
		eh1.setId("2");
		eh1.setName("Two");
		eh1.setType("type");
		fakePath.add(eh1);
		EntityPath fakeEntityPath = new EntityPath();
		fakeEntityPath.setPath(fakePath);
		return fakeEntityPath;
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testCleanOutControlCharacters() throws Exception {
		// Cloud Search cannot handle control characters, strip them out of the
		// search document
		Node node = new Node();
		node.setId("5678");
		node.setParentId("1234");
		node.setETag("0");
		node.setNodeType(EntityType.step.name());
		node
				.setDescription("For the microarray experiments, MV4-11 and MOLM-14 ... Midi Kit, according to the manufacturer\u0019s instruction (Qiagen, Valencia, USA).");
		Long nonexistantPrincipalId = 42L;
		node.setCreatedByPrincipalId(nonexistantPrincipalId);
		node.setCreatedOn(new Date());
		node.setModifiedByPrincipalId(nonexistantPrincipalId);
		node.setModifiedOn(new Date());
		NamedAnnotations named = new NamedAnnotations();
		Annotations primaryAnnos = named.getAdditionalAnnotations();
		primaryAnnos.addAnnotation("stringKey", "a");
		primaryAnnos.addAnnotation("longKey", Long.MAX_VALUE);
		Annotations additionalAnnos = named.getAdditionalAnnotations();
		additionalAnnos.addAnnotation("stringKey", "a");
		additionalAnnos.addAnnotation("longKey", Long.MAX_VALUE);
		AccessControlList acl = new AccessControlList();
		Set<ResourceAccess> resourceAccess = new HashSet<ResourceAccess>();
		acl.setResourceAccess(resourceAccess);
		Document document = searchDocumentDriver.formulateSearchDocument(node,
				named, acl, new EntityPath(), null);
		byte[] cloudSearchDocument = SearchDocumentDriverImpl
				.cleanSearchDocument(document);
		assertEquals(-1, new String(cloudSearchDocument).indexOf("\\u0019"));
	}
	
	@Test
	public void testGetAllWikiPageText(){
		// The expected text fo
		StringBuilder expected = new StringBuilder();
		expected.append("\n");
		expected.append(rootPage.getTitle());
		expected.append("\n");
		expected.append(rootPage.getMarkdown());
		expected.append("\n");
		expected.append(subPage.getTitle());
		expected.append("\n");
		expected.append(subPage.getMarkdown());
		// Now get the text from the d
		String resultText = searchDocumentDriver.getAllWikiPageText(project.getId());
		assertNotNull(resultText);
		assertEquals(expected.toString(), resultText);
		// Should get a null for a bogus node id
		resultText = searchDocumentDriver.getAllWikiPageText("-123");
		assertEquals(null, resultText);
	}


	// http://stackoverflow.com/questions/326390/how-to-create-a-java-string-from-the-contents-of-a-file
	private static String readFile(File file) throws IOException {
		FileInputStream stream = new FileInputStream(file);
		try {
			FileChannel fc = stream.getChannel();
			MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc
					.size());
			/* Instead of using default, pass in a decoder. */
			return Charset.forName("UTF-8").decode(bb).toString();
		} finally {
			stream.close();
		}
	}

}
