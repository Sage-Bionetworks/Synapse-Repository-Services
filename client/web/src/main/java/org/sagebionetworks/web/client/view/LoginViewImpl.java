package org.sagebionetworks.web.client.view;

import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.place.users.PasswordReset;
import org.sagebionetworks.web.client.place.users.RegisterAccount;
import org.sagebionetworks.web.client.widget.login.LoginWidget;
import org.sagebionetworks.web.client.widget.login.UserListener;
import org.sagebionetworks.web.shared.users.UserData;

import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Hyperlink;
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

	public interface Binder extends UiBinder<Widget, LoginViewImpl> {}
	
	@Inject
	public LoginViewImpl(Binder uiBinder, IconsImageBundle icons, LoginWidget widget){
		initWidget(uiBinder.createAndBindUi(this));
		// Add the widget to the panel
		loginWidgetPanel.add(widget.asWidget());
		widget.addUserListener(new UserListener() {
			
			@Override
			public void userChanged(UserData newUser) {
				presenter.setNewUser(newUser);
			}
		});
				
		Button forgotPasswordButton = new Button("Forgot Password?", AbstractImagePrototype.create(icons.help16()), new SelectionListener<ButtonEvent>() {			
			@Override
			public void componentSelected(ButtonEvent ce) {
				presenter.goTo(new PasswordReset("0"));								
			}
		});
		passwordReset.add(forgotPasswordButton);
		

		Button registerButton = new Button("Register for a Synapse Account", AbstractImagePrototype.create(icons.userBusiness16()), new SelectionListener<ButtonEvent>() {			
			@Override
			public void componentSelected(ButtonEvent ce) {
				presenter.goTo(new RegisterAccount("0"));
			}
		});
		register.add(registerButton);
		
	}

	@Override
	public void setPresenter(Presenter loginPresenter) {
		this.presenter = loginPresenter;
	}



}
