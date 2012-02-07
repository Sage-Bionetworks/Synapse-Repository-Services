package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.List;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ontology.ConceptSummary;
import org.sagebionetworks.repo.model.ontology.ConceptSummaryResponse;
import org.sagebionetworks.repo.model.ontology.SummaryRequest;
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
		SummaryRequest request = new SummaryRequest();
		request.setParentConceptUri("http://synapse.sagebase.org/ontology#11291");
		request.setPrefixFilter(null);
		ConceptSummaryResponse response = testHelper.getConceptsForParent(request);
		assertNotNull(response);
		assertEquals(request.getParentConceptUri(), response.getParentConceptUri());
		List<ConceptSummary> children = response.getChildren();
		assertNotNull(children);
		assertTrue(children.size() > 10);
	}
	
	@Test
	public void testGetSummaryWithFilter() throws ServletException, IOException{
		SummaryRequest request = new SummaryRequest();
		request.setParentConceptUri("http://synapse.sagebase.org/ontology#11291");
		request.setPrefixFilter("adrenal medulla cell");
		ConceptSummaryResponse response = testHelper.getConceptsForParent(request);
		assertNotNull(response);
		assertEquals(request.getParentConceptUri(), response.getParentConceptUri());
		List<ConceptSummary> children = response.getChildren();
		assertNotNull(children);
		assertEquals(1, children.size());
		assertEquals("adrenal medulla cell", children.get(0).getPreferredLabel());
		
	}
	
}
