package org.sagebionetworks.repo.manager.ontology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityQueryResults;
import org.sagebionetworks.repo.model.ontology.Concept;
import org.sagebionetworks.repo.model.ontology.ConceptDAO;
import org.sagebionetworks.repo.model.ontology.ConceptSummary;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Basic implementation of the concept manager.
 * 
 * Note: This class currently uses a map for prefix lookup.  A Trie or prefix tree would probably be more efficient but
 * map works naturally with memory caches like mcached.  We can always change to a Trie if this proves inefficient.
 * 
 * @author jmhill
 *
 */
public class ConceptManagerImpl implements ConceptManager {
		
	@Autowired
	ConceptDAO conceptDao;
	
	@Autowired
	ConceptCache conceptCache;
	
	@Autowired
	String ontologyBaseURI;
	
	/**
	 * Used by Spring.
	 */
	public ConceptManagerImpl(){
		
	}

	/**
	 * Used for testing
	 * @param conceptDao
	 * @param conceptCache
	 * @param ontologyBaseURI
	 */
	public ConceptManagerImpl(ConceptDAO conceptDao, ConceptCache conceptCache,	String ontologyBaseURI) {
		super();
		this.conceptDao = conceptDao;
		this.conceptCache = conceptCache;
		this.ontologyBaseURI = ontologyBaseURI;
	}
	
	@Override
	public EntityQueryResults<Concept> getChildConcepts(String parentConceptURI, String prefix, int limit, int offest) throws DatastoreException, NotFoundException {
		// First extract the unique value
		String uniquePart = getUniqueURIPart(parentConceptURI);
		// First check to see if the cache has 
		if(!conceptCache.containsKey(uniquePart)){
			// Populate the cache
			populateCache(parentConceptURI, uniquePart);
		}
		// the unique part is the key unless there is a prefix.
		String key;
		if(prefix != null){
			key = uniquePart+prefix.toLowerCase();
		}else{
			key = uniquePart;
		}
		List<Concept> fullList = conceptCache.getConceptsForKey(key);
		if(fullList == null){
			fullList = new ArrayList<Concept>();
		}
		
		// Return one page.
		return new EntityQueryResults<Concept>(fullList, limit, offest);
	}
	

	/**
	 * Populate the cache using data from the DAO.
	 * @param parentConceptURI
	 * @throws NotFoundException 
	 */
	private void populateCache(String parentConceptURI, String uniquePart)throws DatastoreException, NotFoundException {
		// First get all of the concepts.
		List<ConceptSummary> list = conceptDao.getAllConcepts(parentConceptURI);
		// Build the full list
		List<Concept> fullList = new LinkedList<Concept>();
		for(ConceptSummary summary: list){
			fullList.add(getConcept(summary.getUri()));
		}
		Map<String, List<Concept>> result = new HashMap<String, List<Concept>>();
		// Add the full list to the map using the unique part.
		result.put(uniquePart, fullList);
		// Now get all of the concepts 
		for(ConceptSummary cs: list){
			Concept con = conceptDao.getConceptForUri(cs.getUri());
			// Add this concept to the map
			ConceptUtils.populateMapWithLowerCasePrefixForConcept(uniquePart, con, result);
		}
		// Now sort all of the lists
		ConceptUtils.sortAllSummaryLists(result);
		
		// Add this map the cache
		conceptCache.putAll(result);
	}
	
	
	/**
	 * Extract the unique part of the URI.
	 * @param uri
	 * @return
	 */
	String getUniqueURIPart(String full){
		if(full == null) throw new IllegalArgumentException("uri cannot be null");
		if(full.length() <= (ontologyBaseURI.length())) throw new IllegalArgumentException("The uri.length cannot be less then the length of the ontology URI");
		return full.substring(ontologyBaseURI.length());
		
	}


	@Override
	public Concept getConcept(String uri) throws DatastoreException, NotFoundException {
		// Try to get it from the cache
		Concept con = conceptCache.getConcept(uri);
		if(con == null){
			// Get it from the dao
			con = this.conceptDao.getConceptForUri(uri);
			// Put it in the cache.
			conceptCache.put(uri, con);
		}
		// Pass it along.
		return con;
	}

	@Override
	public String getOntologyBaseURI() {
		return ontologyBaseURI;
	}



}
