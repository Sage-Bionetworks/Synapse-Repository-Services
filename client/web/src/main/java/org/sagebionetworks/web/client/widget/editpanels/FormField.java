package org.sagebionetworks.web.client.widget.editpanels;

import java.util.List;

public class FormField {

	public static enum ColumnType { STRING, DATE, INTEGER, DECIMAL, BOOLEAN, LIST_STRING, LIST_INTEGER, LIST_DECIMAL }
	
	private String key;
	private String value;
	private List<String> valueList;
	private ColumnType type;
	private boolean isList;
	
	public FormField(String key, String value, ColumnType type) throws Exception {
		if(type == ColumnType.LIST_DECIMAL || type == ColumnType.LIST_INTEGER || type == ColumnType.LIST_STRING) {
			throw new Exception("For list values you should use the use the list constructor");
		}
		isList = false;
		this.key = key;
		this.value = value;
		this.type = type;
	}

	public FormField(String key, List<String> valueList, ColumnType type) throws Exception {
		if(type == ColumnType.DECIMAL || type == ColumnType.INTEGER || type == ColumnType.STRING) {
			throw new Exception("For single values you should use the use the single value constructor");
		}
		isList = true;
		this.key = key;
		this.valueList = valueList;
		this.type = type;
	}

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

	public void setValue(String value) {
		this.value = value;
	}

	public void setValueList(List<String> valueList) {
		this.valueList = valueList;
	}

}
