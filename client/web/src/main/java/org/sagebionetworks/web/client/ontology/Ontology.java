package org.sagebionetworks.web.client.ontology;

import java.util.Arrays;

public class Ontology {
	
	private String displayName;
	private OntologyTerm[] terms;
	
	public Ontology() {
	}	

	public Ontology(String displayName, OntologyTerm[] terms) {
		super();
		this.displayName = displayName;
		this.terms = terms;
	}

	public String toString() {
		return displayName;
	}
	
	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public OntologyTerm[] getTerms() {
		return terms;
	}

	public void setTerms(OntologyTerm[] terms) {
		this.terms = terms;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((displayName == null) ? 0 : displayName.hashCode());
		result = prime * result + Arrays.hashCode(terms);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Ontology other = (Ontology) obj;
		if (displayName == null) {
			if (other.displayName != null)
				return false;
		} else if (!displayName.equals(other.displayName))
			return false;
		if (!Arrays.equals(terms, other.terms))
			return false;
		return true;
	}
	
}
