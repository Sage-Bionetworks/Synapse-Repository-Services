package org.sagebionetworks.repo.manager.ontology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ontology.Concept;
import org.sagebionetworks.repo.model.ontology.ConceptDAO;
import org.sagebionetworks.repo.model.ontology.ConceptSummary;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * A simple stub implementation of the ConceptDAO for testing.
 * 
 * @author jmhill
 *
 */
public class StubConceptDAO implements ConceptDAO {

	Map<String, List<ConceptSummary>> children = new HashMap<String, List<ConceptSummary>>();
	Map<String, Concept> concepts = new HashMap<String, Concept>();
	/**
	 * This stub is simply backed by a map.
	 * @param map
	 */
	public StubConceptDAO(List<Concept> in){
		// Build the two maps
		for(Concept con: in){
			// Add the parent map.
			if(con.getParent() != null){
				List<ConceptSummary> childList = children.get(con.getParent());
				if(childList == null){
					childList = new ArrayList<ConceptSummary>();
					children.put(con.getParent(), childList);
				}
				childList.add(con.getSummary());
			}
			// Add to the concept map
			concepts.put(con.getSummary().getUri(), con);
		}
	}
	@Override
	public List<ConceptSummary> getAllConcepts(String parentConceptURI)	throws DatastoreException {
		return children.get(parentConceptURI);
	}

	@Override
	public Concept getConceptForUri(String conceptUri)
			throws DatastoreException, NotFoundException {
		Concept con = concepts.get(conceptUri);
		if(con == null) throw new NotFoundException("Cannot find: "+conceptUri);
		return con;
	}

}
