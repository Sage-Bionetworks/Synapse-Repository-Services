package org.sagebionetworks.web.client.ontology;

public class NcboOntologyTerm {

	private String label;
	private String ontologyVersionId;
	private String conceptId;
	
	public NcboOntologyTerm() {
		
	}
	
	public NcboOntologyTerm(String label, String ontologyVersionId,
			String conceptId) {
		super();
		this.label = label;
		this.ontologyVersionId = ontologyVersionId;
		this.conceptId = conceptId;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getOntologyVersionId() {
		return ontologyVersionId;
	}

	public void setOntologyVersionId(String ontologyVersionId) {
		this.ontologyVersionId = ontologyVersionId;
	}

	public String getConceptId() {
		return conceptId;
	}

	public void setConceptId(String conceptId) {
		this.conceptId = conceptId;
	}
	
	
	
}
