package org.sagebionetworks.repo.manager.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentFields;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;

/**
 * @author deflaux
 * 
 */
public class SearchDocumentDriverImplTest {

	/**
	 * @throws Exception
	 */
	@Before
	public void before() throws Exception {
	}

	/**
	 * 
	 */
	@After
	public void after() {
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testAnnotations() throws Exception {

		// All non-blob annotations belong in the free text annotations field,
		// some are _also_ facets in the search index

		Node node = new Node();
		node.setId("5678");
		node.setParentId("1234");
		node.setETag("0");
		node.setDescription("Test annotations");
		node.setCreatedBy("test");
		node.setCreatedOn(new Date());
		node.setModifiedBy("test");
		node.setModifiedOn(new Date());
		node.setVersionLabel("versionLabel");

		NodeRevisionBackup rev = new NodeRevisionBackup();
		NamedAnnotations named = new NamedAnnotations();
		Annotations primaryAnnos = named.getAdditionalAnnotations();
		primaryAnnos.addAnnotation("species", "Dragon");
		primaryAnnos.addAnnotation("numSamples", 999L);
		Annotations additionalAnnos = named.getAdditionalAnnotations();
		additionalAnnos.addAnnotation("Species", "Unicorn");
		additionalAnnos.addAnnotation("stringKey", "a multi-word annotation gets underscores so we can exact-match find it");
		additionalAnnos.addAnnotation("longKey", 10L);
		additionalAnnos.addAnnotation("number_of_samples", "42");
		additionalAnnos.addAnnotation("Tissue_Tumor", "ear lobe");
		additionalAnnos.addAnnotation("platform", "synapse");
		Date dateValue = new Date();
		additionalAnnos.addAnnotation("dateKey", dateValue);
		additionalAnnos.addAnnotation("blobKey", new String("bytes").getBytes());
		rev.setNamedAnnotations(named);

		Set<Reference> references = new HashSet<Reference>();
		Map<String, Set<Reference>> referenceMap = new HashMap<String, Set<Reference>>();
		referenceMap.put("tooMany", references);
		node.setReferences(referenceMap);
		for(int i = 0; i <= SearchDocumentDriverImpl.FIELD_VALUE_SIZE_LIMIT + 10; i++) {
			Reference ref = new Reference();
			ref.setTargetId("123" + i);
			ref.setTargetVersionNumber(1L);
			references.add(ref);
		}
		
		Set<ACCESS_TYPE> rwAccessType = new HashSet<ACCESS_TYPE>();
		rwAccessType.add(ACCESS_TYPE.READ);
		rwAccessType.add(ACCESS_TYPE.UPDATE);
		ResourceAccess rwResourceAccess = new ResourceAccess();
		rwResourceAccess.setGroupName("readWriteTest@sagebase.org");
		rwResourceAccess.setAccessType(rwAccessType);

		Set<ACCESS_TYPE> roAccessType = new HashSet<ACCESS_TYPE>();
		roAccessType.add(ACCESS_TYPE.READ);
		ResourceAccess roResourceAccess = new ResourceAccess();
		roResourceAccess.setGroupName("readOnlyTest@sagebase.org");
		roResourceAccess.setAccessType(roAccessType);
	
		Set<ResourceAccess> resourceAccesses = new HashSet<ResourceAccess>();
		resourceAccesses.add(rwResourceAccess);
		resourceAccesses.add(roResourceAccess);

		AccessControlList acl = new AccessControlList();
		acl.setResourceAccess(resourceAccesses);
		
		Document document = SearchDocumentDriverImpl.formulateSearchDocument(
				node, rev, acl);
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
		assertEquals(node.getNodeType(), fields.getNode_type());
		assertEquals(node.getDescription(), fields.getDescription());
		assertEquals(node.getVersionLabel(), fields.getVersion_label());
		assertEquals(node.getCreatedBy(), fields.getCreated_by());
		assertEquals(new Long(node.getCreatedOn().getTime() / 1000), fields.getCreated_on());
		assertEquals(node.getModifiedBy(), fields.getModified_by());
		assertEquals(new Long(node.getModifiedOn().getTime() / 1000), fields.getModified_on());
		
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
		assertEquals(SearchDocumentDriverImpl.FIELD_VALUE_SIZE_LIMIT, fields.getReferences().size());
		
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
		assertTrue(annotations.contains("stringKey:a_multi-word_annotation_gets_underscores_so_we_can_exact-match_find_it"));
		assertTrue(annotations.contains("longKey:10"));
		assertTrue(annotations.contains("dateKey:" + dateValue.toString().replaceAll("\\s", "_")));
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
		node
				.setDescription("For the microarray experiments, MV4-11 and MOLM-14 ... Midi Kit, according to the manufacturer\u0019s instruction (Qiagen, Valencia, USA).");
		node.setCreatedBy("test");
		node.setCreatedOn(new Date());
		node.setModifiedBy("test");
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

		Document document = SearchDocumentDriverImpl.formulateSearchDocument(
				node, rev, acl);
		byte[] cloudSearchDocument = SearchDocumentDriverImpl
				.cleanSearchDocument(document);

		assertEquals(-1, new String(cloudSearchDocument).indexOf("\\u0019"));
	}

}
