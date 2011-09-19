package org.sagebionetworks.web.client.view.users;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.widget.filter.QueryFilter;
import org.sagebionetworks.web.client.widget.footer.Footer;
import org.sagebionetworks.web.client.widget.header.Header;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.util.KeyNav;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.FieldSet;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.FlowLayout;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class RegisterAccountViewImpl extends Composite implements RegisterAccountView {

	public interface RegisterAccountViewImplUiBinder extends UiBinder<Widget, RegisterAccountViewImpl> {}
	
	@UiField
	SimplePanel header;
	@UiField
	SimplePanel footer;	
	@UiField
	SimplePanel registerAccountPanel;
	@UiField
	SpanElement contentHtml;

	private Presenter presenter;
	private FormPanel formPanel;
	private FormData formData;
	private IconsImageBundle iconsImageBundle;
	private Button registerButton;
	private Header headerWidget;
	private Footer footerWidget;
	private SageImageBundle sageImageBundle;

	@Inject
	public RegisterAccountViewImpl(RegisterAccountViewImplUiBinder binder, Header headerWidget, Footer footerWidget, IconsImageBundle iconsImageBundle, QueryFilter filter, SageImageBundle imageBundle, SageImageBundle sageImageBundle) {		
		initWidget(binder.createAndBindUi(this));

		this.iconsImageBundle = iconsImageBundle;
		this.headerWidget = headerWidget;
		this.footerWidget = footerWidget;
		this.sageImageBundle = sageImageBundle;
		
		// header setup
		header.clear();
		footer.clear();
		header.add(headerWidget.asWidget());
		footer.add(footerWidget.asWidget());		
	}


	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
		headerWidget.refresh();
	}

	@Override
	public void showDefault() {
		this.clear();
		formData = new FormData("-20");  
		createForm();
		registerAccountPanel.clear();
		registerAccountPanel.add(formPanel);
	}


	@Override
	public void showAccountCreated() {
		this.clear();		
		contentHtml.setInnerHTML(DisplayUtils.getIconHtml(iconsImageBundle.informationBalloon16()) + " Your Synapse account has been created. We have sent you an email with instructions on how to setup a password for your account. Follow the directions in the email, and then <a href=\"#LoginPlace:0\">login here</a>.");				
	}

	@Override
	public void showErrorMessage(String errorMessage) {
		MessageBox.info("Error", errorMessage, null);
	}


	@Override
	public void clear() {		
		if(registerAccountPanel != null) registerAccountPanel.clear();
		if(contentHtml != null) contentHtml.setInnerHTML("");	
	}

	@Override
	public void showAccountCreationFailed() {
		if(registerButton != null) {
			registerButton.enable();
			setRegisterButtonDefaultTextAndIcon();
		}
	}

	
	/*
	 * Private Methods
	 */
	 private void createForm() {  
		     formPanel = new FormPanel();  
		     formPanel.setFrame(true);  
		     formPanel.setHeaderVisible(false);
		     formPanel.setWidth(350);  
		     formPanel.setLayout(new FlowLayout());		     
		   
		     FieldSet fieldSet = new FieldSet();  
		     fieldSet.setHeading("User Information&nbsp;");  
		   
		     FormLayout layout = new FormLayout();  
		     layout.setLabelWidth(85);  
		     fieldSet.setLayout(layout);  
		     
		     final TextField<String> email = new TextField<String>();  
		     email.setFieldLabel("Email Address");
		     email.setAllowBlank(false);
		     fieldSet.add(email, formData);  

		     final TextField<String> firstName = new TextField<String>();  
		     firstName.setFieldLabel("First Name");  
		     firstName.setAllowBlank(false);  
		     fieldSet.add(firstName, formData);  
		   
		     final TextField<String> lastName = new TextField<String>();  
		     lastName.setFieldLabel("Last Name");
		     lastName.setAllowBlank(false);
		     fieldSet.add(lastName, formData);
		     
		     Label passwordLabel = new Label(DisplayUtils.getIconHtml(iconsImageBundle.lock16()) + " Password setup instructions will be sent via email.");
		     fieldSet.add(passwordLabel);
		   		   		   
		     formPanel.add(fieldSet);  
		     formPanel.setButtonAlign(HorizontalAlignment.CENTER);  
		     		     
		     registerButton = new Button(DisplayConstants.BUTTON_REGISTER, new SelectionListener<ButtonEvent>(){
					@Override
					public void componentSelected(ButtonEvent ce) {
						if(validateForm(email, firstName, lastName)) {
							DisplayUtils.changeButtonToSaving(registerButton, sageImageBundle);						
							presenter.registerUser(email.getValue(), firstName.getValue(), lastName.getValue());
						} else {
							showErrorMessage(DisplayConstants.ERROR_ALL_FIELDS_REQUIRED);
						}
					}
		     });
		     setRegisterButtonDefaultTextAndIcon();
		     formPanel.addButton(registerButton);
		     
			// Enter key submits form 
			new KeyNav<ComponentEvent>(formPanel) {
				@Override
				public void onEnter(ComponentEvent ce) {
					super.onEnter(ce);
					if(registerButton.isEnabled()) {
						registerButton.fireEvent(Events.Select);
					}
				}
			};

	 }

	private boolean validateForm(TextField<String> email, TextField<String> firstName, TextField<String> lastName) {
		if (email.getValue() != null && email.getValue().length() > 0
				&& firstName.getValue() != null && firstName.getValue().length() > 0
				&& lastName.getValue() != null && lastName.getValue().length() > 0) {
			return true;
		}
		return false;
	}

	private void setRegisterButtonDefaultTextAndIcon() {
		if(registerButton != null) {
			registerButton.setText(DisplayConstants.BUTTON_REGISTER);
			registerButton.setIcon(AbstractImagePrototype.create(iconsImageBundle.mailArrow16()));
		}
	}
	
}
