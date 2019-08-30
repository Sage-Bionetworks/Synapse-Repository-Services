package org.sagebionetworks.repo.manager.statistics.monthly;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatisticsMonthlyProcessorProviderImpl implements StatisticsMonthlyProcessorProvider {

	private Map<StatisticsObjectType, StatisticsMonthlyProcessor> processorsMap = new HashMap<>();

	@Autowired
	public StatisticsMonthlyProcessorProviderImpl(List<StatisticsMonthlyProcessor> processors) {
		processors.forEach(processor -> {
			processorsMap.put(processor.getSupportedType(), processor);
		});
	}

	@Override
	public StatisticsMonthlyProcessor getMonthlyProcessor(StatisticsObjectType objectType) {
		ValidateArgument.required(objectType, "objectType");

		StatisticsMonthlyProcessor processor = processorsMap.get(objectType);

		if (processor == null) {
			throw new IllegalArgumentException("Object type " + objectType + " not supported yet");
		}

		return processor;
	}

}
