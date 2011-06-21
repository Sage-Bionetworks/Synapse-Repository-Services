package org.sagebionetworks.web.client.view;

import org.sagebionetworks.web.shared.users.UserData;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.IsWidget;

public interface LoginView extends IsWidget{
	
	void setPresenter(Presenter loginPresenter);	
	
	void showErrorMessage(String message);
	
	void clear();
	
	void showLoggingInLoader();
	
	void hideLoggingInLoader();
	
	void showLogout(boolean isSsoLogout);
	
	void showLogin();

	public interface Presenter {
		void goTo(Place place);
		
		void setNewUser(UserData newUser);		
	}
}
