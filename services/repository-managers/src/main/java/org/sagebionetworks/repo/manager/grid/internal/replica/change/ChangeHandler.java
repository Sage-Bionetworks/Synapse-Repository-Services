package org.sagebionetworks.repo.manager.grid.internal.replica.change;

public interface ChangeHandler<T extends IntendedChange> {

	/**
	 * The type that this handle can handle.
	 * 
	 * @return
	 */
	IntendedChangeType getType();

	/**
	 * Handle the change by extending the patch builder.
	 * 
	 * @param builder
	 * @param change
	 */
	void handleChange(PatchBuilder builder, T change);

}
