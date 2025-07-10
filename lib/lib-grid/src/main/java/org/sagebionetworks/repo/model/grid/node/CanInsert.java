package org.sagebionetworks.repo.model.grid.node;

/**
 * Abstraction for a node insert action.
 * 
 * @param <T> The type of object that can be inserted into this node.
 */
public interface CanInsert<T> {

	/**
	 * Attempt to insert the following change into a Node.
	 * 
	 * @param change
	 * @return
	 */
	public boolean attemptInsert(T change);

}
