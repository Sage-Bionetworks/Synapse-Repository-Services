package org.sagebionetworks.repo.manager.events;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EventLogRecordProviderFactoryImpl implements EventLogRecordProviderFactory {

	// Populated on injection
	private Map<Class<? extends SynapseEvent>, EventLogRecordProvider<? extends SynapseEvent>> logRecordProviderMap = new HashMap<>();

	@Autowired
	public EventLogRecordProviderFactoryImpl(List<EventLogRecordProvider<? extends SynapseEvent>> logRecordProviders) {
		logRecordProviders.forEach(provider -> {
			this.logRecordProviderMap.put(provider.getEventClass(), provider);
		});
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E extends SynapseEvent> EventLogRecordProvider<E> getLogRecordProvider(Class<E> eventClass) {
		
		EventLogRecordProvider<E> provider = (EventLogRecordProvider<E>) logRecordProviderMap.get(eventClass);
		
		if (provider == null) {
			throw new UnsupportedOperationException("Log record provider not found for event of type " + eventClass.getSimpleName());
		}
		
		return provider;
	}

}
