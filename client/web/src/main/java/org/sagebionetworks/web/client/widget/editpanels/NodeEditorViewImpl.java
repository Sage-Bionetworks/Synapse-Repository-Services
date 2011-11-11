package org.sagebionetworks.web.client.widget.editpanels;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.ontology.Enumeration;
import org.sagebionetworks.web.client.ontology.EnumerationTerm;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.Html;
import com.extjs.gxt.ui.client.widget.Info;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.form.DateField;
import com.extjs.gxt.ui.client.widget.form.FormButtonBinding;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.SimpleComboBox;
import com.extjs.gxt.ui.client.widget.form.TextArea;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.MarginData;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class NodeEditorViewImpl extends LayoutContainer implements NodeEditorView {

	private Presenter presenter;
	private SageImageBundle sageImageBundle;
	private IconsImageBundle iconsImageBundle;
	private Button formButton;
	private String formButtonOriginalString;
	private String formButtonResultString;
		
	@Inject
	public NodeEditorViewImpl(SageImageBundle sageImageBundle, IconsImageBundle iconsImageBundle) {
		this.sageImageBundle = sageImageBundle;
		this.iconsImageBundle = iconsImageBundle;
		this.setScrollMode(Scroll.AUTOY);
	}
	
	@Override
	protected void onRender(Element parent, int pos) {
		super.onRender(parent, pos);
	}
	
	@Override
	public Widget asWidget() {
		return this;
	}	

	@Override 
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}
	
	@Override
	public void generateCreateForm(List<FormField> formFields,
			String typeDisplay, String topText, List<String> requiredFields,
			Map<String, Enumeration> keyToOntologyTerms) {
		buildNodeForm(formFields, typeDisplay, topText, requiredFields, keyToOntologyTerms, false, null);
	}

	@Override
	public void generateEditForm(List<FormField> formFields,
			String typeDisplay, String topText, List<String> requiredFields,
			Map<String, Enumeration> keyToOntologyTerms,
			JSONObject editorValues) {
		buildNodeForm(formFields, typeDisplay, topText, requiredFields, keyToOntologyTerms, true, editorValues);
	}
		
	@Override
	public void showLoading() {
		Html html = new Html(DisplayUtils.getIconHtml(sageImageBundle.loading16()) + " Loading...");
		this.add(html);
	}
	
	@Override
	public void showErrorMessage(String message) {
		this.clear();	
		Html html = new Html(DisplayUtils.getIconHtml(iconsImageBundle.error16()) + " " + message);
		this.add(html);		
	}

	@Override
	public void clear() {
		this.removeAll();
	}

	
	/*
	 * Private Methods 
	 */
	private void buildNodeForm(final List<FormField> formFields,
			String typeDisplay, String topText, List<String> requiredFields,
			Map<String, Enumeration> keyToOntologyTerms, boolean isEditor,
			JSONObject editorValues) {
		// remove any old forms, this is a singleton afterall
		this.removeAll();

		// add description text
		if(topText != null) {
			LayoutContainer padding = new LayoutContainer();
			padding.setStyleAttribute("background-color", "#fff");
			Html topHtml = new Html(topText);
			topHtml.setStyleAttribute("background-color", "#fff");
			padding.add(topHtml, new MarginData(5, 5, 5, 5));
			this.add(padding);
		}
		
		final FormPanel nodeFormPanel = new FormPanel();	
		FormData formData = new FormData("-20");
		
		nodeFormPanel.setHeaderVisible(false);			
		nodeFormPanel.setFrame(false);
		nodeFormPanel.setAutoWidth(true);
		nodeFormPanel.setLabelWidth(100);

		if(isEditor) addValuesToFormFields(formFields, editorValues);
		
		final Map<String, FormField> keyToFormFieldMap = new HashMap<String, FormField>();
		for(FormField formField : formFields) {
			// only add required fields some fields
			if(!requiredFields.contains(formField.getKey()))
				continue;
			String key = formField.getKey();
			switch(formField.getType()) {
			case STRING:
			case INTEGER:
			case DECIMAL:							
				if(key.equals(DisplayUtils.NODE_DESCRIPTION_KEY)) {
					TextArea description = new TextArea();					
					description.setPreventScrollbars(false);
					description.setHeight(82);
					description.setFieldLabel(key);  
					if(isEditor) description.setValue(formField.getValue());
					nodeFormPanel.add(description, formData);				
				} else if(keyToOntologyTerms.containsKey(key)) {
					SimpleComboBox<EnumerationTerm> combo = new SimpleComboBox<EnumerationTerm>();					
					combo.setFieldLabel(key);  
					combo.setForceSelection(true);					 
					combo.setTriggerAction(TriggerAction.ALL);
					for(EnumerationTerm term : keyToOntologyTerms.get(key).getTerms()) {
						combo.add(term);
					}					
					if(isEditor) combo.setSimpleValue(formField.getOntologyValue());
					nodeFormPanel.add(combo, formData);  					
				} else {				
					TextField<String> stringField = new TextField<String>();
					stringField.setFieldLabel(key);
					stringField.setAllowBlank(false);
					if(isEditor) stringField.setValue(formField.getValue());
					nodeFormPanel.add(stringField, formData);
				}
				break;
			case DATE:
				DateField dateField = new DateField();
				dateField.setFieldLabel(key);				
				if(isEditor && formField.getValue() != null) {					
					try {
						Date date = DisplayConstants.DATE_FORMAT_SERVICES.parse(formField.getValue());
						dateField.setValue(date);
					} catch(IllegalArgumentException ex) {
						// do nothing, field is improperly formatted
					}					
				}
				nodeFormPanel.add(dateField, formData);				
				break;
			}
			
			// save for lookup later
			keyToFormFieldMap.put(key, formField);
		}
			
		final FormButtonBinding binding = new FormButtonBinding(nodeFormPanel);		
		formButtonOriginalString = isEditor ? "Save" : "Create " + typeDisplay;
		formButtonResultString = isEditor ? "Changes Saved" : typeDisplay + " Created";
		formButton = new Button(formButtonOriginalString);
		formButton.addSelectionListener(new SelectionListener<ButtonEvent>() {			
			@Override
			public void componentSelected(ButtonEvent ce) {
				// copy all values back into their FormField and pass to presenter				
				EditorUtils.copyValuesIntoFormFields(formFields, nodeFormPanel, keyToFormFieldMap);
				binding.stopMonitoring();
				formButton.disable();
				formButton.setText("Saving...");
				formButton.setIcon(AbstractImagePrototype.create(sageImageBundle.loading16()));
				presenter.persist(formFields); // persist				
			}
		});
		nodeFormPanel.addButton(formButton);
		
		// close button
		Button closeButton = new Button("Close");
		closeButton.addSelectionListener(new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				presenter.closeButtonSelected();
			}
		});
		nodeFormPanel.addButton(closeButton);
		
		nodeFormPanel.setButtonAlign(HorizontalAlignment.CENTER);
		// only require all fields for creation windows
		if(!isEditor) 
			binding.addButton(formButton);		
		
		nodeFormPanel.layout();
		this.add(nodeFormPanel);
		this.layout();
	}

	private void addValuesToFormFields(List<FormField> formFields,
			JSONObject editorValues) {
		for(FormField formField : formFields) {
			String key = formField.getKey();
			if(editorValues.containsKey(key)) {
				switch (formField.getType()) {
				case STRING:
					String strString = "";
					JSONString jsonStr = editorValues.get(key).isString();
					if(jsonStr != null) {
						strString = jsonStr.stringValue();
					}
					formField.setValue(strString);
					break;
				case DATE:
					String dateString = "";
					JSONString dateUTC = editorValues.get(key).isString();
					if(dateUTC != null) {
						Date date = DisplayUtils.convertStringToDate(dateUTC.stringValue());
						if(date != null) {							
							dateString = DisplayConstants.DATE_FORMAT_SERVICES.format(date);
						}
					}					
					formField.setValue(dateString);					
					break;
				case INTEGER:
				case DECIMAL:
					String decimalString = "";
					JSONNumber decimalDouble = editorValues.get(key).isNumber();
					if(decimalDouble != null) {
						decimalString = ((Double)decimalDouble.doubleValue()).toString();
					}					
					formField.setValue(decimalString);					
					break;
				case BOOLEAN:
					String boolString = "";
					JSONBoolean boolValue = editorValues.get(key).isBoolean();
					if(boolValue != null) {
						boolString = ((Boolean)boolValue.booleanValue()).toString();
					}
					formField.setValue(boolString);
					break;
				case LIST_STRING:
					// TODO : finish this for lists
					break;
				case LIST_INTEGER:
					break;
				case LIST_DECIMAL:
					break;
				}
			}
		}
	}

	@Override
	public void showPersistSuccess() {
		if(formButton != null) {
			formButton.enable();
			formButton.setIcon(AbstractImagePrototype.create(iconsImageBundle.checkGreen16()));			
			if(formButtonResultString == null) {
				formButtonResultString = "Saved";				
			}
			formButton.setText(formButtonResultString);				
		}
		Info.display("Message", DisplayUtils.getIconHtml(iconsImageBundle.checkGreen16()) + " " + formButtonResultString);
	}

	@Override
	public void showPersistFail(String message) {
		if(formButton != null) {
			formButton.enable();
			formButton.setIcon(null);
			if(formButtonOriginalString == null) {
				formButtonOriginalString = "Save";				
			}
			formButton.setText(formButtonOriginalString);				
		}		
		if(message != null) {
			MessageBox.info("Message", message, null);
		}
	}

}
