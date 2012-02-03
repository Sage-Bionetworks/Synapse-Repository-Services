package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;

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
	
	@Autowired
	ServletTestHelper testHelper;
	
	@Before
	public void before() throws Exception{
		testHelper.setUp();
	}
	
	@Test
	public void testGetSummaryNoFilter() throws ServletException, IOException{
		SummaryRequest request = new SummaryRequest();
		request.setParentConceptUri("http://www.infomuse.net/520/vocab/winethesaurus/red_wine");
		request.setPrefixFilter(null);
		ConceptSummaryResponse response = testHelper.getConceptsForParent(request);
		assertNotNull(response);
		assertEquals(request.getParentConceptUri(), response.getParentConceptUri());
		List<ConceptSummary> children = response.getChildren();
		assertNotNull(children);
		assertEquals(2, children.size());
		assertEquals("Cabernet Sauvignon", children.get(0).getPreferredLabel());
		assertEquals("Pinot Noir", children.get(1).getPreferredLabel());
	}
	
	@Test
	public void testGetSummaryWithFilter() throws ServletException, IOException{
		SummaryRequest request = new SummaryRequest();
		request.setParentConceptUri("http://www.infomuse.net/520/vocab/winethesaurus/red_wine");
		request.setPrefixFilter("p");
		ConceptSummaryResponse response = testHelper.getConceptsForParent(request);
		assertNotNull(response);
		assertEquals(request.getParentConceptUri(), response.getParentConceptUri());
		List<ConceptSummary> children = response.getChildren();
		assertNotNull(children);
		assertEquals(1, children.size());
		assertEquals("Pinot Noir", children.get(0).getPreferredLabel());
	}

}
