package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AggregateDataConfiguration;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.DataTypeResponse;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.DataTypeDao;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class ObjectTypeManagerImpl implements ObjectTypeManager {

	@Autowired
	DataTypeDao dataTypeDao;

	@Autowired
	AuthorizationManager authorizationManager;

	@WriteTransaction
	@Override
	public DataTypeResponse changeObjectsDataType(UserInfo userInfo, String objectId, ObjectType objectType,
			DataType dataType) {
		return changeObjectsDataType(userInfo, objectId, objectType, dataType, null);
	}

	@WriteTransaction
	@Override
	public DataTypeResponse changeObjectsDataType(UserInfo userInfo, String objectId, ObjectType objectType,
			DataType dataType, AggregateDataConfiguration configuration) {
		ValidateArgument.required(userInfo, "User");
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, "ObjectType");
		ValidateArgument.required(dataType, "DataType");
		validateConfiguration(dataType, configuration);
		// Any member of the ACT can change any object's type.
		if (!authorizationManager.isACTTeamMemberOrAdmin(userInfo)) {
			// Open and aggregate data can only be set by a member of the ACT.
			if (DataType.OPEN_DATA.equals(dataType) || DataType.AGGREGATE_DATA.equals(dataType)) {
				throw new UnauthorizedException("Must be a member of the 'Synapse Access and Compliance Team' to change an object's DataType to: "+dataType.name());
			}
			// must have the update permission.
			if (!authorizationManager.canAccess(userInfo, objectId, objectType, ACCESS_TYPE.UPDATE).isAuthorized()) {
				throw new UnauthorizedException("Must have "+ACCESS_TYPE.UPDATE+" permission to change an object's DataType to : "+dataType.name());
			}
		}
		return dataTypeDao.changeDataType(userInfo.getId(), objectId, objectType, dataType, configuration);
	}

	/**
	 * The configuration is required and validated only for the AGGREGATE_DATA type;
	 * it must be absent for every other type.
	 */
	static void validateConfiguration(DataType dataType, AggregateDataConfiguration configuration) {
		if (DataType.AGGREGATE_DATA.equals(dataType)) {
			ValidateArgument.required(configuration, "aggregateDataConfiguration");
			ValidateArgument.required(configuration.getSuppressionThreshold(),
					"aggregateDataConfiguration.suppressionThreshold");
			ValidateArgument.requirement(configuration.getSuppressionThreshold() > 0,
					"aggregateDataConfiguration.suppressionThreshold must be greater than zero.");
			if (configuration.getFacetPostProcessingConfig() != null) {
				ValidateArgument.required(configuration.getFacetPostProcessingConfig().getAlgorithm(),
						"aggregateDataConfiguration.facetPostProcessingConfig.algorithm");
			}
		} else {
			ValidateArgument.requirement(configuration == null,
					"An aggregateDataConfiguration can only be provided for the " + DataType.AGGREGATE_DATA.name()
							+ " DataType.");
		}
	}

	@Override
	public DataType getObjectsDataType(String objectId, ObjectType objectType) {
		return dataTypeDao.getObjectDataType(objectId, objectType);
	}

}
