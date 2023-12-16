package org.sagebionetworks.repo.manager.monitoring;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.audit.utils.VirtualMachineIdProvider;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;

import com.amazonaws.services.cloudwatch.model.StandardUnit;

public class DataSourcePoolMonitor {
	
	private static final Logger LOGGER = LogManager.getLogger(DataSourcePoolMonitor.class);
	
	public enum ApplicationType {
		repository, workers
	}
	
	public enum DataSourceId {

		idgen("idGeneratorDataSourcePool"), 
		main("dataSourcePool"), 
		migration("migrationDataSourcePool"), 
		tables("tableDatabaseConnectionPool");
		
		String beanName;
		
		private DataSourceId(String beanName) {
			this.beanName = beanName;
		}
		
		static Optional<DataSourceId> fromBeanName(String beanName) {
			for (DataSourceId id : DataSourceId.values()) {
				if (id.beanName.equals(beanName)) {
					return Optional.of(id);
				}
			}
			return Optional.empty();
		}
		
	}
	
	private final ApplicationType applicationType;
	private Map<DataSourceId, BasicDataSource> dataSources;
	private final Consumer consumer;
	private final String namespace;

	public DataSourcePoolMonitor(ApplicationType applicationType, Map<String, BasicDataSource> dataSources, Consumer consumer, StackConfiguration config) {
		this.applicationType = applicationType;
		this.consumer = consumer;
		this.namespace = String.format("%s-Database-%s", StringUtils.capitalize(applicationType.name()), config.getStackInstance());
		this.dataSources = dataSources.entrySet().stream().collect(Collectors.toMap(
			entry -> DataSourceId.fromBeanName(entry.getKey()).orElseThrow(() -> new IllegalStateException("Could not find a DataSourceId mapped to the bean " + entry.getKey())), 
			entry -> entry.getValue())
		);
	}
	
	public void collectMetrics() {
				
		String vmId = VirtualMachineIdProvider.getVMID();
		
		dataSources.forEach((id, dataSource) -> {
			int idleConnectionsCount = dataSource.getNumIdle();
			int activeConnectionsCount = dataSource.getNumActive();
			
			LOGGER.info("Collecting DB pool metrics for {} -> {} (Active: {}, Idle: {})", applicationType, id, activeConnectionsCount, idleConnectionsCount);
			
			consumer.addProfileData(
				new ProfileData()
					.setNamespace(namespace)
					.setName("idleConnectionsCount")
					.setValue(Double.valueOf(idleConnectionsCount))
					.setUnit(StandardUnit.Count.name())
					.setDimension(createDimensions(vmId, id))
			);
			
			consumer.addProfileData(
					new ProfileData()
						.setNamespace(namespace)
						.setName("activeConnectionsCount")
						.setValue(Double.valueOf(activeConnectionsCount))
						.setUnit(StandardUnit.Count.name())
						.setDimension(createDimensions(vmId, id))
				);
		});
		
		
	}
	
	Map<String, String> createDimensions(String vmId, DataSourceId id) {
		return Map.of(
			"vmId", vmId,
			"dataSourceId", id.name()
		);
	}

}
