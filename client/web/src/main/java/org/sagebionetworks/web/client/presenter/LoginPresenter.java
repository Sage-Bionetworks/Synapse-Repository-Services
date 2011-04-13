package org.sagebionetworks.web.client.presenter;

import org.sagebionetworks.web.client.place.LoginPlace;
import org.sagebionetworks.web.client.security.user.UserData;
import org.sagebionetworks.web.client.view.LoginView;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

public class LoginPresenter extends AbstractActivity implements LoginView.Presenter {

	private LoginPlace loginPlace;
	private LoginView view;
	private EventBus bus;
	
	@Inject
	public LoginPresenter(LoginView view){
		this.view = view;
		this.view.setPresenter(this);
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		panel.setWidget(this.view.asWidget());
		this.bus = eventBus;
	}

	public void setPlace(LoginPlace place) {
		this.loginPlace = place;
		
	}

	@Override
	public void setNewUser(UserData newUser) {
		// Allow the user to proceed.
		bus.fireEvent( new PlaceChangeEvent(loginPlace.getForwardPlace()));
		
	}

}
