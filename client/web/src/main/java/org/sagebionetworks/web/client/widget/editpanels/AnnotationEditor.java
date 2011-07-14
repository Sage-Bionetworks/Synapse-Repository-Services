package org.sagebionetworks.web.client.widget.editpanels;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.widget.editpanels.FormField.ColumnType;
import org.sagebionetworks.web.shared.Annotations;
import org.sagebionetworks.web.shared.NodeType;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class AnnotationEditor implements AnnotationEditorView.Presenter {
 
    private static final String STRING_ANNOTATIONS = "stringAnnotations";
    private static final String LONG_ANNOTATIONS = "longAnnotations";
    private static final String DOUBLE_ANNOTATIONS = "doubleAnnotations";
    private static final String DATE_ANNOTATIONS = "dateAnnotations";
    private static final String BLOB_ANNOTATIONS = "blobAnnotations";
    
	private AnnotationEditorView view;
	private NodeServiceAsync service;
	private NodeEditorDisplayHelper nodeEditorDisplayHelper;
    private NodeType nodetype;
    private String nodeId;
    private JSONObject originalAnnotationObject;
    private PlaceChanger placeChanger;
    private NodeModelCreator nodeModelCreator;
 
    @Inject
    public AnnotationEditor(AnnotationEditorView view, NodeServiceAsync service, NodeEditorDisplayHelper nodeEditorDisplayHelper, NodeModelCreator nodeModelCreator) {
        this.view = view;
		this.service = service;
		this.nodeEditorDisplayHelper = nodeEditorDisplayHelper;
		this.nodeModelCreator = nodeModelCreator;
        view.setPresenter(this);
    }

    public void setPlaceChanger(PlaceChanger placeChanger) {
    	this.placeChanger = placeChanger;
    }
    
    public void setResource(NodeType type, String id) {
    	this.nodetype = type;
    	this.nodeId = id;
    	
    	view.clear();
    	
		// define columns based on type 
		if(type == null) throw new IllegalArgumentException("You must use a valid NodeType");		
		view.showLoading();
		
		// Generate form fields from the object. NOT IDEAL. Would like to use annotation JSON schema in the future
		service.getNodeAnnotationsJSON(type, id, new AsyncCallback<String>() {				
			@Override
			public void onSuccess(String annotationJsonString) {				
				// TODO : convert this class to working with the Annotations object and not the JSONObject
				originalAnnotationObject = JSONParser.parseStrict(annotationJsonString).isObject();
				try {
					Annotations annotations = nodeModelCreator.createAnnotations(annotationJsonString);
				} catch (RestServiceException ex) {
					DisplayUtils.handleServiceException(ex, placeChanger);
					return;
				}
				
				List<FormField> formFields = generateFieldsFromAnnotations(originalAnnotationObject); 							
				SpecificNodeTypeDeviation deviation = nodeEditorDisplayHelper.getNodeTypeDeviation(nodetype);										
				view.generateAnnotationForm(formFields, deviation.getDisplayString(), DisplayConstants.EDIT_ANNOTATIONS_TEXT);
			}
			
			@Override
			public void onFailure(Throwable caught) {
				view.showErrorMessage("Unable to load form. Please reload the page and try again.");
			}
		});
    }
    
	@Override
	public void persist(List<FormField> formFields) {		
		// updates annoataions
		//final SpecificNodeTypeDeviation deviation = nodeEditorDisplayHelper.getNodeTypeDeviation(nodeType);
		JSONValue eTagJSON = originalAnnotationObject.get(DisplayConstants.SERVICE_ETAG_KEY);
		String etag = eTagJSON != null ? eTagJSON.isString().stringValue() : null; 
		
		String updateJson = formFieldsToUpdateJson(originalAnnotationObject, formFields);
		service.updateNodeAnnotations(nodetype, nodeId, updateJson, etag, new AsyncCallback<String>() {		
			@Override
			public void onSuccess(String result) {
				try {
					Annotations annotations = nodeModelCreator.createAnnotations(result);
				} catch (RestServiceException ex) {
					DisplayUtils.handleServiceException(ex, placeChanger);
				}
				view.showPersistSuccess();
			}
			
			@Override
			public void onFailure(Throwable caught) {
				view.showPersistFail();
			}
		});
			
	}
    
   	public Widget asWidget() {
   		view.setPresenter(this);
        return view.asWidget();
    }
    
    
    /*
     * Private Methods
     */
	private static List<FormField> generateFieldsFromAnnotations(JSONObject annotationObj) {
		List<FormField> formFields = new ArrayList<FormField>();
		if(annotationObj != null) {			
			if(annotationObj.containsKey(STRING_ANNOTATIONS)) 
				addFormFieldsByType(STRING_ANNOTATIONS, annotationObj, formFields);

			if(annotationObj.containsKey(LONG_ANNOTATIONS)) 
				addFormFieldsByType(LONG_ANNOTATIONS, annotationObj, formFields);			
			
			if(annotationObj.containsKey(DOUBLE_ANNOTATIONS)) 
				addFormFieldsByType(DOUBLE_ANNOTATIONS, annotationObj, formFields);
						
			if(annotationObj.containsKey(DATE_ANNOTATIONS))
				addFormFieldsByType(DATE_ANNOTATIONS, annotationObj, formFields);						
		}
		return formFields;		
	}

	private static void addFormFieldsByType(String annotationKey, JSONObject annotationObj, List<FormField> formFields) {
		JSONObject typeOfAnnotations = annotationObj.get(annotationKey).isObject();
		if(typeOfAnnotations != null) {
			for(String key : typeOfAnnotations.keySet()) {						
				String value = "";
				ColumnType ctype = null;
				JSONArray values = typeOfAnnotations.get(key).isArray();
				if(values != null) {
					JSONValue firstVal = values.get(0);
					if(firstVal != null) {
						if(annotationKey == LONG_ANNOTATIONS || annotationKey == DOUBLE_ANNOTATIONS) {						
							value = ((Double)firstVal.isNumber().doubleValue()).toString();
							ctype = annotationKey == LONG_ANNOTATIONS ? ColumnType.INTEGER : ColumnType.DECIMAL;
						} else if (annotationKey == STRING_ANNOTATIONS) {
							value = firstVal.isString().stringValue();
							ctype = ColumnType.STRING;
						} else if (annotationKey == DATE_ANNOTATIONS) {
							Double time = firstVal.isNumber().doubleValue();
							Date date = new Date(time.longValue());
							value = DisplayConstants.DATE_FORMAT_SERVICES.format(date);
							ctype = ColumnType.DATE;
						}
					}
				}									
				FormField field = new FormField(key, value, ctype);
				formFields.add(field);
			}
		}
	}

    private static String formFieldsToUpdateJson(JSONObject originalAnnotationObject, List<FormField> formFields) {
    	if(originalAnnotationObject.containsKey(STRING_ANNOTATIONS)) 
    		copyValuesIntoObject(originalAnnotationObject, STRING_ANNOTATIONS, formFields);  	
    	
    	if(originalAnnotationObject.containsKey(LONG_ANNOTATIONS))   		
    		copyValuesIntoObject(originalAnnotationObject, LONG_ANNOTATIONS, formFields);
    	
    	if(originalAnnotationObject.containsKey(DOUBLE_ANNOTATIONS))   		
    		copyValuesIntoObject(originalAnnotationObject, DOUBLE_ANNOTATIONS, formFields);
    	
    	if(originalAnnotationObject.containsKey(DATE_ANNOTATIONS))   		
    		copyValuesIntoObject(originalAnnotationObject, DATE_ANNOTATIONS, formFields);
    	
    	
		return originalAnnotationObject.toString();

	}

    private static void copyValuesIntoObject(JSONObject originalAnnotationObject, String annotationKey, List<FormField> formFields) {
		JSONObject typeOfAnnotations = originalAnnotationObject.get(STRING_ANNOTATIONS).isObject();
		if(typeOfAnnotations != null) {
			for(FormField formField : formFields) {
				if(annotationKey == STRING_ANNOTATIONS) { 
					if(formField.getType() == ColumnType.STRING)
						typeOfAnnotations.put(formField.getKey(), new JSONString(formField.getValue()));
				} else if (annotationKey == LONG_ANNOTATIONS) {
					if(formField.getType() == ColumnType.INTEGER)
						typeOfAnnotations.put(formField.getKey(), new JSONNumber(Double.parseDouble(formField.getValue())));
				} else if (annotationKey == DOUBLE_ANNOTATIONS) {
					if(formField.getType() == ColumnType.DECIMAL)
						typeOfAnnotations.put(formField.getKey(), new JSONNumber(Double.parseDouble(formField.getValue())));
				} else if (annotationKey == DATE_ANNOTATIONS) {
					if(formField.getType() == ColumnType.DATE) {
						Date date = DisplayConstants.DATE_FORMAT_SERVICES.parse(formField.getValue());
						typeOfAnnotations.put(formField.getKey(), new JSONNumber(date.getTime()));
					}
				}
			}
		}
    }
	
}