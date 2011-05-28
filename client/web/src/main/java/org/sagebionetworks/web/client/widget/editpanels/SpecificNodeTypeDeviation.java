package org.sagebionetworks.web.client.widget.editpanels;

import java.util.List;

import org.sagebionetworks.web.shared.NodeType;

public class SpecificNodeTypeDeviation {

	private NodeType nodeType;
	private String displayString;
	private String createText;
	private String editText;
	private List<String> creationIgnoreFields;
		
	public SpecificNodeTypeDeviation() { }
		
	public SpecificNodeTypeDeviation(NodeType nodeType, String displayString,
			String createText, String editText,
			List<String> creationShowOnlyFields) {
		super();
		this.nodeType = nodeType;
		this.displayString = displayString;
		this.createText = createText;
		this.editText = editText;
		this.creationIgnoreFields = creationShowOnlyFields;
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

	public List<String> getCreationIgnoreFields() {
		return creationIgnoreFields;
	}

	public void setCreationIgnoreFields(List<String> creationIgnoreFields) {
		this.creationIgnoreFields = creationIgnoreFields;
	}
	
}
