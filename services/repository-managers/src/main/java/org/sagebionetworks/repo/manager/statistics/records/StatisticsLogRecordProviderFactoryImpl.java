package org.sagebionetworks.repo.manager.statistics.records;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.manager.statistics.events.StatisticsEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatisticsLogRecordProviderFactoryImpl implements StatisticsLogRecordProviderFactory {

	// Populated on injection
	private Map<Class<? extends StatisticsEvent>, StatisticsEventLogRecordProvider<? extends StatisticsEvent>> logRecordProviderMap = new HashMap<>();

	@Autowired
	public StatisticsLogRecordProviderFactoryImpl(List<StatisticsEventLogRecordProvider<? extends StatisticsEvent>> logRecordProviders) {
		logRecordProviders.forEach(provider -> {
			this.logRecordProviderMap.put(provider.getEventClass(), provider);
		});
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E extends StatisticsEvent> StatisticsEventLogRecordProvider<E> getLogRecordProvider(E event) {
		Class<E> eventClass = (Class<E>) event.getClass();
		
		StatisticsEventLogRecordProvider<E> provider = (StatisticsEventLogRecordProvider<E>) logRecordProviderMap.get(eventClass);
		
		if (provider == null) {
			throw new UnsupportedOperationException(
					"Log record provider not found for event of type " + eventClass.getSimpleName());
		}
		return provider;
	}

}
