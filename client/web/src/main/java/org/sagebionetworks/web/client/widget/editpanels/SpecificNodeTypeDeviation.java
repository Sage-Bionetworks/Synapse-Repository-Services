package org.sagebionetworks.web.client.widget.editpanels;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.ontology.Enumeration;
import org.sagebionetworks.web.shared.NodeType;

public class SpecificNodeTypeDeviation {

	private NodeType nodeType;
	private String displayString;
	private String createText;
	private String editText;
	private List<String> creationRequiredFields;
	private List<String> updateShowFields;	
	private Map<String, Enumeration> keyToOntology;	
		
	public SpecificNodeTypeDeviation() { }
		
	public SpecificNodeTypeDeviation(NodeType nodeType, String displayString,
			String createText, String editText,
			List<String> creationRequiredFields, List<String> updateShowFields) {
		super();
		this.nodeType = nodeType;
		this.displayString = displayString;
		this.createText = createText;
		this.editText = editText;
		this.creationRequiredFields = creationRequiredFields;
		this.updateShowFields = updateShowFields;
	}

	public NodeType getNodeType() {
		return nodeType;
	}
	public void setNodeType(NodeType nodeType) {
		this.nodeType = nodeType;
	}
	public String getDisplayString() {
		return displayString;
	}
	public void setDisplayString(String displayString) {
		this.displayString = displayString;
	}
	public String getCreateText() {
		return createText;
	}
	public void setCreateText(String createText) {
		this.createText = createText;
	}
	public String getEditText() {
		return editText;
	}
	public void setEditText(String editText) {
		this.editText = editText;
	}

	public List<String> getCreationRequiredFields() {
		return creationRequiredFields;
	}

	public void setCreationRequiredFields(List<String> creationRequiredFields) {
		this.creationRequiredFields = creationRequiredFields;
	}

	public Map<String, Enumeration> getKeyToOntology() {
		return keyToOntology;
	}

	public void setKeyToOntology(Map<String, Enumeration> keyToOntology) {
		this.keyToOntology = keyToOntology;
	}

	public List<String> getUpdateShowFields() {
		return updateShowFields;
	}

	public void setUpdateShowFields(List<String> updateShowFields) {
		this.updateShowFields = updateShowFields;
	}
	
}
