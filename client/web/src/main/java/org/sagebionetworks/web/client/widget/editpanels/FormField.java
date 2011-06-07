package org.sagebionetworks.web.client.widget.editpanels;

import java.util.List;

import org.sagebionetworks.web.client.ontology.OntologyTerm;

public class FormField {

	public static enum ColumnType { STRING, DATE, INTEGER, DECIMAL, BOOLEAN, LIST_STRING, LIST_INTEGER, LIST_DECIMAL }
	
	private String key;
	private String value;
	private OntologyTerm ontologyValue;
	private OntologyTerm[] ontologyTerms;
	private List<String> valueList;
	private ColumnType type;
	private boolean isList;
	private boolean isOntologyBased;
	
	public FormField(String key, String value, ColumnType type) {
		isList = false;
		this.isOntologyBased = false;
		this.key = key;
		this.value = value;
		this.type = type;
	}

	public FormField(String key, OntologyTerm ontologyValue, OntologyTerm[] ontologyTerms, ColumnType type) {
		isList = false;
		this.isOntologyBased = true;
		this.key = key;
		this.ontologyValue = ontologyValue;
		this.ontologyTerms = ontologyTerms;
		if(ontologyValue != null) this.value = ontologyValue.getValue();
		this.type = type;
	}
	
	public FormField(String key, List<String> valueList, ColumnType type) {
		isList = true;		
		this.key = key;
		this.valueList = valueList;
		this.type = type;
	}
	
	/**
	 * Sets the value of the field. If this is an ontology based field, it finds the matching value in the ontology
	 * @param value
	 */
	public void setValue(String value) {
		if(isOntologyBased && ontologyTerms != null) {
			for(OntologyTerm term : ontologyTerms) {
				if(term.getValue().equals(value)) {
					this.ontologyValue = term;
					this.value = value;
				}
			}
		} else {
			this.value = value;
		}
	}

	
	/*
	 * Auto Generated methods
	 */

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	public List<String> getValueList() {
		return valueList;
	}

	public ColumnType getType() {
		return type;
	}

	public boolean isList() {
		return isList;
	}	


	public void setValueList(List<String> valueList) {
		this.valueList = valueList;
	}

	public OntologyTerm getOntologyValue() {
		return ontologyValue;
	}

	public void setOntologyValue(OntologyTerm ontologyValue) {
		this.ontologyValue = ontologyValue;
	}

	public OntologyTerm[] getOntologyTerms() {
		return ontologyTerms;
	}

	public void setOntologyTerms(OntologyTerm[] ontologyTerms) {
		this.ontologyTerms = ontologyTerms;
	}

	public boolean isOntologyBased() {
		return isOntologyBased;
	}

	public void setOntologyBased(boolean isOntologyBased) {
		this.isOntologyBased = isOntologyBased;
	}
	
}
