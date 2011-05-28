package org.sagebionetworks.web.client.widget.editpanels;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.shared.NodeType;

import com.google.inject.Inject;

public class NodeEditorDisplayHelper {

	private Map<NodeType, SpecificNodeTypeDeviation> typeToDevaition;
	
	@Inject
	public NodeEditorDisplayHelper() {		
		typeToDevaition = new HashMap<NodeType, SpecificNodeTypeDeviation>();
		
		typeToDevaition.put(NodeType.DATASET, new SpecificNodeTypeDeviation(
				NodeType.DATASET, "Dataset",
				DisplayConstants.CREATE_DATASET_TEXT,
				DisplayConstants.EDIT_DATASET_TEXT,
				Arrays.asList(new String[] {"annotations", "id", "creationDate", "parentId", "uri", "etag", "layer", "hasExpressionData", "hasGeneticData", "hasClinicalData"})));
		
		typeToDevaition.put(NodeType.LAYER, new SpecificNodeTypeDeviation(
				NodeType.LAYER, "Layer", DisplayConstants.CREATE_LAYER_TEXT,
				DisplayConstants.EDIT_LAYER_TEXT,
				null)); // TODO : update creation fields
		
		typeToDevaition.put(NodeType.PROJECT, new SpecificNodeTypeDeviation(
				NodeType.PROJECT, "Project",
				DisplayConstants.CREATE_PROJECT_TEXT,
				DisplayConstants.EDIT_PROJECT_TEXT, 
				Arrays.asList(new String[] {"annotations", "id", "creationDate", "parentId", "uri", "etag"})));
		
	}
	
	public SpecificNodeTypeDeviation getNodeTypeDeviation(NodeType type) {
		if(typeToDevaition.containsKey(type)) {
			return typeToDevaition.get(type);
		}
		return null;
	}
			
}
