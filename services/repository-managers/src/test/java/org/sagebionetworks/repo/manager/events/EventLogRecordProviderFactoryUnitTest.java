package org.sagebionetworks.repo.manager.events;

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

@ExtendWith(MockitoExtension.class)
public class EventLogRecordProviderFactoryUnitTest {

	@Mock
	private SynapseEvent mockUnregisteredEvent;

	@Mock
	private EventLogRecordProvider<EventStub> mockProvider;

	private EventLogRecordProviderFactory logProviderFactory;

	@BeforeEach
	public void before() {
		
		when(mockProvider.getEventClass()).thenReturn(EventStub.class);
		
		List<EventLogRecordProvider<? extends SynapseEvent>> providers = Collections.singletonList(mockProvider);
		
		logProviderFactory = new EventLogRecordProviderFactoryImpl(providers);
	}

	@Test
	public void testGetRegisteredProvider() {
		EventLogRecordProvider<EventStub> provider = logProviderFactory.getLogRecordProvider(EventStub.class);
		assertNotNull(provider);
		assertEquals(EventStub.class, provider.getEventClass());
	}
	
	@Test
	public void testUnregistredProvider() {
		Assertions.assertThrows(UnsupportedOperationException.class, () -> {
			logProviderFactory.getLogRecordProvider(mockUnregisteredEvent.getClass());
		});
	}

}
