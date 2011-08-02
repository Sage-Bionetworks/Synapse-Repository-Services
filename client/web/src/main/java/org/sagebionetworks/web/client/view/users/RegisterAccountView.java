package org.sagebionetworks.web.client.view.users;

import org.sagebionetworks.web.shared.users.UserRegistration;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.IsWidget;

public interface RegisterAccountView extends IsWidget {
	
	/**
	 * Set this view's presenter
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);
		
	public void showAccountCreated();
	
	public void showErrorMessage(String message);
	
	public void clear();
	
	public interface Presenter {	
		void goTo(Place place);
		
		void registerUser(String email, String firstName, String lastName);
	}

}
