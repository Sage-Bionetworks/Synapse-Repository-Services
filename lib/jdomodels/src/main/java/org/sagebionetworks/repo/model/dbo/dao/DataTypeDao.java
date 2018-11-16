package org.sagebionetworks.repo.model.dbo.dao;

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

}
