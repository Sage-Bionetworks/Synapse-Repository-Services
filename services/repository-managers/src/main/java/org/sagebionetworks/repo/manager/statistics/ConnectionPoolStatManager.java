package org.sagebionetworks.repo.manager.statistics;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.model.DatabaseConnectionPoolStats;
import org.sagebionetworks.util.ValidateArgument;

import com.amazonaws.services.cloudwatch.model.StandardUnit;

/**
 * This manager will generate cloud watch metrics for all of the database
 * connection pools when called from a timer. Both repo and workers will have
 * their own instance of this class.
 *
 */
public class ConnectionPoolStatManager {

	public enum ApplicationType {
		repo, workers
	}

	private final Consumer consumer;
	private final List<DatabaseConnectionPoolStats> poolStats;
	private final ApplicationType applicationType;
	private final String stack;
	private final String instance;

	/**
	 * Note: Both repo and workers will create their own instance of this manager,
	 * so this is not autowired.
	 * 
	 * @param consumer
	 * @param poolStats
	 * @param applicationType
	 */
	public ConnectionPoolStatManager(Consumer consumer, List<DatabaseConnectionPoolStats> poolStats,
			ApplicationType applicationType, StackConfiguration stackConfig) {
		super();
		ValidateArgument.required(consumer, "consumer");
		ValidateArgument.required(poolStats, "poolStats");
		ValidateArgument.required(applicationType, "applicationType");
		ValidateArgument.required(stackConfig, "stackConfig");
		this.consumer = consumer;
		this.poolStats = poolStats;
		this.applicationType = applicationType;
		this.stack = stackConfig.getStack();
		this.instance = stackConfig.getStackInstance();
	}

	/**
	 * 
	 */
	public void timerFired() {
		poolStats.stream().forEach(p -> {
			// idle
			consumer.addProfileData(new ProfileData().setNamespace("Synapse").setName("db-pool-idle-connections")
					.setValue(Double.valueOf(p.getNumberOfIdleConnections()))
					.setUnit(StandardUnit.Count.name())
					.setDimension(createDimensions(p)));
			// active
			consumer.addProfileData(new ProfileData().setNamespace("Synapse").setName("db-pool-active-connections")
					.setValue(Double.valueOf(p.getNumberOfActiveConnections()))
					.setUnit(StandardUnit.Count.name())
					.setDimension(createDimensions(p)));
		});
	}
	
	Map<String, String> createDimensions(DatabaseConnectionPoolStats p){
		return Map.of("db-type", p.getDatabaseType().name(), "pool-type", p.getPoolType().name(),
				"app-type", applicationType.name(), "stack", stack, "instance", instance);
	}

}
