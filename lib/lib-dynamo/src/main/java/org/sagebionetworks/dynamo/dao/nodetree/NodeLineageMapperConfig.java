package org.sagebionetworks.dynamo.dao.nodetree;

import org.sagebionetworks.StackConfiguration;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.ConsistentReads;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.TableNameOverride;

/**
 * Provides predefined configurations on the {@link DboNodeLineage} table.
 */
class NodeLineageMapperConfig {

	private static final DynamoDBMapperConfig MAPPER_CONFIG;
	private static final DynamoDBMapperConfig MAPPER_CONFIG_CR;
	static {
		String stackPrefix = StackConfiguration.getStack() + "-" + StackConfiguration.getStackInstance() + "-";
		TableNameOverride tableNameOverride = new TableNameOverride(stackPrefix + DboNodeLineage.TABLE_NAME);
		MAPPER_CONFIG = new DynamoDBMapperConfig(tableNameOverride);
		MAPPER_CONFIG_CR = new DynamoDBMapperConfig(null, ConsistentReads.CONSISTENT, tableNameOverride);
	}

	static DynamoDBMapperConfig getMapperConfig() {
		return MAPPER_CONFIG;
	}

	static DynamoDBMapperConfig getMapperConfigWithConsistentReads() {
		return MAPPER_CONFIG_CR;
	}
}
