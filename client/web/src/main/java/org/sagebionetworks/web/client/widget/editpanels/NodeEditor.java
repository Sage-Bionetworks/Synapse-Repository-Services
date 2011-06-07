package org.sagebionetworks.web.client.widget.editpanels;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.events.CancelEvent;
import org.sagebionetworks.web.client.events.CancelHandler;
import org.sagebionetworks.web.client.events.PersistSuccessEvent;
import org.sagebionetworks.web.client.events.PersistSuccessHandler;
import org.sagebionetworks.web.client.ontology.OntologyTerm;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.widget.editpanels.FormField.ColumnType;
import org.sagebionetworks.web.shared.NodeType;

import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
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
	private String editId;
	private NodeType nodeType;
	private JSONObject originalNode;
	private String parentId;	
	private final HandlerManager handlerManager = new HandlerManager(this);

	
	@Inject
	public NodeEditor(NodeEditorView view, NodeServiceAsync service, NodeEditorDisplayHelper nodeEditorDisplayHelper) {
		this.view = view;
		this.service = service;
		this.nodeEditorDisplayHelper = nodeEditorDisplayHelper;
		view.setPresenter(this);
	}	
	
	public Widget asWidget(NodeType type) {
		return asWidget(type, null, null);
	}
	
	public Widget asWidget(NodeType type, String editId) {
		return asWidget(type, editId, null);
	}
	
	public Widget asWidget(final NodeType type, final String editId, final String parentId) {
		this.editId = editId;
		this.nodeType = type;
		this.parentId = parentId;
		this.originalNode = null;		
		view.clear();
		
		
		// define columns based on type 
		if(type == null) throw new IllegalArgumentException("You must use a valid NodeType");		
		view.showLoading();
		
		final boolean isEditor;
		if(editId == null) isEditor = false;
		else isEditor = true;
		
		if(editId == null) {
			if(type == NodeType.LAYER) {
				// treat layers specially for now
				tempLayerCreateHack(type, editId, parentId);
			} else {
				service.getNodeJSONSchema(type, new AsyncCallback<String>() {				
					@Override
					public void onSuccess(String schema) {
						JSONObject schemaObj = JSONParser.parseStrict(schema).isObject();					
						originalNode = schemaObj;
						SpecificNodeTypeDeviation deviation = nodeEditorDisplayHelper.getNodeTypeDeviation(type);										
						List<FormField> formFields = getSchemaFormFields(schemaObj, deviation.getKeyToOntology());
						view.generateCreateForm(formFields, deviation.getDisplayString(), deviation.getCreateText(), deviation.getCreationIgnoreFields(), deviation.getKeyToOntology());
					}
					
					@Override
					public void onFailure(Throwable caught) {
						view.showErrorMessage("Unable to load form. Please reload the page and try again.");
					}
				});
			}
		} else {			
			if(type == NodeType.LAYER) {
				// treat layers specially for now
				tempLayerUpdateHack(type, editId, parentId);
			} else {
				// retrieve object and build ui
				service.getNodeJSONSchema(type, new AsyncCallback<String>() {				
					@Override
					public void onSuccess(String nodeJson) {
						JSONObject schemaObj = JSONParser.parseStrict(nodeJson).isObject();					
						final SpecificNodeTypeDeviation deviation = nodeEditorDisplayHelper.getNodeTypeDeviation(type);										
						final List<FormField> formFields = getSchemaFormFields(schemaObj, deviation.getKeyToOntology());
						
						// retrieve actual node for this id and fill in values
						service.getNodeJSON(type, editId, new AsyncCallback<String>() {
							@Override
							public void onSuccess(String nodeJsonString) {
								originalNode = JSONParser.parseStrict(nodeJsonString).isObject();
								view.generateEditForm(formFields, deviation.getDisplayString(), deviation.getEditText(), deviation.getCreationIgnoreFields(), deviation.getKeyToOntology(), originalNode);							
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
		}		
		
		return view.asWidget();
	}	

	@Override
	public void closeButtonSelected() {
		handlerManager.fireEvent(new CancelEvent());
	}
	
	@SuppressWarnings("unchecked")
	public void addCancelHandler(CancelHandler handler) {
		handlerManager.addHandler(CancelEvent.getType(), handler);
	}

	@SuppressWarnings("unchecked")
	public void addPersistSuccessHandler(PersistSuccessHandler handler) {
		handlerManager.addHandler(PersistSuccessEvent.getType(), handler);
	}

	
	/*
	 * Private Methods
	 */
	private static List<FormField> getSchemaFormFields(JSONObject schema, Map<String, OntologyTerm[]> getKeyToOntology) {
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
								
								// create an new form field
								FormField field; 
								if(getKeyToOntology.containsKey(propertyName)) {
									field = new FormField(propertyName, null, getKeyToOntology.get(propertyName), colType);
								} else {									
									field = new FormField(propertyName, "", colType);
								}
								formFields.add(field);
							}							
						}
					}
				}
			}
		}
		return formFields;
	}

	@Override
	public void persist(List<FormField> formFields) {
		if(nodeType == NodeType.LAYER) {
			tempLayerHackPersist(formFields);
		} else {
			final SpecificNodeTypeDeviation deviation = nodeEditorDisplayHelper.getNodeTypeDeviation(nodeType);
			if(editId != null) {
				// UPDATE
				JSONValue eTagJSON = originalNode.get(DisplayConstants.SERVICE_ETAG_KEY);
				String etag = eTagJSON != null ? eTagJSON.isString().stringValue() : null; 
				String updateJson = mergeChangesIntoJsonString(originalNode, formFields, deviation.getCreationIgnoreFields(), parentId);
				service.updateNode(nodeType, editId, updateJson, etag, new AsyncCallback<String>() {		
					@Override
					public void onSuccess(String result) {
						view.showPersistSuccess();
						handlerManager.fireEvent(new PersistSuccessEvent());
					}
					
					@Override
					public void onFailure(Throwable caught) {
						view.showPersistFail();
					}
				});
			} else {
				// CREATE			
				String createJson = formFieldsToJsonString(formFields, deviation.getCreationIgnoreFields(), parentId);
				service.createNode(nodeType, createJson, new AsyncCallback<String>() {		
					@Override
					public void onSuccess(String result) {
						view.showPersistSuccess();
						handlerManager.fireEvent(new PersistSuccessEvent());
					}
					
					@Override
					public void onFailure(Throwable caught) {
						view.showPersistFail();
					}
				});
			}	
		}
	}


	/*
	 * Private Methods
	 */
	private static String mergeChangesIntoJsonString(JSONObject jsonObject, List<FormField> formFields, List<String> ignoreFields, String parentId) {
		JSONObject merged = new JSONObject(jsonObject.getJavaScriptObject()); // make a copy
		for(FormField formField : formFields) {
			if(ignoreFields.contains(formField.getKey())) continue;
			if(formField.isOntologyBased()) {
				// get ontology value
				OntologyTerm value = formField.getOntologyValue();
				if(value != null)
					merged.put(formField.getKey(), new JSONString(value.getValue()));
			} else {
				if(formField.getKey().matches(".+Date$")) {
					// date field
					Date date = DisplayConstants.DATE_FORMAT_SERVICES.parse(formField.getValue());
					merged.put(formField.getKey(), new JSONNumber(date.getTime()));
				} else {
					// normal field
					merged.put(formField.getKey(), new JSONString(formField.getValue()));
				}
			}
		}				
		if(parentId != null) {
			merged.put(DisplayConstants.SERVICE_PARENT_ID_KEY, new JSONString(parentId));
		}
		return merged.toString();
	}
	
	private static String formFieldsToJsonString(List<FormField> formFields, List<String> ignoreFields, String parentId) {
		JSONObject json = new JSONObject();
		for(FormField formField : formFields) {
			if(ignoreFields.contains(formField.getKey())) continue;
			json.put(formField.getKey(), new JSONString(formField.getValue()));
		}
		if(parentId != null) {
			json.put(DisplayConstants.SERVICE_PARENT_ID_KEY, new JSONString(parentId));
		}
		return json.toString();
	}

	
	/*
	 * Temp Hacks for two layers
	 */
	private void tempLayerUpdateHack(final NodeType type, final String editId, final String parentId) {
		// retrieve object and build ui
		service.getNodeJSONSchemaTwoLayer(type, NodeType.DATASET, parentId, new AsyncCallback<String>() {				
			@Override
			public void onSuccess(String nodeJson) {
				JSONObject schemaObj = JSONParser.parseStrict(nodeJson).isObject();					
				final SpecificNodeTypeDeviation deviation = nodeEditorDisplayHelper.getNodeTypeDeviation(type);										
				final List<FormField> formFields = getSchemaFormFields(schemaObj, deviation.getKeyToOntology());
				
				// retrieve actual node for this id and fill in values
				service.getNodeJSONTwoLayer(type, editId, NodeType.DATASET, parentId, new AsyncCallback<String>() { 
					@Override
					public void onSuccess(String nodeJsonString) {
						originalNode = JSONParser.parseStrict(nodeJsonString).isObject();
						view.generateEditForm(formFields, deviation.getDisplayString(), deviation.getEditText(), deviation.getCreationIgnoreFields(), deviation.getKeyToOntology(), originalNode);							
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

	private void tempLayerCreateHack(final NodeType type, String editId, String parentId) {
		service.getNodeJSONSchemaTwoLayer(type, NodeType.DATASET, parentId, new AsyncCallback<String>() {				
			@Override
			public void onSuccess(String schema) {
				JSONObject schemaObj = JSONParser.parseStrict(schema).isObject();					
				originalNode = schemaObj;
				SpecificNodeTypeDeviation deviation = nodeEditorDisplayHelper.getNodeTypeDeviation(type);										
				List<FormField> formFields = getSchemaFormFields(schemaObj, deviation.getKeyToOntology());
				view.generateCreateForm(formFields, deviation.getDisplayString(), deviation.getCreateText(), deviation.getCreationIgnoreFields(), deviation.getKeyToOntology());
			}
			
			@Override
			public void onFailure(Throwable caught) {
				view.showErrorMessage("Unable to load form. Please reload the page and try again.");
			}
		});
	}

	private void tempLayerHackPersist(List<FormField> formFields) {
		final SpecificNodeTypeDeviation deviation = nodeEditorDisplayHelper.getNodeTypeDeviation(nodeType);
		if(editId != null) {
			// UPDATE
			JSONValue eTagJSON = originalNode.get(DisplayConstants.SERVICE_ETAG_KEY);
			String etag = eTagJSON != null ? eTagJSON.isString().stringValue() : null; 
			String updateJson = mergeChangesIntoJsonString(originalNode, formFields, deviation.getCreationIgnoreFields(), parentId);
			service.updateNodeTwoLayer(nodeType, editId, updateJson, etag, NodeType.DATASET, parentId, new AsyncCallback<String>() {		
				@Override
				public void onSuccess(String result) {
					view.showPersistSuccess();
					handlerManager.fireEvent(new PersistSuccessEvent());
				}
				
				@Override
				public void onFailure(Throwable caught) {
					view.showPersistFail();
				}
			});
		} else {
			// CREATE			
			String createJson = formFieldsToJsonString(formFields, deviation.getCreationIgnoreFields(), parentId);
			service.createNodeTwoLayer(nodeType, createJson, NodeType.DATASET, parentId, new AsyncCallback<String>() {		
				@Override
				public void onSuccess(String result) {
					view.showPersistSuccess();
					handlerManager.fireEvent(new PersistSuccessEvent());
				}
				
				@Override
				public void onFailure(Throwable caught) {
					view.showPersistFail();
				}
			});
		}	
	}
	
}
