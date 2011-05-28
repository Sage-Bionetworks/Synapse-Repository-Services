package org.sagebionetworks.web.client.widget.editpanels;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.widget.editpanels.FormField.ColumnType;
import org.sagebionetworks.web.shared.NodeType;

import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class NodeEditor implements NodeEditorView.Presenter {

	private static final String PROPERTY_TYPE_KEY = "type";
	private static final String SCHEMA_PROPERTIES_KEY = "properties";
	private static final String FIELD_TYPE_STRING = "string";
	private static final String FIELD_TYPE_NUMBER = "number";
	private static final String FIELD_TYPE_BOOLEAN = "boolean";
	
	private NodeEditorView view;
	private NodeServiceAsync service;
	private NodeEditorDisplayHelper nodeEditorDisplayHelper;
	
	@Inject
	public NodeEditor(NodeEditorView view, NodeServiceAsync service, NodeEditorDisplayHelper nodeEditorDisplayHelper) {
		this.view = view;
		this.service = service;
		this.nodeEditorDisplayHelper = nodeEditorDisplayHelper;
		view.setPresenter(this);
	}	
	
	public Widget asWidget(final NodeType type, final String editId) {
		// define columns based on type 
		if(type == null) throw new IllegalArgumentException("You must use a valid NodeType");		
		view.showLoading();
		
		final boolean isEditor;
		if(editId == null) isEditor = false;
		else isEditor = true;
		
		if(editId == null) {
			service.getNodeJSONSchema(type, new AsyncCallback<String>() {				
				@Override
				public void onSuccess(String schema) {
					JSONObject schemaObj = JSONParser.parseStrict(schema).isObject();					
					List<FormField> formFields = getSchemaFormFields(schemaObj);
					SpecificNodeTypeDeviation deviation = nodeEditorDisplayHelper.getNodeTypeDeviation(type);										
					view.generateCreateForm(formFields, deviation.getDisplayString(), deviation.getCreateText(), deviation.getCreationIgnoreFields());
				}
				
				@Override
				public void onFailure(Throwable caught) {
					view.showErrorMessage("Unable to load form. Please reload the page and try again.");
				}
			});
		} else {
			// retrieve object and build ui
			service.getNodeJSONSchema(type, new AsyncCallback<String>() {				
				@Override
				public void onSuccess(String nodeJson) {
					JSONObject schemaObj = JSONParser.parseStrict(nodeJson).isObject();					
					final List<FormField> formFields = getSchemaFormFields(schemaObj);
					final SpecificNodeTypeDeviation deviation = nodeEditorDisplayHelper.getNodeTypeDeviation(type);										
					
					// retrieve actual node for this id and fill in values
					service.getNodeJSON(type, editId, new AsyncCallback<String>() {
						@Override
						public void onSuccess(String nodeJsonString) {
							JSONObject nodeObject = JSONParser.parseStrict(nodeJsonString).isObject();							
							view.generateEditForm(formFields, deviation.getDisplayString(), deviation.getCreateText(), deviation.getCreationIgnoreFields(), nodeObject);							
						}
						
						@Override
						public void onFailure(Throwable caught) {
							view.showErrorMessage("Unable to load data. Please reload the page and try again.");
						}
					});
					
					
					
				}
				
				@Override
				public void onFailure(Throwable caught) {
					view.showErrorMessage("Unable to load form. Please reload the page and try again.");
				}
			});

		}		
		
		return view.asWidget();
	}	

	
	/*
	 * Private Methods
	 */
	private static List<FormField> getSchemaFormFields(JSONObject schema) {
		List<FormField> formFields = new ArrayList<FormField>();
		if(schema != null) {
			if(schema.containsKey(SCHEMA_PROPERTIES_KEY)) {
				JSONObject properties = schema.get(SCHEMA_PROPERTIES_KEY).isObject();
				if(properties != null) {
					Set<String> keys = properties.keySet();
					for(String propertyName : keys) {							
						JSONObject propertyObj = properties.get(propertyName).isObject();
						if(propertyObj != null) {
							if(propertyObj.containsKey(PROPERTY_TYPE_KEY)) {
								ColumnType colType = null;
								String propertyType = propertyObj.get(PROPERTY_TYPE_KEY).isString().stringValue();								
								if(FIELD_TYPE_STRING.equals(propertyType)) {
									colType = ColumnType.STRING;
								} else if(FIELD_TYPE_NUMBER.equals(propertyType)) {
									if(propertyName.matches(".*Date$")) { 
										colType = ColumnType.DATE;
									} else {
										//Log.error("Unknown Type: " + propertyType);
										continue;
									}
								} else if(FIELD_TYPE_NUMBER.equals(propertyType)) {
									colType = ColumnType.BOOLEAN;
								} else {
									//Log.error("Unknown Type: " + propertyType);
									continue;
								}
								try {
									FormField field = new FormField(propertyName, "", colType);
									formFields.add(field);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}							
						}
					}
				}
			}
		}
		return formFields;
	}
	
}
