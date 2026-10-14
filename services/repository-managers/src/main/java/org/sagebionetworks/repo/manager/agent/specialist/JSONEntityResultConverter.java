package org.sagebionetworks.repo.manager.agent.specialist;

import java.lang.reflect.Type;

import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.lang.Nullable;

/**
 * Converts tool return values that implement {@link JSONEntity} to JSON using
 * the canonical Synapse serialization path ({@link JDOSecondaryPropertyUtils}).
 * For null or non-JSONEntity results, falls back to toString().
 */
public class JSONEntityResultConverter implements ToolCallResultConverter {

	@Override
	public String convert(@Nullable Object result, @Nullable Type returnType) {
		if (result == null) {
			return "null";
		}
		if (result instanceof JSONEntity jsonEntity) {
			return JDOSecondaryPropertyUtils.createJSONFromObject(jsonEntity);
		}
		return result.toString();
	}
}
