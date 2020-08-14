package org.sagebionetworks.repo.model.dbo.schema;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.model.schema.ValidationSummaryStatistics;

public interface EntitySchemaValidationResultDao {

	/**
	 * Get the entity validation results for the given Entity.
	 * 
	 * @param entityId
	 * @return
	 */
	ValidationResults getValidationResults(String entityId);

	/**
	 * Get the JSON schema validation statistics for the given container ID.
	 * 
	 * @param entityId
	 * @param childIdsToExclude The children of the container that the caller lacks
	 *                          permission to read. These children must be excluded
	 *                          from the results.
	 * @return
	 */
	ValidationSummaryStatistics getEntityValidationStatistics(String entityId, Set<Long> childIdsToExclude);

	/**
	 * Get a single page of invalid ValidationResults for the given container ID>
	 * 
	 * @param containerId
	 * @param childIdsToExclude The children of the container that the caller lacks
	 *                          permission to read. These children must be excluded
	 *                          from the results.
	 * @param limit
	 * @param offset
	 * @return
	 */
	List<ValidationResults> getInvalidEntitySchemaValidationPage(String containerId, Set<Long> childIdsToExclude,
			long limit, long offset);

}
