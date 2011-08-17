package org.sagebionetworks.web.client.widget.editpanels;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.ontology.Ontology;
import org.sagebionetworks.web.client.ontology.OntologyTerm;
import org.sagebionetworks.web.client.ontology.StaticOntologies;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.widget.editpanels.FormField.ColumnType;
import org.sagebionetworks.web.shared.Annotations;
import org.sagebionetworks.web.shared.NodeType;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;
import org.sagebionetworks.web.shared.users.AclAccessType;

import com.extjs.gxt.ui.client.widget.MessageBox;
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

    private enum PersistOperation { CREATE, UPDATE, DELETE }; 
    
	private AnnotationEditorView view;
	private NodeServiceAsync service;
	private NodeEditorDisplayHelper nodeEditorDisplayHelper;
    private NodeType nodetype;
    private String nodeId;
    private JSONObject originalAnnotationObject;
    private PlaceChanger placeChanger;
    private NodeModelCreator nodeModelCreator;
    private StaticOntologies staticOntologies;
    private List<FormField> formFields;
 
    @Inject
    public AnnotationEditor(AnnotationEditorView view, NodeServiceAsync service, NodeEditorDisplayHelper nodeEditorDisplayHelper, NodeModelCreator nodeModelCreator, StaticOntologies staticOntologies) {
        this.view = view;
		this.service = service;
		this.nodeEditorDisplayHelper = nodeEditorDisplayHelper;
		this.nodeModelCreator = nodeModelCreator;
		this.staticOntologies = staticOntologies;
        view.setPresenter(this);
    }

    public void setPlaceChanger(PlaceChanger placeChanger) {
    	this.placeChanger = placeChanger;
    }
    
    public void setResource(final NodeType type, final String id) {
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
					if(!DisplayUtils.handleServiceException(ex, placeChanger)) {
						onFailure(null);
					}
					return;
				}
								
				// check for update access, then create the grid in the view accordingly
				service.hasAccess(type, id, AclAccessType.UPDATE, new AsyncCallback<Boolean>() {
					@Override
					public void onSuccess(Boolean result) {
						setupFormAndGenerate(result);
					}

					@Override
					public void onFailure(Throwable caught) {
						// if access check fails, just provide annotations without editing.
						setupFormAndGenerate(false);
						view.showInfo("Notice", "Annotation editing is currently unavailable.");
					}
				});
			}
			
			@Override
			public void onFailure(Throwable caught) {
				view.showErrorMessage("Unable to load form. Please reload the page and try again.");
			}
		});
    }

	@Override
	public void editAnnotation(String key, String newValue) {		
		if(key != null && newValue != null) {
			for(FormField formField : formFields) {
				if(formField.getKey().equals(key)) {
					formField.setValue(newValue);
					persist(formFields, PersistOperation.UPDATE);
					return;
				}
			}
		}
	}
    
   	public Widget asWidget() {
   		view.setPresenter(this);
        return view.asWidget();
    }
    
	@Override
	public void addAnnotation(String key, ColumnEditType type) {
		addAnnotationField(key, type, null);
	}

	@Override
	public void addAnnotation(String key, ColumnEditType type, Ontology ontology) {
		addAnnotationField(key, type, ontology); 
	}

	@Override
	public void deleteAnnotation(String key) {
		if(key != null) {
			for(FormField formField : formFields) {
				if(formField.getKey().equals(key)) {
					// delete from the formFields as well as the original object 
					formFields.remove(formField);
					deleteFieldFromJsonObject(originalAnnotationObject, formField);					
					persist(formFields, PersistOperation.DELETE);
					return;
				}
			}
		}
	}	

	/*
     * Private Methods
     */   	

	private void setupFormAndGenerate(boolean editable) {
		formFields = generateFieldsFromAnnotations(originalAnnotationObject, staticOntologies.getAnnotationToOntology()); 							
		SpecificNodeTypeDeviation deviation = nodeEditorDisplayHelper.getNodeTypeDeviation(nodetype);										
		view.generateAnnotationForm(formFields, deviation.getDisplayString(), DisplayConstants.EDIT_ANNOTATIONS_TEXT, editable);						
		
		// add ontologies
		view.setOntologies(staticOntologies.getAnnotationToOntology().values());
	}

	private void persist(final List<FormField> formFields, final PersistOperation operation) {		
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
					originalAnnotationObject = JSONParser.parseStrict(result).isObject();
					view.updateAnnotations(formFields);										
					if(operation == PersistOperation.CREATE) {
						view.showAddAnnotationSuccess();
					} else if (operation == PersistOperation.DELETE) {
						view.showDeleteAnnotationSuccess();
					} else {
						view.showPersistSuccess();
					}
				} catch (RestServiceException ex) {
					DisplayUtils.handleServiceException(ex, placeChanger);
					onFailure(null);					
					return;
				}				
			}
			
			@Override
			public void onFailure(Throwable caught) {
				if(operation == PersistOperation.CREATE) {
					view.showAddAnnotationFail("An error occured creating the new Annotation.");
				} else if (operation == PersistOperation.DELETE) {
					view.showDeleteAnnotationFail();
				} else {
					view.showPersistFail();
				}					
				getCleanAnnotationObject();
			}
		});			
	}
	
	private void getCleanAnnotationObject() {
		// Generate form fields from the object. NOT IDEAL. Would like to use annotation JSON schema in the future
		service.getNodeAnnotationsJSON(nodetype, nodeId, new AsyncCallback<String>() {				
			@Override
			public void onSuccess(String annotationJsonString) {				
				// TODO : convert this class to working with the Annotations object and not the JSONObject
				originalAnnotationObject = JSONParser.parseStrict(annotationJsonString).isObject();
				try {
					Annotations annotations = nodeModelCreator.createAnnotations(annotationJsonString);
				} catch (RestServiceException ex) {
					if(!DisplayUtils.handleServiceException(ex, placeChanger)) {
						onFailure(null);
					}
					return;
				}

				// update form fields here and in the view
				formFields = generateFieldsFromAnnotations(originalAnnotationObject, staticOntologies.getAnnotationToOntology());
				view.updateAnnotations(formFields);
			}
			
			@Override
			public void onFailure(Throwable caught) {
				view.showErrorMessage("Unable to load form. Please reload the page and try again.");
			}
		});
	}
	
	private static List<FormField> generateFieldsFromAnnotations(JSONObject annotationObj, Map<String, Ontology> getKeyToOntology) {
		List<FormField> formFields = new ArrayList<FormField>();
		if(annotationObj != null) {			
			if(annotationObj.containsKey(STRING_ANNOTATIONS)) 
				addFormFieldsByType(STRING_ANNOTATIONS, annotationObj, formFields, getKeyToOntology);

			if(annotationObj.containsKey(LONG_ANNOTATIONS)) 
				addFormFieldsByType(LONG_ANNOTATIONS, annotationObj, formFields, getKeyToOntology);			
			
			if(annotationObj.containsKey(DOUBLE_ANNOTATIONS)) 
				addFormFieldsByType(DOUBLE_ANNOTATIONS, annotationObj, formFields, getKeyToOntology);
						
			if(annotationObj.containsKey(DATE_ANNOTATIONS))
				addFormFieldsByType(DATE_ANNOTATIONS, annotationObj, formFields, getKeyToOntology);						
		}
		return formFields;		
	}

	private static void addFormFieldsByType(String annotationKey, JSONObject annotationObj, List<FormField> formFields, Map<String, Ontology> getKeyToOntology) {
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
							value = DisplayConstants.DATE_FORMAT.format(date);
							ctype = ColumnType.DATE;
						}
					}
				}
				FormField field = null;
				if(getKeyToOntology.containsKey(key)) {
					OntologyTerm[] ontologyTerms = getKeyToOntology.get(key).getTerms();
					// Assure that current value is in ontology
					OntologyTerm oValue = null;
					for(OntologyTerm term : ontologyTerms) {
						if(term.getValue().equals(value)) 
							oValue = term;
					}
					if(oValue == null) {
						// value not found, set value to empty
						oValue = new OntologyTerm("", "");
					}
					// create ontology base FormField
					field = new FormField(key, oValue, ontologyTerms, ctype);
				} else {
					// create regular FormField
					field = new FormField(key, value, ctype);
				}
				formFields.add(field);					
				
			}
		}
	}

    /**
     * NOTE: fields are copied into the original object because not all operations of the annotation object are supported.
     * Thus just rebuilding the annotation object from FormFields is not possible.
     * @param originalAnnotationObject
     * @param formFields
     * @return
     */
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
		JSONObject typeOfAnnotations = originalAnnotationObject.get(annotationKey).isObject();
		if(typeOfAnnotations != null) {
			for(FormField formField : formFields) {
				if(annotationKey == STRING_ANNOTATIONS) { 
					if(formField.getType() == ColumnType.STRING) {
						JSONArray array = new JSONArray();
						array.set(0, new JSONString(formField.getValue()));
						typeOfAnnotations.put(formField.getKey(), array);
					}
				} else if (annotationKey == LONG_ANNOTATIONS) {
					if(formField.getType() == ColumnType.INTEGER) {
						JSONArray array = new JSONArray();
						array.set(0, new JSONNumber(Double.parseDouble(formField.getValue())));
						typeOfAnnotations.put(formField.getKey(), array);
					}
				} else if (annotationKey == DOUBLE_ANNOTATIONS) {
					if(formField.getType() == ColumnType.DECIMAL) {
						JSONArray array = new JSONArray();
						array.set(0, new JSONNumber(Double.parseDouble(formField.getValue())));
						typeOfAnnotations.put(formField.getKey(), array);
					}
				} else if (annotationKey == DATE_ANNOTATIONS) {
					if(formField.getType() == ColumnType.DATE) {
						String dateString = formField.getValue();
						Date date = DisplayConstants.DATE_FORMAT.parse(dateString);
						JSONArray array = new JSONArray();
						array.set(0, new JSONNumber(date.getTime()));
						typeOfAnnotations.put(formField.getKey(), array);
					}
				}
			}
		}
    }
	
	private void addAnnotationField(String key, ColumnEditType type, Ontology ontology) {
		// validate key
		String errorMessage = validateKey(key);
		if(errorMessage != null) {
			view.showAddAnnotationFail(errorMessage);
			return;
		}
		
		// TODO : incorporate ontology into the mix
		
		if(originalAnnotationObject.containsKey(key)) {
			view.showAddAnnotationFail("The annotation \"" + key + "\" already exists.");
		} else {
			String blankValue = "";
			ColumnType columnType = null;
			if(type == ColumnEditType.TEXT) columnType = ColumnType.STRING;
			else if (type == ColumnEditType.TEXTAREA) columnType = ColumnType.STRING; 
			else if (type == ColumnEditType.BOOLEAN) columnType = ColumnType.BOOLEAN;
			else if (type == ColumnEditType.COMBO) columnType = ColumnType.STRING;
			else if (type == ColumnEditType.DATE) { 
				columnType = ColumnType.DATE; 
				blankValue = DisplayConstants.DATE_FORMAT.format(new Date());
			}
			
			if(columnType != null) {
				if(ontology != null) {
					formFields.add(new FormField(key, new OntologyTerm("", ""), ontology.getTerms(), columnType));
				} else {
					formFields.add(new FormField(key, blankValue, columnType));					
				}
				persist(formFields, PersistOperation.CREATE);
			}
		}
	}

	private void deleteFieldFromJsonObject(JSONObject object, FormField formField) {
		String keyToDelete = formField.getKey();
		String typeKey = "";
		ColumnType type = formField.getType();
		switch (type) {
		case STRING:
			typeKey = STRING_ANNOTATIONS;
			break;
		case DATE:
			typeKey = DATE_ANNOTATIONS;
			break;
		case DECIMAL:
			typeKey = DOUBLE_ANNOTATIONS;
			break;
		case INTEGER:
			typeKey = LONG_ANNOTATIONS;
			break;
		}
		JSONObject typeObj = object.get(typeKey).isObject();
		if (typeObj != null) {
			if(typeObj.containsKey(keyToDelete)) {
				// copy all other keys to a new object
				JSONObject newTypeObj = new JSONObject();
				for(String key : typeObj.keySet()) {
					if(!key.equals(keyToDelete)) {
						newTypeObj.put(key, typeObj.get(key));
					}
				}
				object.put(typeKey, newTypeObj);
			}
		}
	}
	
	private String validateKey(String key) {
		String errorMsg = null;
		if(!DisplayUtils.validateAnnotationKey(key)) {
			errorMsg = DisplayConstants.ERROR_INVALID_ENTITY_NAME;
			String offending = DisplayUtils.getOffendingCharacterForAnnotationKey(key);
			errorMsg += offending == null ? "." : ": " + offending;
		}
		return errorMsg;
	}
}