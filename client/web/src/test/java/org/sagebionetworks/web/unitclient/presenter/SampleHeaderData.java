package org.sagebionetworks.web.unitclient.presenter;

import org.sagebionetworks.web.shared.HeaderData;

/**
 * A stub HeaderData for test.
 * Please do not implement hash() or equals()
 * for this class.
 * 
 * @author jmhill
 *
 */
public class SampleHeaderData implements HeaderData{
	
	private String id;
	private String displayName;
	private String description;
	private String sortId;
	
	public SampleHeaderData(String id, String displayName, String description) {
		super();
		this.id = id;
		this.displayName = displayName;
		this.description = description;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	@Override
	public String getSortId() {
		return sortId;
	}

}