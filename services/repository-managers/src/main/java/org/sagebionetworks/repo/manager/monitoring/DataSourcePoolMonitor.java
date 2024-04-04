package org.sagebionetworks.repo.manager.monitoring;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang.StringUtils;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.util.VirtualMachineIdProvider;

import com.amazonaws.services.cloudwatch.model.StandardUnit;

public class DataSourcePoolMonitor {
	
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
	
	private Map<DataSourceId, BasicDataSource> dataSources;
	private final Consumer consumer;
	private final String namespace;

	public DataSourcePoolMonitor(ApplicationType applicationType, Map<String, BasicDataSource> dataSources, Consumer consumer, StackConfiguration config) {
		this.consumer = consumer;
		this.namespace = String.format("%s-Database-%s", StringUtils.capitalize(applicationType.name()), config.getStackInstance());
		this.dataSources = dataSources.entrySet().stream().collect(Collectors.toMap(
			entry -> DataSourceId.fromBeanName(entry.getKey()).orElseThrow(() -> new IllegalStateException("Could not find a DataSourceId mapped to the bean " + entry.getKey())), 
			entry -> entry.getValue())
		);
	}
	
	public void collectMetrics() {
		
		dataSources.forEach((id, dataSource) -> {
			int idleConnectionsCount = dataSource.getNumIdle();
			int activeConnectionsCount = dataSource.getNumActive();
			
			consumer.addProfileData(
				new ProfileData()
					.setNamespace(namespace)
					.setName("idleConnectionsCount")
					.setValue(Double.valueOf(idleConnectionsCount))
					.setUnit(StandardUnit.Count.name())
					.setDimension(createDimensions(id))
			);
			
			consumer.addProfileData(
					new ProfileData()
						.setNamespace(namespace)
						.setName("activeConnectionsCount")
						.setValue(Double.valueOf(activeConnectionsCount))
						.setUnit(StandardUnit.Count.name())
						.setDimension(createDimensions(id))
				);
		});
		
		
	}
	
	private static Map<String, String> createDimensions(DataSourceId id) {
		return Map.of(
			"dataSourceId", id.name()
		);
	}

}
