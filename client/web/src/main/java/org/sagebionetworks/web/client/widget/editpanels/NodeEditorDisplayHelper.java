package org.sagebionetworks.web.client.widget.editpanels;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.ontology.OntologyTerm;
import org.sagebionetworks.web.client.ontology.StaticOntologies;
import org.sagebionetworks.web.shared.NodeType;

import com.google.inject.Inject;

public class NodeEditorDisplayHelper {

	private Map<NodeType, SpecificNodeTypeDeviation> typeToDevaition;
		
	@Inject
	public NodeEditorDisplayHelper() {		
		typeToDevaition = new HashMap<NodeType, SpecificNodeTypeDeviation>();		
		typeToDevaition.put(NodeType.DATASET, createDatasetDeviation());
		typeToDevaition.put(NodeType.LAYER, createLayerDeviation());
		typeToDevaition.put(NodeType.PROJECT, createProjectDeviation());		
	}

	public SpecificNodeTypeDeviation getNodeTypeDeviation(NodeType type) {
		if(typeToDevaition.containsKey(type)) {
			return typeToDevaition.get(type);
		}
		return null;
	}
			
	/*
	 * Private Methods
	 */
	// DATASET
	private SpecificNodeTypeDeviation createDatasetDeviation() {
		SpecificNodeTypeDeviation deviation = new SpecificNodeTypeDeviation(
				NodeType.DATASET, "Dataset",
				DisplayConstants.CREATE_DATASET_TEXT,
				DisplayConstants.EDIT_DATASET_TEXT,
				Arrays.asList(new String[] {"annotations", "accessControlList", "id", "creationDate", "creator", "parentId", "uri", "etag", "layers", "locations", "hasExpressionData", "hasGeneticData", "hasClinicalData"}),
				Arrays.asList(new String[] {"annotations", "accessControlList", "id", "creationDate", "creator", "parentId", "uri", "etag", "layers", "locations", "hasExpressionData", "hasGeneticData", "hasClinicalData"}));
		Map<String, OntologyTerm[]> keyToOntology = new HashMap<String, OntologyTerm[]>();
		keyToOntology.put(DisplayConstants.SERVICE_STATUS_KEY, StaticOntologies.STATUS);		
		deviation.setKeyToOntology(keyToOntology);
			
		return deviation;
	}
	
	// LAYER
	private SpecificNodeTypeDeviation createLayerDeviation() {
		SpecificNodeTypeDeviation deviation = new SpecificNodeTypeDeviation(
				NodeType.LAYER, "Layer", DisplayConstants.CREATE_LAYER_TEXT,
				DisplayConstants.EDIT_LAYER_TEXT,
				Arrays.asList(new String[] {"annotations", "accessControlList", "id", "creationDate", "creator", "parentId", "uri", "etag", "locations", "previews", "description", 
						"publicationDate", "releaseNotes", "tissueType", "processingFacility", "qcBy", "qcDate"}),
				Arrays.asList(new String[] {"annotations", "accessControlList", "id", "creationDate", "creator", "parentId", "uri", "etag", "locations", "previews"}));
		Map<String, OntologyTerm[]> keyToOntology = new HashMap<String, OntologyTerm[]>();
		keyToOntology.put(DisplayConstants.SERVICE_STATUS_KEY, StaticOntologies.STATUS);
		keyToOntology.put(DisplayConstants.SERVICE_LAYER_TYPE_KEY, StaticOntologies.LAYER_TYPES);
		deviation.setKeyToOntology(keyToOntology);
			
		return deviation;
	}
	
	// PROJECT
	private SpecificNodeTypeDeviation createProjectDeviation() {
		SpecificNodeTypeDeviation deviation = new SpecificNodeTypeDeviation(
				NodeType.PROJECT, "Project",
				DisplayConstants.CREATE_PROJECT_TEXT,
				DisplayConstants.EDIT_PROJECT_TEXT, 
				Arrays.asList(new String[] {"annotations", "accessControlList", "id", "creationDate", "creator", "parentId", "uri", "etag"}),
				Arrays.asList(new String[] {"annotations", "accessControlList", "id", "creationDate", "creator", "parentId", "uri", "etag"}));
		Map<String, OntologyTerm[]> keyToOntology = new HashMap<String, OntologyTerm[]>();
		deviation.setKeyToOntology(keyToOntology);
			
		return deviation;
	}
	
	
	
	
}
