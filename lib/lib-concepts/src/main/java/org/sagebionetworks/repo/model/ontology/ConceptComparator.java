package org.sagebionetworks.repo.model.ontology;

import java.util.Comparator;

public class ConceptComparator implements Comparator<Concept> {

	@Override
	public int compare(Concept a, Concept b) {
		// Null are not allowed.
		if(a == b) return 0;
		// Compare the label ignoring case
		int perfCompare = String.CASE_INSENSITIVE_ORDER.compare(a.getPreferredLabel(), b.getPreferredLabel());
		if(perfCompare == 0){
			// If the labels are equal then compare the uris
			return a.getUri().compareTo(b.getUri());
		}else{
			return perfCompare;
		}
	}

}
