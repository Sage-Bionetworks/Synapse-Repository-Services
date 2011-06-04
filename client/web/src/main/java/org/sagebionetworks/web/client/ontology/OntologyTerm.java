package org.sagebionetworks.web.client.ontology;

public class OntologyTerm {

	private String display;
	private String value;
	
	public OntologyTerm() {		
	}	
	
	public OntologyTerm(String display, String value) {
		super();
		this.display = display;
		this.value = value;
	}

	public String getDisplay() {
		return display;
	}
	public void setDisplay(String display) {
		this.display = display;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}

	public String toString() {
		return display;
	}
	
}
