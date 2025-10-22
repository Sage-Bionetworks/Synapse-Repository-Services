package org.sagebionetworks.repo.model.dbo.schema;

import java.util.List;
import java.util.Optional;
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
	
	/**
	 * The validation summary statistics for the content of a record set is computed in a grid session and is stored 
	 * separately from the validation results of a normal entity.
	 * 
	 * @param recordSetId
	 * @param recordSetVersion
	 * @param stats
	 */
	void setRecordSetValidationSummaryStatistics(Long recordSetId, Long recordSetVersion, ValidationSummaryStatistics stats);

	/**
	 * @param recordSetId
	 * @param recordSetVersion
	 * @return The {@link ValidationSummaryStatistics} for the record set with the given id and version if such stats exist
	 */
	Optional<ValidationSummaryStatistics> getRecordSetValidationSummaryStatistics(Long recordSetId, Long recordSetVersion);
	
	void truncateAll();
}
