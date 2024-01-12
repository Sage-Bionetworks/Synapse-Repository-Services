package org.sagebionetworks.repo.manager.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.manager.monitoring.DataSourcePoolMonitor.ApplicationType;
import org.sagebionetworks.repo.manager.monitoring.DataSourcePoolMonitor.DataSourceId;
import org.sagebionetworks.util.VirtualMachineIdProvider;

import com.amazonaws.services.cloudwatch.model.StandardUnit;

@ExtendWith(MockitoExtension.class)
public class DataSourcePoolMonitorTest {

	@Mock
	private Consumer mockConsumer;
	
	@Mock
	private StackConfiguration mockConfig;
	
	@Mock
	private BasicDataSource mockDataSource;
	
	private DataSourcePoolMonitor monitor;
	
	private String vmId = VirtualMachineIdProvider.getVMID();

	@BeforeEach
	public void beforeEach() {
		MockitoAnnotations.initMocks(this);
		
		Map<String, BasicDataSource> dataSources = Map.of(
			DataSourceId.main.beanName, mockDataSource
		);
		
		when(mockConfig.getStackInstance()).thenReturn("test");
		monitor = new DataSourcePoolMonitor(ApplicationType.repository, dataSources, mockConsumer, mockConfig);
	}
	
	@Test
	public void testMonitorWithUnexpectedDataSource() {
		Map<String, BasicDataSource> dataSources = Map.of(
			"unexpectedBeanName", mockDataSource
		);
		
		String result = assertThrows(IllegalStateException.class, () -> {
			monitor = new DataSourcePoolMonitor(ApplicationType.repository, dataSources, mockConsumer, mockConfig);
		}).getMessage();
		
		assertEquals("Could not find a DataSourceId mapped to the bean unexpectedBeanName", result);
	}
	
	@Test
	public void testCollectMetrics() {
		when(mockDataSource.getNumActive()).thenReturn(5);
		when(mockDataSource.getNumIdle()).thenReturn(10);
		
		// Call under test
		monitor.collectMetrics();
		
		ProfileData expectedIdleData = new ProfileData()
			.setNamespace("Repository-Database-test")
			.setName("idleConnectionsCount")
			.setValue(10.0)
			.setUnit(StandardUnit.Count.name())
			.setDimension(Map.of(
				"vmId", vmId,
				"dataSourceId", DataSourceId.main.name()
			));
		
		ProfileData expectedActiveData = new ProfileData()
			.setNamespace("Repository-Database-test")
			.setName("activeConnectionsCount")
			.setValue(5.0)
			.setUnit(StandardUnit.Count.name())
			.setDimension(Map.of(
				"vmId", vmId,
				"dataSourceId", DataSourceId.main.name()
			));
		
		verify(mockConsumer).addProfileData(expectedIdleData);
		verify(mockConsumer).addProfileData(expectedActiveData);
	}
}
