package org.sagebionetworks.web.client.widget.editpanels;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.ontology.OntologyTerm;

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
						OntologyTerm term = ((SimpleComboValue<OntologyTerm>)value).getValue();
						formField.setValue(term.getValue());
					} else {				
						formField.setValue(gxtField.getValue().toString());
					}
				}
			}
		}
	}

}
