package org.sagebionetworks.web.client.widget.editpanels;

import java.util.List;

import org.sagebionetworks.web.client.ontology.EnumerationTerm;
import org.sagebionetworks.web.client.ontology.NcboOntologyTerm;

public class FormField {

	public static enum ColumnType { STRING, DATE, INTEGER, DECIMAL, BOOLEAN, LIST_STRING, LIST_INTEGER, LIST_DECIMAL }
	
	private String key;
	private String value;
	private NcboOntologyTerm ncboOntologyTerm;
	private EnumerationTerm enumValue;
	private EnumerationTerm[] enumTerms;
	private List<String> valueList;
	private ColumnType type;
	private boolean isList;
	private boolean isEnumBased;
	private boolean isOntologyBased;
	
	public FormField(String key, NcboOntologyTerm ncboOntologyTerm, ColumnType type) {
		this.key = key;
		this.ncboOntologyTerm = ncboOntologyTerm;
		this.type = type;
	}
	
	public FormField(String key, String value, ColumnType type) {
		isList = false;
		this.isEnumBased = false;
		this.key = key;
		this.value = value;
		this.type = type;
	}

	public FormField(String key, EnumerationTerm enumValue, EnumerationTerm[] enumTerms, ColumnType type) {
		isList = false;
		this.isEnumBased = true;
		this.key = key;
		this.enumValue = enumValue;
		this.enumTerms = enumTerms;
		if(enumValue != null) this.value = enumValue.getValue();
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
		if(isEnumBased && enumTerms != null) {
			for(EnumerationTerm term : enumTerms) {
				if(term.getValue().equals(value)) {
					this.enumValue = term;
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

	public EnumerationTerm getOntologyValue() {
		return enumValue;
	}

	public void setOntologyValue(EnumerationTerm ontologyValue) {
		this.enumValue = ontologyValue;
	}

	public EnumerationTerm[] getEnumTerms() {
		return enumTerms;
	}

	public boolean isEnumBased() {
		return isEnumBased;
	}

	public EnumerationTerm getEnumValue() {
		return enumValue;
	}

	public void setEnumValue(EnumerationTerm enumValue) {
		this.enumValue = enumValue;
	}

	public void setEnumTerms(EnumerationTerm[] enumTerms) {
		this.enumTerms = enumTerms;
	}

	public void setEnumBased(boolean isEnumBased) {
		this.isEnumBased = isEnumBased;
	}

	public NcboOntologyTerm getNcboOntologyTerm() {
		return ncboOntologyTerm;
	}

	public void setNcboOntologyTerm(NcboOntologyTerm ncboOntologyTerm) {
		this.ncboOntologyTerm = ncboOntologyTerm;
	}

	public boolean isOntologyBased() {
		return isOntologyBased;
	}

	public void setOntologyBased(boolean isOntologyBased) {
		this.isOntologyBased = isOntologyBased;
	}
	
}
