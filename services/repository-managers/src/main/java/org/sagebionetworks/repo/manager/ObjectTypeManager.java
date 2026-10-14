package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.AggregateDataConfiguration;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.DataTypeResponse;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;

public interface ObjectTypeManager {

	/**
	 * Change the given object's DataType.
	 * @param userInfo
	 * @param id
	 * @param entity
	 * @param dataType
	 * @return
	 */
	DataTypeResponse changeObjectsDataType(UserInfo userInfo, String objectId, ObjectType objectType, DataType dataType);

	/**
	 * Change the given object's DataType, binding an
	 * {@link AggregateDataConfiguration} when the type is
	 * {@link DataType#AGGREGATE_DATA}.
	 *
	 * @param userInfo
	 * @param objectId
	 * @param objectType
	 * @param dataType
	 * @param configuration The aggregate-data configuration to bind. Required when
	 *                      the type is {@link DataType#AGGREGATE_DATA} and must be
	 *                      null for any other type.
	 * @return
	 */
	DataTypeResponse changeObjectsDataType(UserInfo userInfo, String objectId, ObjectType objectType, DataType dataType,
			AggregateDataConfiguration configuration);

	/**
	 * Get the {@link DataType} for the given Object ID and ObjectType.
	 * @param objectId
	 * @param objectType
	 * @return
	 */
	DataType getObjectsDataType(String objectId, ObjectType objectType);

}
