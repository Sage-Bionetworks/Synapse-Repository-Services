package org.sagebionetworks.repo.manager.statistics.records;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.sagebionetworks.repo.manager.statistics.events.StatisticsEvent;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class StatisticsLogRecordProviderFactoryUnitTest {

	@Mock
	private StatisticsEvent mockUnregisteredEvent;

	@Mock
	private StatisticsEventLogRecordProvider<StatisticsEventMock> mockProvider;

	private StatisticsLogRecordProviderFactory logProviderFactory;

	@BeforeEach
	public void before() {
		
		when(mockProvider.getEventClass()).thenReturn(StatisticsEventMock.class);
		
		List<StatisticsEventLogRecordProvider<? extends StatisticsEvent>> providers = Collections.singletonList(mockProvider);
		
		logProviderFactory = new StatisticsLogRecordProviderFactoryImpl(providers);
	}

	@Test
	public void testGetRegisteredProvider() {
		StatisticsEventLogRecordProvider<StatisticsEventMock> provider = logProviderFactory.getLogRecordProvider(StatisticsEventMock.class);
		assertNotNull(provider);
		assertEquals(StatisticsEventMock.class, provider.getEventClass());
	}
	
	@Test
	public void testUnregistredProvider() {
		Assertions.assertThrows(UnsupportedOperationException.class, () -> {
			logProviderFactory.getLogRecordProvider(mockUnregisteredEvent.getClass());
		});
	}

}
