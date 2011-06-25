package org.sagebionetworks.web.client.widget.login;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.IconsImageBundle;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.util.KeyNav;
import com.extjs.gxt.ui.client.widget.Html;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.VerticalPanel;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.FieldSet;
import com.extjs.gxt.ui.client.widget.form.FormButtonBinding;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.FormPanel.Method;
import com.extjs.gxt.ui.client.widget.form.HiddenField;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;
import com.extjs.gxt.ui.client.widget.layout.MarginData;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class LoginWidgetViewImpl extends LayoutContainer implements
		LoginWidgetView {

	private Presenter presenter;
	private VerticalPanel vp;
	private FormData formData;
	private Label messageLabel;
	private IconsImageBundle iconsImageBundle;
	private TextField<String> firstName = new TextField<String>();
	private TextField<String> password = new TextField<String>();
	
	@Inject
	public LoginWidgetViewImpl(IconsImageBundle iconsImageBundle) {
		this.iconsImageBundle = iconsImageBundle;
		messageLabel = new Label();
	}

	@Override
	protected void onRender(Element parent, int pos) {
		super.onRender(parent, pos);
		formData = new FormData("-20");
		vp = new VerticalPanel();
		vp.setSpacing(10);
		
		// federated login button
		StringBuilder sb = new StringBuilder();		
		sb.append("<form accept-charset=\"UTF-8\" action=\""+ presenter.getOpenIdActionUrl() +"\" class=\"aui\" id=\"gapp-openid-form\" method=\"post\" name=\"gapp-openid-form\">");
		sb.append("    <input name=\"OPEN_ID_PROVIDER\" type=\"hidden\" value=\""+ DisplayConstants.OPEN_ID_PROVIDER_SAGE_VALUE +"\"/>");
		sb.append("    <input name=\"RETURN_TO_URL\" type=\"hidden\" value=\""+ presenter.getOpenIdReturnUrl() +"\"/>");
		sb.append("    <button id=\"login-via-gapp-google\" type=\"submit\"><img alt=\""+ DisplayConstants.OPEN_ID_SAGE_LOGIN_BUTTON_TEXT +"\" src=\"http://www.google.com/favicon.ico\"/>&nbsp; "+ DisplayConstants.OPEN_ID_SAGE_LOGIN_BUTTON_TEXT +"</button>");
		sb.append("</form>");		
		sb.append("<p>&nbsp;</p>");
		Html sageSSOLogin = new Html(sb.toString());
		vp.add(sageSSOLogin);
		
		createForm();
		add(vp);
	}

	private void createForm() {
		final FormPanel formPanel = new FormPanel();
		formPanel.setHeaderVisible(false);
		formPanel.setFrame(true);
		formPanel.setWidth(350);
		formPanel.setLabelWidth(85);

		
//		FieldSet fieldSetFederated = new FieldSet();  
//		fieldSetFederated.setHeading("Login with an External Account");  
//		fieldSetFederated.setCheckboxToggle(false);
//		fieldSetFederated.setCollapsible(false);
//		fieldSetFederated.setLayout(layout);  
//		formPanel.setAction(DisplayConstants.OPEN_ID_ACTION_ENDPOINT);
//		formPanel.setMethod(Method.POST);
//		
//		HiddenField<String> openIdProvider = new HiddenField<String>();
//		openIdProvider.setFieldLabel(OPEN_ID_PROVIDER_FIELD_NAME);
//		openIdProvider.setValue(OPEN_ID_PROVIDER_SAGE_VALUE);
//		
//		Button sagebaseLoginButton = new Button("Login with a Sagebase.org Account");
//		sagebaseLoginButton.setIcon(AbstractImagePrototype.create(iconsImageBundle.google16()));
//		sagebaseLoginButton.addSelectionListener(new SelectionListener<ButtonEvent>() {
//			@Override
//			public void componentSelected(ButtonEvent ce) {
//				formPanel.submit();
//			}
//		});
//		formPanel.add(sagebaseLoginButton, new MarginData(0, 0, 13, 0));

		
		// synapse login
		FieldSet fieldSet = new FieldSet();  
		fieldSet.setHeading("Login with a Synapse Account");  
		fieldSet.setCheckboxToggle(false);  		       
		FormLayout layout = new FormLayout();  
		layout.setLabelWidth(85);  		
		fieldSet.setLayout(layout);  
		fieldSet.setCollapsible(true);
		fieldSet.collapse();
		
		firstName.setFieldLabel("Email Address");
		firstName.setAllowBlank(false);
		firstName.getFocusSupport().setPreviousId(formPanel.getButtonBar().getId());
		fieldSet.add(firstName, formData);

		password.setFieldLabel("Password");
		password.setAllowBlank(false);
		password.setPassword(true);
		fieldSet.add(password, formData);

		fieldSet.add(messageLabel);
		
		//formPanel.add(fieldSet);
		
		final Button loginButton = new Button("Login", new SelectionListener<ButtonEvent>(){			
			@Override
			public void componentSelected(ButtonEvent ce) {
				messageLabel.setText(""); 
				presenter.setUsernameAndPassword(firstName.getValue(), password.getValue());
			}
		});

		fieldSet.add(loginButton);
		formPanel.add(fieldSet);
		//formPanel.addButton(loginButton);
		//formPanel.setButtonAlign(HorizontalAlignment.CENTER);
		
		formPanel.add(fieldSet);
		
		FormButtonBinding binding = new FormButtonBinding(formPanel);
		binding.addButton(loginButton);

		// Enter key submits login 
		new KeyNav<ComponentEvent>(formPanel) {
			@Override
			public void onEnter(ComponentEvent ce) {
				super.onEnter(ce);
				if(loginButton.isEnabled())
					loginButton.fireEvent(Events.Select);
			}
		};
		
		vp.add(formPanel);
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
	public void showError(String message) {
		com.google.gwt.user.client.Window.alert(message);
	}

	@Override
	public void showAuthenticationFailed() {
		messageLabel.setStyleAttribute("color", "red");
		messageLabel.setText(AbstractImagePrototype.create(iconsImageBundle.warning16()).getHTML() + " Invalid username or password.");
		clear();
	}

	@Override
	public void clear() {		
		password.clear();
	}

}
