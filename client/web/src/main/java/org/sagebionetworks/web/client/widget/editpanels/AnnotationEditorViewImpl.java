package org.sagebionetworks.web.client.widget.editpanels;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.ontology.OntologyTerm;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.Html;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.DateField;
import com.extjs.gxt.ui.client.widget.form.FormButtonBinding;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.SimpleComboBox;
import com.extjs.gxt.ui.client.widget.form.TextArea;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.MarginData;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
 
public class AnnotationEditorViewImpl extends LayoutContainer implements AnnotationEditorView {
 
    private Presenter presenter;
	private SageImageBundle sageImageBundle;
	private IconsImageBundle iconsImageBundle;
	private String formButtonOriginalString;
	private String formButtonResultString;
	private Button formButton;

 
    @Inject
    public AnnotationEditorViewImpl(SageImageBundle sageImageBundle, IconsImageBundle iconsImageBundle) {
		this.sageImageBundle = sageImageBundle;
		this.iconsImageBundle = iconsImageBundle;
		this.setScrollMode(Scroll.AUTOY);
    }
 
    @Override
    protected void onRender(Element parent, int pos) {
        super.onRender(parent, pos);
    }

	@Override
	public void generateAnnotationForm(final List<FormField> formFields, String displayString, String topText) {
		// remove any old forms, this is a singleton afterall
		this.clear();
		

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
		nodeFormPanel.setLabelWidth(120);
		
		final Map<String, FormField> keyToFormFieldMap = new HashMap<String, FormField>();
		for(FormField formField : formFields) {
			String key = formField.getKey();
			switch(formField.getType()) {
			case STRING:
			case INTEGER:
			case DECIMAL:							
				if(key.equals(DisplayConstants.NODE_DESCRIPTION_KEY)) {
					TextArea description = new TextArea();					
					description.setPreventScrollbars(false);
					description.setHeight(82);
					description.setFieldLabel(key);  
					description.setValue(formField.getValue());
					nodeFormPanel.add(description, formData);				
				} else {				
					TextField<String> stringField = new TextField<String>();
					stringField.setFieldLabel(key);
					stringField.setAllowBlank(false);
					stringField.setValue(formField.getValue());
					nodeFormPanel.add(stringField, formData);
				}
				break;
			case DATE:
				DateField dateField = new DateField();
				dateField.setFieldLabel(key);
				dateField.setValue(DisplayConstants.DATE_FORMAT_SERVICES.parse(formField.getValue()));
				nodeFormPanel.add(dateField, formData);				
				break;
			}
			
			// save for lookup later
			keyToFormFieldMap.put(key, formField);
		}
			
		final FormButtonBinding binding = new FormButtonBinding(nodeFormPanel);		
		formButtonOriginalString = "Save";
		formButtonResultString = "Changes Saved";
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
		nodeFormPanel.setButtonAlign(HorizontalAlignment.CENTER);
		binding.addButton(formButton);		
		
		nodeFormPanel.layout();
		this.add(nodeFormPanel);
		this.layout();		
	}

	@Override
	public void showErrorMessage(String message) {
		MessageBox.info("Error", message, null);
	}

    @Override
    public Widget asWidget() {
        return this;
    }

    
	@Override
	public void showLoading() {
		Html html = new Html(DisplayUtils.getIconHtml(sageImageBundle.loading16()) + " Loading...");
		this.add(html);
	}
    
	@Override
	public void clear() {
		this.removeAll();
		this.formButtonOriginalString = null;
		this.formButtonResultString = null;
		this.formButton = null;

	}
 
    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
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
		
	}

	@Override
	public void showPersistFail() {
		if(formButton != null) {
			formButton.enable();
			formButton.setIcon(null);
			if(formButtonOriginalString == null) {
				formButtonOriginalString = "Save";				
			}
			formButton.setText(formButtonOriginalString);				
		}		
		MessageBox.info("Message", "An error occuring attempting to save. Please try again.", null);
	}
 
}