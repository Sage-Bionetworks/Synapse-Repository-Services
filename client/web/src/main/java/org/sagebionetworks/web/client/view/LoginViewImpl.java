package org.sagebionetworks.web.client.view;

import org.sagebionetworks.web.client.security.user.UserData;
import org.sagebionetworks.web.client.widget.login.LoginWidget;
import org.sagebionetworks.web.client.widget.login.UserListener;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class LoginViewImpl extends Composite implements LoginView {
	
	@UiField
	SimplePanel loginWidgetPanel;
	private Presenter presenter;

	public interface Binder extends UiBinder<Widget, LoginViewImpl> {}
	
	@Inject
	public LoginViewImpl(Binder uiBinder, LoginWidget widget){
		initWidget(uiBinder.createAndBindUi(this));
		// Add the widget to the panel
		loginWidgetPanel.add(widget.asWidget());
		widget.addUserListener(new UserListener() {
			
			@Override
			public void userChanged(UserData newUser) {
				presenter.setNewUser(newUser);
			}
		});
	}

	@Override
	public void setPresenter(Presenter loginPresenter) {
		this.presenter = loginPresenter;
	}



}
