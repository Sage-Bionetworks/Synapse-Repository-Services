package org.sagebionetworks.web.client.widget.login;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.security.AuthenticationException;
import org.sagebionetworks.web.client.security.user.UserData;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class LoginWidget implements LoginWidgetView.Presenter {

	private LoginWidgetView view;
	private AuthenticationController controller;
	
	private List<UserListener> listeners = new ArrayList<UserListener>();
	
	@Inject
	public LoginWidget(LoginWidgetView view, AuthenticationController controller) {
		this.view = view;
		view.setPresenter(this);
		this.controller = controller;
	}

	public Widget asWidget() {
		return view.asWidget();
	}
	
	public void addUserListener(UserListener listener){
		listeners.add(listener);
	}

	@Override
	public void setUsernameAndPassword(String username, String password) {
		try{
			UserData user = controller.loginUser(username, password);
			fireUserChage(user);
		}catch(AuthenticationException e){
			view.showError(e.getMessage());
		}
	}

	private void fireUserChage(UserData user) {
		for(UserListener listener: listeners){
			listener.userChanged(user);
		}
	}
	
	
	
	
}
