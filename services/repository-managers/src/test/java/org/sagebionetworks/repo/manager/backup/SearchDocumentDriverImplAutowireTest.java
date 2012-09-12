package org.sagebionetworks.repo.manager.backup;

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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentFields;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.AdapterFactoryImpl;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
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

	private static final String TEST_USER = "foo@bar.com";
	UserInfo userInfo;
	
	@Autowired
	SearchDocumentDriver searchDocumentDriver;

	@Autowired
	UserManager userManager;
	
	/**
	 * @throws Exception
	 */
	@Before
	public void before() throws Exception {
		// This will create the user if they do not exist
		userInfo = userManager.getUserInfo(TEST_USER);
	}

	/**
	 * 
	 */
	@After
	public void after() {
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
		NodeRevisionBackup rev = new NodeRevisionBackup();
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
		rev.setNamedAnnotations(named);
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
				rev, acl, fakeEntityPath);
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
		
		assertEquals(fakeEntityPathJSONString, fields.getPath());
		assertEquals("study", fields.getNode_type());
		assertEquals(node.getDescription(), fields.getDescription());
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

		// Annotations are always of length 1
		assertEquals(1, fields.getAnnotations().size());
		// Check the free text annotations
		String annotationValues[] = fields.getAnnotations().get(0).split(" ");
		List<String> annotations = Arrays.asList(annotationValues);
		assertEquals(9, annotations.size());
		assertTrue(annotations.contains("species:Dragon"));
		assertTrue(annotations.contains("Species:Unicorn"));
		assertTrue(annotations.contains("Tissue_Tumor:ear_lobe"));
		assertTrue(annotations.contains("platform:synapse"));
		assertTrue(annotations.contains("numSamples:999"));
		assertTrue(annotations.contains("number_of_samples:42"));
		assertTrue(annotations
				.contains("stringKey:a_multi-word_annotation_gets_underscores_so_we_can_exact-match_find_it"));
		assertTrue(annotations.contains("longKey:10"));
		assertTrue(annotations.contains("dateKey:"
				+ dateValue.toString().replaceAll("\\s", "_")));
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
		NodeRevisionBackup rev = new NodeRevisionBackup();
		NamedAnnotations named = new NamedAnnotations();
		Annotations primaryAnnos = named.getAdditionalAnnotations();
		primaryAnnos.addAnnotation("stringKey", "a");
		primaryAnnos.addAnnotation("longKey", Long.MAX_VALUE);
		Annotations additionalAnnos = named.getAdditionalAnnotations();
		additionalAnnos.addAnnotation("stringKey", "a");
		additionalAnnos.addAnnotation("longKey", Long.MAX_VALUE);
		rev.setNamedAnnotations(named);
		AccessControlList acl = new AccessControlList();
		Set<ResourceAccess> resourceAccess = new HashSet<ResourceAccess>();
		acl.setResourceAccess(resourceAccess);
		Document document = searchDocumentDriver.formulateSearchDocument(node,
				rev, acl, new EntityPath());
		byte[] cloudSearchDocument = SearchDocumentDriverImpl
				.cleanSearchDocument(document);
		assertEquals(-1, new String(cloudSearchDocument).indexOf("\\u0019"));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testWriteAllSearchDocuments() throws Exception {

		File destination = File.createTempFile("foo", ".txt");
		destination.deleteOnExit();

		destination.createNewFile();
		searchDocumentDriver.writeSearchDocument(destination, new Progress(),
				null);
		assertTrue(256 < destination.length());
		String serializedSearchDocuments = readFile(destination);
		JSONArray searchDocuments = new JSONArray(serializedSearchDocuments);
		assertTrue(0 < searchDocuments.length());
		JSONObject searchDocument = searchDocuments.getJSONObject(0);
		Document document = EntityFactory.createEntityFromJSONObject(
				searchDocument, Document.class);
		assertNotNull(document);
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
