package org.sagebionetworks.table.model;

/**
 * Generic container for a table change.
 *
 * @param <T>
 */
public class ChangeData <T extends TableChange> {
	
	long changeNumber;
	T change;
	
	public ChangeData(long changeNumber, T change) {
		super();
		this.changeNumber = changeNumber;
		this.change = change;
	}

	public T getChange() {
		return change;
	}
	
	public long getChangeNumber() {
		return changeNumber;
	}

}