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
import org.sagebionetworks.repo.manager.statistics.events.StatisticsEvent;

@ExtendWith(MockitoExtension.class)
public class StatisticsLogRecordProviderFactoryUnitTest {

	@Mock
	private StatisticsEvent mockUnregisteredEvent;

	@Mock
	private StatisticsEventLogRecordProvider<StatisticsEventStub> mockProvider;

	private StatisticsLogRecordProviderFactory logProviderFactory;

	@BeforeEach
	public void before() {
		
		when(mockProvider.getEventClass()).thenReturn(StatisticsEventStub.class);
		
		List<StatisticsEventLogRecordProvider<? extends StatisticsEvent>> providers = Collections.singletonList(mockProvider);
		
		logProviderFactory = new StatisticsLogRecordProviderFactoryImpl(providers);
	}

	@Test
	public void testGetRegisteredProvider() {
		StatisticsEventLogRecordProvider<StatisticsEventStub> provider = logProviderFactory.getLogRecordProvider(StatisticsEventStub.class);
		assertNotNull(provider);
		assertEquals(StatisticsEventStub.class, provider.getEventClass());
	}
	
	@Test
	public void testUnregistredProvider() {
		Assertions.assertThrows(UnsupportedOperationException.class, () -> {
			logProviderFactory.getLogRecordProvider(mockUnregisteredEvent.getClass());
		});
	}
	
	private class StatisticsEventStub implements StatisticsEvent {

		@Override
		public Long getTimestamp() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Long getUserId() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}

}
