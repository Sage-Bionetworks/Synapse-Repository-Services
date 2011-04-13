package org.sagebionetworks.web.unitclient.presenter;

import java.util.List;

import org.sagebionetworks.web.shared.CompositeColumn;
import org.sagebionetworks.web.shared.HeaderData;

/**
 * A stub HeaderData for tests.
 * Please do not implement hash() or equals()
 * for this class.
 * 
 * @author jmhill
 *
 */
public class SampleHeaderData implements HeaderData, CompositeColumn{
	
	private String id;
	private String displayName;
	private String description;
	private String sortId;
	private List<String> dependencyList;
	
	public SampleHeaderData(String id, String displayName, String description) {
		super();
		this.id = id;
		this.displayName = displayName;
		this.description = description;
	}

	public void setSortId(String sortId) {
		this.sortId = sortId;
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
	
	@Override
	public List<String> getBaseDependencyIds() {
		return this.dependencyList;
	}

	public void setDependencyList(List<String> dependencyList) {
		this.dependencyList = dependencyList;
	}

	@Override
	public int getColumnWidth() {
		// TODO Auto-generated method stub
		return 150;
	}

}