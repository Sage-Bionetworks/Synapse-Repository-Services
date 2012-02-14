package org.sagebionetworks.repo.model.ontology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;

import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.impl.OntModelImpl;
import com.hp.hpl.jena.rdf.model.Model;

public class ConceptJenaDAOImplTest {

	Model wineThesaurusModel;

	@Before
	public void before() {
		String fileName = "winethesaurus.skos";
		URL exampleURL = ConceptJenaDAOImplTest.class.getClassLoader()
				.getResource(fileName);
		assertNotNull("Failed to find the example file on the classpath: "
				+ fileName, exampleURL);
		OntModelImpl reader = new OntModelImpl(OntModelSpec.OWL_MEM);
		// System.out.println(exampleURL.toString());
		wineThesaurusModel = reader.read(exampleURL.toString());
		assertNotNull(wineThesaurusModel);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetAllConceptsNull() throws DatastoreException{
		ConceptJenaDAOImpl dao = new ConceptJenaDAOImpl(wineThesaurusModel);
		dao.getAllConcepts(null);
	}

	@Test
	public void testGetAllConceptsRedWine() throws Exception {
		// Create a dao with the model
		ConceptJenaDAOImpl dao = new ConceptJenaDAOImpl(wineThesaurusModel);
		
		// This this is what we expect to get back for red wine.
		List<ConceptSummary> expected = new ArrayList<ConceptSummary>();
		ConceptSummary summary = new ConceptSummary();
		summary.setUri("http://www.infomuse.net/520/vocab/winethesaurus/pinot_noir");
		summary.setPreferredLabel("Pinot Noir");
		expected.add(summary);
		summary = new ConceptSummary();
		summary.setUri("http://www.infomuse.net/520/vocab/winethesaurus/cabernet");
		summary.setPreferredLabel("Cabernet Sauvignon");
		expected.add(summary);
		
		// Execute the query to get all red wines.
		String parentConceptURI = "http://www.infomuse.net/520/vocab/winethesaurus/red_wine";
		List<ConceptSummary> results = dao.getAllConcepts(parentConceptURI);
//		System.out.println(results);

		// // Do the results match the expected.
		assertEquals(expected, results);
	}
	
	@Test
	public void testGetAllConceptsWineType() throws Exception {
		// Create a dao with the model
		ConceptJenaDAOImpl dao = new ConceptJenaDAOImpl(wineThesaurusModel);
		
		// This this is what we expect to get back for red wine.
		List<ConceptSummary> expected = new ArrayList<ConceptSummary>();
		ConceptSummary summary = new ConceptSummary();
		// port
		summary.setUri("http://www.infomuse.net/520/vocab/winethesaurus/port");
		summary.setPreferredLabel("Port");
		expected.add(summary);
		// sherry
		summary = new ConceptSummary();
		summary.setUri("http://www.infomuse.net/520/vocab/winethesaurus/sherry");
		summary.setPreferredLabel("Sherry");
		expected.add(summary);
		// Sparkling Wine
		summary = new ConceptSummary();
		summary.setUri("http://www.infomuse.net/520/vocab/winethesaurus/sparkling_wine");
		summary.setPreferredLabel("Sparkling Wine");
		expected.add(summary);
		// Champagne
		summary = new ConceptSummary();
		summary.setUri("http://www.infomuse.net/520/vocab/winethesaurus/champagne");
		summary.setPreferredLabel("Champagne");
		expected.add(summary);
		// White wine
		summary = new ConceptSummary();
		summary.setUri("http://www.infomuse.net/520/vocab/winethesaurus/white_wine");
		summary.setPreferredLabel("White wine");
		expected.add(summary);
		// White wine
		summary = new ConceptSummary();
		summary.setUri("http://www.infomuse.net/520/vocab/winethesaurus/red_wine");
		summary.setPreferredLabel("Red wine");
		expected.add(summary);
		// Execute the query to get all red wines.
		String parentConceptURI = "http://www.infomuse.net/520/vocab/winethesaurus/wine_type";
		List<ConceptSummary> results = dao.getAllConcepts(parentConceptURI);
		// Do the results match the expected.
		System.out.println(results);
		// Sort both
		Collections.sort(results, new ConceptSummaryComparator());
		Collections.sort(expected, new ConceptSummaryComparator());
		assertEquals(expected.size(), results.size());
		assertEquals(expected, results);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetConceptForUriNull() throws DatastoreException, NotFoundException{
		ConceptJenaDAOImpl dao = new ConceptJenaDAOImpl(wineThesaurusModel);
		dao.getConceptForUri(null);
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetConceptForNotFound() throws DatastoreException, NotFoundException{
		ConceptJenaDAOImpl dao = new ConceptJenaDAOImpl(wineThesaurusModel);
		// This does not exist and should throw a NotFoundException
		dao.getConceptForUri("http://www.infomuse.net/520/vocab/winethesaurus/fake");
	}

	@Test 
	public void testGetConceptUS() throws DatastoreException, NotFoundException{
		// This concept exists.
		String conceptUri = "http://www.infomuse.net/520/vocab/winethesaurus/United_States";
		ConceptJenaDAOImpl dao = new ConceptJenaDAOImpl(wineThesaurusModel);
//		ConceptSummary summary = new ConceptSummary();
//		summary.setUri(conceptUri);
//		summary.setPreferredLabel("United States");
		Concept expected = new Concept();
		expected.setUri(conceptUri);
		expected.setPreferredLabel("United States");
		expected.setDefinition("United States");
		expected.setParent("http://www.infomuse.net/520/vocab/winethesaurus/wine_region");
		List<String> synonyms = new ArrayList<String>();
		expected.setSynonyms(synonyms);
		synonyms.add("United States of America");
		synonyms.add("USA");
		synonyms.add("US");
		synonyms.add("U.S.");
		
		// fetch the concept.
		Concept concept = dao.getConceptForUri(conceptUri);
		assertNotNull(concept);
		assertEquals(expected, concept);
	}

}
