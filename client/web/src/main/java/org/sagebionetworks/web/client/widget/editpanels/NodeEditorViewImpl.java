package org.sagebionetworks.web.client.widget.editpanels;

import java.util.Date;
import java.util.List;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.SageImageBundle;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.widget.Html;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.DateField;
import com.extjs.gxt.ui.client.widget.form.FormButtonBinding;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.TextArea;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.MarginData;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class NodeEditorViewImpl extends LayoutContainer implements NodeEditorView {

	private Presenter presenter;
	private SageImageBundle sageImageBundle;
	
	@Inject
	public NodeEditorViewImpl(SageImageBundle sageImageBundle) {
		this.sageImageBundle = sageImageBundle;
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
			String typeDisplay, String topText, List<String> ignoreFields) {
		buildNodeForm(formFields, typeDisplay, topText, ignoreFields, false, null);
	}

	@Override
	public void generateEditForm(List<FormField> formFields,
			String typeDisplay, String topText, List<String> ignoreFields,
			JSONObject editorValues) {
		buildNodeForm(formFields, typeDisplay, topText, ignoreFields, true, editorValues);		
	}	
		
	@Override
	public void showLoading() {
		Html html = new Html(DisplayUtils.getIconHtml(sageImageBundle.loading16()) + " Loading...");
		this.add(html);
	}
	
	@Override
	public void showErrorMessage(String message) {
		MessageBox.info("Error", message, null);
	}

	
	/*
	 * Private Methods 
	 */
	private void buildNodeForm(List<FormField> formFields, String typeDisplay, String topText, List<String> ignoreFields, boolean isEditor, JSONObject editorValues) {
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
		
		FormPanel nodeFormPanel = new FormPanel();	
		FormData formData = new FormData("-20");

		nodeFormPanel.setHeaderVisible(false);			
		nodeFormPanel.setFrame(false);
		nodeFormPanel.setAutoWidth(true);

		if(isEditor) addValuesToFormFields(formFields, editorValues);
		
		for(FormField formField : formFields) {
			// skip some fields
			if(ignoreFields.contains(formField.getKey()))
				continue;
			
			switch(formField.getType()) {
			case STRING:
			case INTEGER:
			case DECIMAL:							
				if(formField.getKey().equals(DisplayConstants.NODE_DESCRIPTION_KEY)) {
					TextArea description = new TextArea();					
					description.setPreventScrollbars(false);
					description.setHeight(82);
					description.setFieldLabel(formField.getKey());  
					if(isEditor) description.setValue(formField.getValue());
					nodeFormPanel.add(description, formData);				
				} else {				
					TextField<String> stringField = new TextField<String>();
					stringField.setFieldLabel(formField.getKey());
					stringField.setAllowBlank(false);
					if(isEditor) stringField.setValue(formField.getValue());
					nodeFormPanel.add(stringField, formData);
				}
				break;
			case DATE:
				DateField dateField = new DateField();
				dateField.setFieldLabel(formField.getKey());
				if(isEditor) dateField.setValue(DisplayConstants.DATE_FORMAT.parse(formField.getValue()));
				nodeFormPanel.add(dateField, formData);				
				break;
			}
		}
			
		String saveButtonString;
		saveButtonString = isEditor ? "Save" : "Create " + typeDisplay; 
		Button saveButton = new Button(saveButtonString);
		nodeFormPanel.addButton(saveButton);
		nodeFormPanel.setButtonAlign(HorizontalAlignment.CENTER);

		FormButtonBinding binding = new FormButtonBinding(nodeFormPanel);
		binding.addButton(saveButton);
			
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
					JSONNumber dateDouble = editorValues.get(key).isNumber();
					if(dateDouble != null) {
						Date date = new Date(((Double)dateDouble.doubleValue()).longValue());
						if(date != null) {							
							dateString = DisplayConstants.DATE_FORMAT.format(date);
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

}
