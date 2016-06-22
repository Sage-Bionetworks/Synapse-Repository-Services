package org.sagebionetworks.table.cluster;

import java.util.List;

import org.sagebionetworks.util.ValidateArgument;
/**
 * Information about indices to add, rename, or remove.
 *
 */
public class IndexChange {

	List<DatabaseColumnInfo> toAdd = null;
	List<DatabaseColumnInfo> toRemove = null;
	List<DatabaseColumnInfo> toRename = null;
	
	public IndexChange(List<DatabaseColumnInfo> toAdd,
			List<DatabaseColumnInfo> toRemove, List<DatabaseColumnInfo> toRename) {
		ValidateArgument.required(toAdd, "toAdd");
		ValidateArgument.required(toRemove, "toRemove");
		ValidateArgument.required(toRename, "toRename");
		
		this.toAdd = toAdd;
		this.toRemove = toRemove;
		this.toRename = toRename;
	}

	public List<DatabaseColumnInfo> getToAdd() {
		return toAdd;
	}

	public void setToAdd(List<DatabaseColumnInfo> toAdd) {
		this.toAdd = toAdd;
	}

	public List<DatabaseColumnInfo> getToRemove() {
		return toRemove;
	}

	public void setToRemove(List<DatabaseColumnInfo> toRemove) {
		this.toRemove = toRemove;
	}

	public List<DatabaseColumnInfo> getToRename() {
		return toRename;
	}

	public void setToRename(List<DatabaseColumnInfo> toRename) {
		this.toRename = toRename;
	}
	
	
}
