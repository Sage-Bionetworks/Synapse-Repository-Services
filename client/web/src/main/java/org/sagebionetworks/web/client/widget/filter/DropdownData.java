package org.sagebionetworks.web.client.widget.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * Data for a single drop-down.
 * 
 * @author jmhill
 *
 */
public class DropdownData {
	
	private String id;
	private List<String> valueList = new ArrayList<String>();
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	public void addValue(String value){
		valueList.add(value);
	}
	
	public List<String> getValueList() {
		return valueList;
	}

}
