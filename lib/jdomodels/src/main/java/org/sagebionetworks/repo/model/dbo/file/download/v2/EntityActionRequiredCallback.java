package org.sagebionetworks.repo.model.dbo.file.download.v2;

import java.util.List;

@FunctionalInterface
public interface EntityActionRequiredCallback {

	/**
	 * For the given batch of file ids, return the sub-set of entities that require
	 * the user to take some action in order to download.
	 * Note: A single file can have more than one required action.
	 * 
	 * @param fileIds
	 * @return
	 */
	List<FileActionRequired> filter(List<Long> fileIds);
}
