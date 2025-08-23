package org.sagebionetworks.repo.manager.grid.internal.replica.model;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public interface HasConstantIds {

	/**
	 * Get the con IDs associated with this element.
	 * 
	 * @return
	 */
	List<LogicalTimestamp> getConstantIds();

	/**
	 * Apply the provided constant IDs mapped to each constant value.
	 * 
	 */
	void applyConstants(Map<LogicalTimestamp, ConstantNode> constants);
}
