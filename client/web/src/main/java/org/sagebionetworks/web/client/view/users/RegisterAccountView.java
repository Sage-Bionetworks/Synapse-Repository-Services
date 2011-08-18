package org.sagebionetworks.web.client.view.users;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.IsWidget;

public interface RegisterAccountView extends IsWidget {
	
	/**
	 * Set this view's presenter
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);

	/**
	 * The default register view
	 */
	public void showDefault();
	
	/**
	 * The account was created view
	 */
	public void showAccountCreated();
	
	public void showErrorMessage(String message);
	
	public void clear();
	
	public interface Presenter {	
		void goTo(Place place);
		
		void registerUser(String email, String firstName, String lastName);
	}

	public void showAccountCreationFailed();


}
