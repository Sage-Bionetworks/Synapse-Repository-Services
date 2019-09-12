package org.sagebionetworks.repo.manager.statistics.monthly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatisticsMonthlyProcessorProviderImpl implements StatisticsMonthlyProcessorProvider {

	private Map<StatisticsObjectType, List<StatisticsMonthlyProcessor>> processorsMap = new HashMap<>();

	@Autowired
	public StatisticsMonthlyProcessorProviderImpl(List<StatisticsMonthlyProcessor> processors) {
		processors.forEach(processor -> {
			List<StatisticsMonthlyProcessor> registered = processorsMap.get(processor.getSupportedType());
			if (registered == null) {
				processorsMap.put(processor.getSupportedType(), registered = new ArrayList<>());
			}
			registered.add(processor);
		});
	}

	@Override
	public List<StatisticsMonthlyProcessor> getProcessors(StatisticsObjectType objectType) {
		ValidateArgument.required(objectType, "objectType");

		List<StatisticsMonthlyProcessor> processors = processorsMap.get(objectType);

		if (processors == null || processors.isEmpty()) {
			throw new IllegalArgumentException("No processor was found for object type " + objectType);
		}

		return processors;
	}

}
