package org.sagebionetworks.web.client.view;

import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.place.users.PasswordReset;
import org.sagebionetworks.web.client.place.users.RegisterAccount;
import org.sagebionetworks.web.client.widget.login.LoginWidget;
import org.sagebionetworks.web.client.widget.login.UserListener;
import org.sagebionetworks.web.shared.users.UserData;

import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class LoginViewImpl extends Composite implements LoginView {
	
	@UiField
	SimplePanel loginWidgetPanel;
	@UiField
	SimplePanel passwordReset;
	@UiField 
	SimplePanel register;
	

	private Presenter presenter;
	private LoginWidget loginWidget;

	public interface Binder extends UiBinder<Widget, LoginViewImpl> {}
	
	@Inject
	public LoginViewImpl(Binder uiBinder, IconsImageBundle icons, LoginWidget loginWidget){
		initWidget(uiBinder.createAndBindUi(this));
		this.loginWidget = loginWidget;
		// Add the widget to the panel
		loginWidgetPanel.add(loginWidget.asWidget());
		loginWidget.addUserListener(new UserListener() {
			
			@Override
			public void userChanged(UserData newUser) {
				presenter.setNewUser(newUser);
			}
		});
				
		Button forgotPasswordButton = new Button("Setup R Client/API Password", AbstractImagePrototype.create(icons.help16()), new SelectionListener<ButtonEvent>() {			
			@Override
			public void componentSelected(ButtonEvent ce) {
				presenter.goTo(new PasswordReset("0"));								
			}
		});
		passwordReset.add(forgotPasswordButton);
		

		Button registerButton = new Button("Register for a Synapse", AbstractImagePrototype.create(icons.userBusiness16()), new SelectionListener<ButtonEvent>() {			
			@Override
			public void componentSelected(ButtonEvent ce) {
				presenter.goTo(new RegisterAccount("0"));
			}
		});
		registerButton.disable();
		register.add(registerButton);
		
	}

	@Override
	public void setPresenter(Presenter loginPresenter) {
		this.presenter = loginPresenter;
	}

	@Override
	public void clear() {
		loginWidget.clear();
	}
	
	@Override
	public void showErrorMessage(String message) {
		MessageBox.info("Message", message, null);
	}


}
