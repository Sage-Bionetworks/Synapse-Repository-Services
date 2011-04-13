package org.sagebionetworks.web.shared;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Enumerates all of the columns for a given type.
 * 
 * @author jmhill
 *
 */
public class ColumnsForType implements IsSerializable{
	
	String type;
	List<HeaderData> defaultColumns;
	List<HeaderData> additionalColumns;
	
	public List<HeaderData> getDefaultColumns() {
		return defaultColumns;
	}
	public List<HeaderData> getAdditionalColumns() {
		return additionalColumns;
	}
	public String getType() {
		return type;
	}
	
	public ColumnsForType(){
		
	}
	
	public ColumnsForType(String type, List<HeaderData> defaultColumns,
			List<HeaderData> additionalColumns) {
		super();
		this.type = type;
		this.defaultColumns = defaultColumns;
		this.additionalColumns = additionalColumns;
	}
	

}
