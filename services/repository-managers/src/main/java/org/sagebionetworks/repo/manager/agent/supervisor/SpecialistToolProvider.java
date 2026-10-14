package org.sagebionetworks.repo.manager.agent.supervisor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

/**
 * Provides the specialist delegation tools by name so that each supervisor is given only the
 * specialists it needs, instead of the full set. The delegation tools themselves are defined
 * once in {@link SupervisorTools}; this provider indexes them so a supervisor can select a
 * focused subset without duplicating tool definitions. {@link SupervisorTools} extends
 * {@code JSONEntityToolBase}, so each callback it returns is already wrapped so that every
 * supervisor-to-specialist delegation is logged.
 */
@Service
public class SpecialistToolProvider {

	private final Map<String, ToolCallback> toolsByName;

	public SpecialistToolProvider(SupervisorTools supervisorTools) {
		Map<String, ToolCallback> index = new HashMap<>();
		for (ToolCallback tool : supervisorTools.getToolCallbacks()) {
			index.put(tool.getToolDefinition().name(), tool);
		}
		this.toolsByName = index;
	}

	/**
	 * Returns the delegation tools for the given names, in the order requested.
	 *
	 * @throws IllegalArgumentException if any requested name is not a known specialist tool.
	 */
	public List<ToolCallback> getTools(String... names) {
		List<ToolCallback> selected = new ArrayList<>(names.length);
		for (String name : names) {
			ToolCallback tool = toolsByName.get(name);
			if (tool == null) {
				throw new IllegalArgumentException("Unknown specialist tool: " + name);
			}
			selected.add(tool);
		}
		return selected;
	}
}
