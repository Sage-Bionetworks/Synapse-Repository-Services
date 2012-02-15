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
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ontology.Concept;
import org.sagebionetworks.repo.model.ontology.ConceptResponsePage;
import org.sagebionetworks.repo.util.JSONEntityUtil;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class ConceptControllerTest {
	
	final MemoryMXBean memBean = ManagementFactory.getMemoryMXBean(); 
	private static final int MB_PER_BYTE = 1024*1024;;
	@Autowired
	ServletTestHelper testHelper;
	
	// Start memory
	long startMemoryMB;
	
	@Before
	public void before() throws Exception{
		startMemoryMB = memBean.getHeapMemoryUsage().getUsed()/MB_PER_BYTE;
		testHelper.setUp();
	}
	
	@After
	public void after(){
		long endMemoryMB = memBean.getHeapMemoryUsage().getUsed()/MB_PER_BYTE;
		System.out.println("Memory used = "+(endMemoryMB-startMemoryMB)+" mb");
	}
	
	@Test
	public void testGetSummaryNoFilter() throws ServletException, IOException{
		String parentId = "11291";
		String parentUrl = "http://synapse.sagebase.org/ontology#"+parentId;
		ConceptResponsePage response = testHelper.getConceptsForParent(parentId, null, 10, 0);
		assertNotNull(response);
		assertEquals(parentUrl, response.getParentConceptUri());
		List<Concept> children = response.getChildren();
		assertNotNull(children);
		assertTrue(response.getTotalNumberOfResults().longValue() > 50);
		assertEquals(10, children.size());
		
		// Test paging
		Concept fourthConcept = children.get(3);
		response = testHelper.getConceptsForParent(parentId, null, 1, 3);
		assertNotNull(response);
		assertEquals(parentUrl, response.getParentConceptUri());
		children = response.getChildren();
		assertNotNull(children);
		assertTrue(response.getTotalNumberOfResults().longValue() > 50);
		assertEquals(1, children.size());
		assertEquals(fourthConcept, children.get(0));
	}
	
	
	
	@Test
	public void testGetSummaryWithFilter() throws ServletException, IOException{
		String parentId = "11291";
		String parentUrl = "http://synapse.sagebase.org/ontology#"+parentId;
		String prefix = "adrenal medulla cell";
		ConceptResponsePage response = testHelper.getConceptsForParent(parentId, prefix, Integer.MAX_VALUE, 0);
		assertNotNull(response);
		assertEquals(parentUrl, response.getParentConceptUri());
		assertEquals(parentUrl, response.getParentConceptUri());
		List<Concept> children = response.getChildren();
		assertNotNull(children);
		assertEquals(1, children.size());
		assertEquals("adrenal medulla cell", children.get(0).getPreferredLabel());
		
	}
	
	@Test
	public void testGetConcept() throws ServletException, IOException{
		String conceptId = "11291";
		String conceptUrl = "http://synapse.sagebase.org/ontology#"+conceptId;
		Concept response = testHelper.getConcept(conceptId);
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
	public void testGetConceptJSONP() throws ServletException, IOException, JSONObjectAdapterException{
		String conceptId = "11291";
		String conceptUrl = "http://synapse.sagebase.org/ontology#"+conceptId;
		String callbackName = "exampleCallback";
		String response = testHelper.getConceptAsJSONP(conceptId, callbackName);
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
