package org.sagebionetworks.repo.manager.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.ResourceAccess;

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
		node.setDescription("Test annotations");
		node.setCreatedBy("test");
		node.setCreatedOn(new Date());
		node.setModifiedBy("test");
		node.setModifiedOn(new Date());

		NodeRevisionBackup rev = new NodeRevisionBackup();
		NamedAnnotations named = new NamedAnnotations();
		Annotations primaryAnnos = named.getAdditionalAnnotations();
		primaryAnnos.addAnnotation("Species", "Unicorn");
		primaryAnnos.addAnnotation("numSamples", 999L);
		Annotations additionalAnnos = named.getAdditionalAnnotations();
		additionalAnnos.addAnnotation("stringKey", "a multi-word annotation gets underscores so we can exact-match find it");
		additionalAnnos.addAnnotation("longKey", 10L);
		additionalAnnos.addAnnotation("dateKey", new Date());
		additionalAnnos.addAnnotation("blobKey", new String("bytes").getBytes());
		rev.setNamedAnnotations(named);

		AccessControlList acl = new AccessControlList();
		Set<ResourceAccess> resourceAccess = new HashSet<ResourceAccess>();
		acl.setResourceAccess(resourceAccess);

		JSONObject document = SearchDocumentDriverImpl.formulateSearchDocument(
				node, rev, acl);

		// Check the facted fields
		assertTrue(document.getJSONObject("fields").has("num_samples"));
		assertEquals(999, document.getJSONObject("fields").getJSONArray(
				"num_samples").getInt(0));
		assertTrue(document.getJSONObject("fields").has("species"));
		assertEquals("Unicorn", document.getJSONObject("fields").getJSONArray(
				"species").getString(0));
		assertTrue(document.getJSONObject("fields").has("annotations"));

		// Check the free text annotations
		JSONArray annotations = document.getJSONObject("fields").getJSONArray(
				"annotations");
		String joinedAnnotations = annotations.join(" ");
		assertTrue(-1 < joinedAnnotations.indexOf("numSamples:999"));
		assertTrue(-1 < joinedAnnotations.indexOf("Species:Unicorn"));
		assertTrue(-1 < joinedAnnotations.indexOf("stringKey:a_multi-word_annotation_gets_underscores_so_we_can_exact-match_find_it"));
		assertTrue(-1 < joinedAnnotations.indexOf("longKey:10"));
		assertTrue(-1 < joinedAnnotations.indexOf("dateKey:"));
		assertTrue(-1 == joinedAnnotations.indexOf("blobKey:"));		
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testCleanOutControlCharacters() throws Exception {

		// Cloud Search cannot handle control characters, strip them out of the
		// search document

		Node node = new Node();
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

		JSONObject document = SearchDocumentDriverImpl.formulateSearchDocument(
				node, rev, acl);
		byte[] cloudSearchDocument = SearchDocumentDriverImpl
				.convertToCloudSearchDocument(document);

		assertEquals(-1, new String(cloudSearchDocument).indexOf("\\u0019"));
	}

}
