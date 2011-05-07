package org.sagebionetworks.web.client.widget.login;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.web.client.UserAccountServiceAsync;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.security.AuthenticationException;
import org.sagebionetworks.web.shared.users.UserData;

import com.google.gwt.user.client.rpc.AsyncCallback;
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
		controller.loginUser(username, password, new AsyncCallback<UserData>() {
			@Override
			public void onSuccess(UserData result) {
				fireUserChage(result);
			}

			@Override
			public void onFailure(Throwable caught) {
				view.showAuthenticationFailed();
			}
		});
	}

	// needed?
	private void fireUserChage(UserData user) {
		for(UserListener listener: listeners){
			listener.userChanged(user);
		}
	}
	
	
	
	
}
