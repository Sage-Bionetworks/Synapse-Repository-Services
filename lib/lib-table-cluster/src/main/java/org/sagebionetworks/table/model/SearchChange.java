package org.sagebionetworks.table.model;

public class SearchChange implements TableChange {
	
	private boolean enabled;

	public SearchChange(boolean enabled) {	
		this.enabled = enabled;
	}
	
	public boolean isEnabled() {
		return enabled;
	}

}
