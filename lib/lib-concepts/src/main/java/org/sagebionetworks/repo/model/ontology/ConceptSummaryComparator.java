package org.sagebionetworks.repo.model.ontology;

import java.util.Comparator;

/**
 * Comparator for content summary. Will sort perferedLabel first then URI if the labels are equal.
 * 
 * @author jmhill
 *
 */
public class ConceptSummaryComparator implements Comparator<ConceptSummary> {

	@Override
	public int compare(ConceptSummary a, ConceptSummary b) {
		// Null are not allowed.
		if(a == b) return 0;
		int perfCompare = a.getPreferredLabel().compareTo( b.getPreferredLabel());
		if(perfCompare == 0){
			// If the labels are equal then compare the uris
			return a.getUri().compareTo(b.getUri());
		}else{
			return perfCompare;
		}
	}

}
