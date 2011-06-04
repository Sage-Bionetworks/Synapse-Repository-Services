package org.sagebionetworks.web.client.ontology;

public class StaticOntologies {

	public static final OntologyTerm[] STATUS = new OntologyTerm[] {
			new OntologyTerm("Active", "Active"),
			new OntologyTerm("Complete", "Complete") };
	
	public static final OntologyTerm[] LAYER_TYPES = new OntologyTerm[] {
			new OntologyTerm("Genetic Sequence Data", "G"),
			new OntologyTerm("Clinical / Phenotypic", "C"),
			new OntologyTerm("Molecular Expression Data", "E") };
}
