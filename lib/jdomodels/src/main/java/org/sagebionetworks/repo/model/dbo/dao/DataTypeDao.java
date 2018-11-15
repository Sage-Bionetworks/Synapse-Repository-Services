package org.sagebionetworks.repo.model.dbo.dao;

import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.DataTypeResponse;
import org.sagebionetworks.repo.model.ObjectType;

public interface DataTypeDao {

	/**
	 * Change the {@link DataType} of the given object.
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

}
