package org.sagebionetworks.web.client.widget.editpanels;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.ontology.EnumerationTerm;
import org.sagebionetworks.web.client.ontology.NcboOntologyTerm;

import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.form.Field;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.SimpleComboValue;

public class EditorUtils {

	public static void copyValuesIntoFormFields(List<FormField> formFields, FormPanel nodeFormPanel, Map<String, FormField> keyToFormFieldMap) {			
		for(Field<?> gxtField : nodeFormPanel.getFields()) {
			String key = gxtField.getFieldLabel();
			FormField formField = keyToFormFieldMap.get(key);
			if(formField != null && gxtField.isDirty()) {				
				Object value = gxtField.getValue();
				if(value != null) {
					if(value instanceof Date) {
						formField.setValue(DisplayConstants.DATE_FORMAT_SERVICES.format((Date)value));
					} else if(value instanceof SimpleComboValue) {
						@SuppressWarnings("unchecked")
						EnumerationTerm term = ((SimpleComboValue<EnumerationTerm>)value).getValue();
						formField.setValue(term.getValue());
					} else {				
						formField.setValue(gxtField.getValue().toString());
					}
				}
			}
		}
	}
	
//	public static void copyStoreValuesIntoFormFields(List<FormField> formFields, ListStore<EditableAnnotationModelData> store, Map<String, FormField> keyToFormFieldMap) {			
//		for(EditableAnnotationModelData model : store.getModels()) {
//			String key = model.getKey();
//			FormField formField = keyToFormFieldMap.get(key);
//			if(formField != null && model.isDirty()) {				
//				Object value = model.getValue();
//				if(value != null) {
//					if(value instanceof Date) {
//						formField.setValue(DisplayConstants.DATE_FORMAT_SERVICES.format((Date)value));
//					} else if(value instanceof SimpleComboValue) {
//						@SuppressWarnings("unchecked")
//						OntologyTerm term = ((SimpleComboValue<OntologyTerm>)value).getValue();
//						formField.setValue(term.getValue());
//					} else {										
//						formField.setValue((String) value);
//					}
//				}
//			}
//		}
//	}

	public static void addAnnotationsToStore(final List<FormField> formFields, final ListStore<EditableAnnotationModelData> store, Map<String, FormField> keyToFormFieldMap) {			
		for(FormField formField : formFields) {
			String key = formField.getKey();
			EditableAnnotationModelData model = new EditableAnnotationModelData();
			model.setKey(key);
			
			// set type
			switch(formField.getType()) {
			case STRING:
			case INTEGER:
			case DECIMAL:
			case BOOLEAN:			
				if(key.equals(DisplayUtils.NODE_DESCRIPTION_KEY) || key.matches("^" + DisplayUtils.LAYER_COLUMN_DESCRIPTION_KEY_PREFIX + ".*")) {
					model.setColumnEditType(ColumnEditType.TEXTAREA);
					model.setValue(formField.getValue());
				} else {
					if(formField.isOntologyBased()) {
						model.setColumnEditType(ColumnEditType.ONTOLOGY);
						NcboOntologyTerm term = formField.getNcboOntologyTerm();
						model.setValueDisplay(term.getPreferredName());
						model.setValue(term.serialize());		
					} else {
						// normal text
						model.setColumnEditType(ColumnEditType.TEXT);
						model.setValue(formField.getValue());
					}
				}
				break;
			case DATE:		
				model.setColumnEditType(ColumnEditType.DATE);
				model.setValue(DisplayConstants.DATE_FORMAT.parse(formField.getValue()));
				break;
			}

			// set type to combo for ontologies
			if(formField.isEnumBased()) {				
				model.setColumnEditType(ColumnEditType.ENUMERATION);
			}
						
			store.add(model);
			
			// save for lookup later
			keyToFormFieldMap.put(key, formField);
		}			
	}

	
}
