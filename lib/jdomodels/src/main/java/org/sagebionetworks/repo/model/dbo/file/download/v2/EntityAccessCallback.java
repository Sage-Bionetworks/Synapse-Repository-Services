package org.sagebionetworks.repo.model.dbo.file.download.v2;

import java.util.List;

@FunctionalInterface
public interface EntityAccessCallback {

	/**
	 * For the given entity IDs, return the sub-set that the user can download.
	 * 
	 * @param enityIds
	 * @return
	 */
	List<Long> filter(List<Long> enityIds);

}
