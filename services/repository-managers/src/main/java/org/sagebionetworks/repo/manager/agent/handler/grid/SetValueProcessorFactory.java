package org.sagebionetworks.repo.manager.agent.handler.grid;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.update.SetValue;
import org.springframework.stereotype.Service;

import com.google.common.base.Functions;

@Service
public class SetValueProcessorFactory {

	private final Map<Class<? extends SetValue>, SetValueProcessor<?>> processorMap;

	SetValueProcessorFactory(List<SetValueProcessor<?>> list) {
	    this.processorMap = list.stream()
	        .collect(Collectors.toMap(
	            SetValueProcessor::getSetValueClass,
	            Functions.identity()
	        ));
	}

	public Optional<ConValue> createConValue(RowView row, SetValue sv, JSONObject rawSetValue) {
		SetValueProcessor processor = processorMap.get(sv.getClass());
		if (processor == null) {
			throw new IllegalArgumentException("No processor found for SetValue type: " + sv.getClass().getName());
		}
		return processor.createConValue(row, sv, rawSetValue);
	}

}
