package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.List;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.repo.model.ontology.Concept;
import org.sagebionetworks.repo.model.ontology.ConceptResponsePage;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

public class ConceptControllerTest extends AbstractAutowiredControllerTestBase {
	static final String MOTILE_CELL_CONCEPT_ID = "11328";
	static final String ADRENAL_MEDULLA_CELL_CONCEPT_ID = "398";
	final MemoryMXBean memBean = ManagementFactory.getMemoryMXBean(); 
	private static final int MB_PER_BYTE = 1024*1024;
	
	// Start memory
	long startMemoryMB;
	
	@Before
	public void before() throws Exception{
		startMemoryMB = memBean.getHeapMemoryUsage().getUsed()/MB_PER_BYTE;
	}
	
	@After
	public void after(){
		long endMemoryMB = memBean.getHeapMemoryUsage().getUsed()/MB_PER_BYTE;
		System.out.println("Memory used = "+(endMemoryMB-startMemoryMB)+" mb");
	}
	
	@Test
	public void testGetSummaryNoFilter() throws Exception {
		String parentUrl = "http://synapse.sagebase.org/ontology#"+MOTILE_CELL_CONCEPT_ID;
		ConceptResponsePage response = servletTestHelper.getConceptsForParent(MOTILE_CELL_CONCEPT_ID, null, 10, 0);
		assertNotNull(response);
		assertEquals(parentUrl, response.getParentConceptUri());
		List<Concept> children = response.getChildren();
		assertNotNull(children);
		assertTrue(response.getTotalNumberOfResults().longValue() > 50);
		assertEquals(10, children.size());
		
		// Test paging
		Concept fourthConcept = children.get(3);
		response = servletTestHelper.getConceptsForParent(MOTILE_CELL_CONCEPT_ID, null, 1, 3);
		assertNotNull(response);
		assertEquals(parentUrl, response.getParentConceptUri());
		children = response.getChildren();
		assertNotNull(children);
		assertTrue(response.getTotalNumberOfResults().longValue() > 50);
		assertEquals(1, children.size());
		assertEquals(fourthConcept, children.get(0));
	}
	
	
	
	@Test
	public void testGetSummaryWithFilter() throws Exception {
		String parentId = "8406";
		String parentUrl = "http://synapse.sagebase.org/ontology#"+parentId;
		String prefix = "adrenal medulla cell";
		ConceptResponsePage response = servletTestHelper.getConceptsForParent(parentId, prefix, Integer.MAX_VALUE, 0);
		assertNotNull(response);
		assertEquals(parentUrl, response.getParentConceptUri());
		assertEquals(parentUrl, response.getParentConceptUri());
		List<Concept> children = response.getChildren();
		assertNotNull(children);
		assertEquals(1, children.size());
		assertEquals("adrenal medulla cell", children.get(0).getPreferredLabel());
		
	}
	
	@Test
	public void testGetConcept() throws Exception {
		String conceptUrl = "http://synapse.sagebase.org/ontology#"+MOTILE_CELL_CONCEPT_ID;
		Concept response = servletTestHelper.getConcept(MOTILE_CELL_CONCEPT_ID);
		assertNotNull(response);
		assertEquals(conceptUrl, response.getUri());
	}
	
	/**
	 * Make sure we can get a concept as JSONP
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException 
	 */
	@Ignore // This does not seem to work as a filter.
	@Test
	public void testGetConceptJSONP() throws Exception , JSONObjectAdapterException{
		String conceptUrl = "http://synapse.sagebase.org/ontology#"+ADRENAL_MEDULLA_CELL_CONCEPT_ID;
		String callbackName = "exampleCallback";
		String response = servletTestHelper.getConceptAsJSONP(ADRENAL_MEDULLA_CELL_CONCEPT_ID, callbackName);
		assertNotNull(response);
		System.out.println(response);
		String expectedPrefix = callbackName+"(";
		assertTrue(response.startsWith(expectedPrefix));
		assertTrue(response.endsWith(")"));
		// Extract the JSON from the JSONP
		String extractedJson = response.substring(expectedPrefix.length(), response.length()-1);
		Concept fromJSONP = EntityFactory.createEntityFromJSONString(extractedJson, Concept.class);
		assertEquals(conceptUrl, fromJSONP.getUri());
	}
}
