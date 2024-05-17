package org.sagebionetworks.repo.manager.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;

import com.amazonaws.services.cloudwatch.model.StandardUnit;

@ExtendWith(MockitoExtension.class)
public class DiskMonitorTest {

	@Mock
	private TempDiskProvider mockProvider;

	@Mock
	private LoggerProvider mockLoggerProivder;

	@Mock
	private Logger mockLogger;

	@Mock
	private Consumer mockConsumer;

	@Captor
	private ArgumentCaptor<String> stringCaptor;

	private DiskMonitor monitor;

	@BeforeEach
	public void before() {
		when(mockLoggerProivder.getLogger(any())).thenReturn(mockLogger);
		monitor = new DiskMonitor(ApplicationType.workers, mockProvider, mockLoggerProivder, mockConsumer, "test");
	}

	@Test
	public void testCollectMetricsOver90() {
		when(mockProvider.getDiskSpaceUsedPercent()).thenReturn(0.945);
		when(mockProvider.listTempFiles()).thenReturn(new ArrayList<>(List.of(
				new FileInfo((long) 1.2e9, "a.txt"),
				new FileInfo((long) 89.2e9, "b.zip"),
				new FileInfo((long) 12.3e9, "c.jar"),
				new FileInfo((long) 91.1e9, "d.txt"),
				new FileInfo((long) 15.2e9, "e.tar"),
				new FileInfo((long) 17.2e9, "f.foo"),
				new FileInfo((long) 13.3e9, "bar.bar"),
				new FileInfo((long) 6.2e9, "car.txt"),
				new FileInfo((long) 3.2e9, "truck.txt"),
				new FileInfo((long) 1.2e9, "truck2.txt"),
				new FileInfo((long) 2.2e9, "truck3.txt"),
				new FileInfo((long) 2.2e9, "soo.zip")
		)));
		when(mockProvider.getTempDirectoryName()).thenReturn("/foo/tmp/");
		when(mockProvider.getMachineId()).thenReturn("someVMID");
		
		// call under test
		monitor.collectMetrics();
		
		verify(mockProvider).getDiskSpaceUsedPercent();
		verify(mockProvider).getTempDirectoryName();
		verify(mockProvider).getMachineId();
		verify(mockProvider).listTempFiles();
		
		verify(mockConsumer).addProfileData(new ProfileData()
				.setNamespace("Workers-Disk-test")
				.setName("percentTempDiskSpaceUsed")
				.setValue(94.5)
				.setUnit(StandardUnit.Percent.name())
				.setDimension(Map.of(
					"machineId", "someVMID"
				)));
		
		verify(mockLogger).info(stringCaptor.capture());
		String expected = "Drive of the temp directory: '/foo/tmp/' is 94.50 % full. Top 10 files by size:\n"
				+ "    84.843 GB 'd.txt'\n"
				+ "    83.074 GB 'b.zip'\n"
				+ "    16.019 GB 'f.foo'\n"
				+ "    14.156 GB 'e.tar'\n"
				+ "    12.387 GB 'bar.bar'\n"
				+ "    11.455 GB 'c.jar'\n"
				+ "     5.774 GB 'car.txt'\n"
				+ "     2.980 GB 'truck.txt'\n"
				+ "     2.049 GB 'truck3.txt'\n"
				+ "     2.049 GB 'soo.zip'";
		assertEquals(expected, stringCaptor.getValue());
	}
	
	@Test
	public void testCollectMetricsOver90AndNoFiles() {
		when(mockProvider.getDiskSpaceUsedPercent()).thenReturn(0.945);
		when(mockProvider.listTempFiles()).thenReturn(new ArrayList<>());
		when(mockProvider.getTempDirectoryName()).thenReturn("/foo/tmp/");
		when(mockProvider.getMachineId()).thenReturn("someVMID");
		
		// call under test
		monitor.collectMetrics();
		
		verify(mockProvider).getDiskSpaceUsedPercent();
		verify(mockProvider).getTempDirectoryName();
		verify(mockProvider).getMachineId();
		verify(mockProvider).listTempFiles();
		
		verify(mockConsumer).addProfileData(new ProfileData()
				.setNamespace("Workers-Disk-test")
				.setName("percentTempDiskSpaceUsed")
				.setValue(94.5)
				.setUnit(StandardUnit.Percent.name())
				.setDimension(Map.of(
					"machineId", "someVMID"
				)));
		
		verify(mockLogger).info(stringCaptor.capture());
		String expected = "Drive of the temp directory: '/foo/tmp/' is 94.50 % full. Top 10 files by size:\n";
		assertEquals(expected, stringCaptor.getValue());
	}

	@Test
	public void testCollectMetricsUnder90() {
		when(mockProvider.getDiskSpaceUsedPercent()).thenReturn(0.89);
		when(mockProvider.getMachineId()).thenReturn("someVMID");
		
		// call under test
		monitor.collectMetrics();
		
		verify(mockProvider).getDiskSpaceUsedPercent();
		verify(mockProvider, never()).getTempDirectoryName();
		verify(mockProvider).getMachineId();
		verify(mockProvider, never()).listTempFiles();
		
		verify(mockConsumer).addProfileData(new ProfileData()
				.setNamespace("Workers-Disk-test")
				.setName("percentTempDiskSpaceUsed")
				.setValue(89.0)
				.setUnit(StandardUnit.Percent.name())
				.setDimension(Map.of(
					"machineId", "someVMID"
				)));
		
		verifyZeroInteractions(mockLogger);
	}
}
