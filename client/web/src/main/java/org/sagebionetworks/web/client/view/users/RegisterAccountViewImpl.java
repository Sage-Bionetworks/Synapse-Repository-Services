package org.sagebionetworks.web.client.view.users;

import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.widget.filter.QueryFilter;
import org.sagebionetworks.web.client.widget.footer.Footer;
import org.sagebionetworks.web.client.widget.header.Header;
import org.sagebionetworks.web.shared.users.UserRegistration;

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
import com.extjs.gxt.ui.client.widget.form.FormButtonBinding;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.FlowLayout;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
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
	
	@Inject
	public RegisterAccountViewImpl(RegisterAccountViewImplUiBinder binder, Header headerWidget, Footer footerWidget, IconsImageBundle iconsImageBundle, QueryFilter filter, SageImageBundle imageBundle) {		
		initWidget(binder.createAndBindUi(this));

		this.iconsImageBundle = iconsImageBundle;
		
		header.add(headerWidget.asWidget());
		footer.add(footerWidget.asWidget());	
		formData = new FormData("-20");  
		createForm();		
		registerAccountPanel.add(formPanel);
	}


	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}


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
		     
		     Label passwordLabel = new Label(DisplayUtils.getIconHtml(iconsImageBundle.lock16()) + " Password setup insturctions will be sent via email.");
		     fieldSet.add(passwordLabel);
		   		   		   
		     formPanel.add(fieldSet);  
		     formPanel.setButtonAlign(HorizontalAlignment.CENTER);  
		     		     
		     registerButton = new Button("Register", new SelectionListener<ButtonEvent>(){
					@Override
					public void componentSelected(ButtonEvent ce) {
						registerButton.disable();
						presenter.registerUser(new UserRegistration(email.getValue(), email.getValue(), firstName.getValue(), lastName.getValue(), firstName.getValue() + " " + lastName.getValue()));
					}});
		     formPanel.addButton(registerButton);
		     
 			 FormButtonBinding binding = new FormButtonBinding(formPanel);
			 binding.addButton(registerButton);

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


	@Override
	public void showAccountCreated() {
		registerAccountPanel.clear();		
		contentHtml.setInnerHTML(DisplayUtils.getIconHtml(iconsImageBundle.informationBalloon16()) + " Your Synapse account has been created. We have sent you an email with instructions on how to setup a password for your account. Follow the directions in the email, and then <a href=\"#LoginPlace:0\">login here</a>.");				
	}

	@Override
	public void showErrorMessage(String errorMessage) {
		MessageBox.info("Error", errorMessage, null);
	}


	@Override
	public void clear() {		
		registerAccountPanel.clear();
		contentHtml.setInnerHTML("");
		registerButton.enable();
		registerAccountPanel.add(formPanel);
	}
}
