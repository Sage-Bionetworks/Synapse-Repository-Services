package org.sagebionetworks.repo.model.dbo.dao;

import java.util.Optional;

import org.sagebionetworks.repo.model.AggregateDataConfiguration;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.DataTypeResponse;
import org.sagebionetworks.repo.model.ObjectType;

public interface DataTypeDao {

	/**
	 * Change the {@link DataType} of the given object.
	 *
	 * @param userId
	 * @param objectId
	 * @param objectType
	 * @param dataType
	 * @return
	 */
	DataTypeResponse changeDataType(Long userId, String objectId, ObjectType objectType, DataType dataType);

	/**
	 * Change the {@link DataType} of the given object, binding an
	 * {@link AggregateDataConfiguration} when the type is
	 * {@link DataType#AGGREGATE_DATA}.
	 *
	 * @param userId
	 * @param objectId
	 * @param objectType
	 * @param dataType
	 * @param configuration The aggregate-data configuration to bind. Must be null
	 *                      for any type other than {@link DataType#AGGREGATE_DATA}.
	 * @return
	 */
	DataTypeResponse changeDataType(Long userId, String objectId, ObjectType objectType, DataType dataType,
			AggregateDataConfiguration configuration);

	/**
	 * Remove all type data.
	 */
	void truncateAllData();

	/**
	 * Get the {@link DataType} for the given object.
	 *
	 * Note: If the DataType has not been set for the given object, then the default
	 * type will be returned.
	 *
	 * @param objectId   The ID of the object .
	 * @param objectType The type of the object.
	 * @return
	 */
	DataType getObjectDataType(String objectId, ObjectType objectType);

	/**
	 * Get the {@link AggregateDataConfiguration} bound to the given object.
	 *
	 * @param objectId   The ID of the object.
	 * @param objectType The type of the object.
	 * @return The bound configuration, or {@link Optional#empty()} if the object
	 *         has no row or is not {@link DataType#AGGREGATE_DATA}.
	 */
	Optional<AggregateDataConfiguration> getAggregateDataConfiguration(String objectId, ObjectType objectType);

}
