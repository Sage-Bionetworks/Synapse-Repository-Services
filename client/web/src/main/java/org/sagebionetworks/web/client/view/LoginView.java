package org.sagebionetworks.web.client.view;

import org.sagebionetworks.web.client.security.user.UserData;

import com.google.gwt.user.client.ui.IsWidget;

public interface LoginView extends IsWidget{
	
	public interface Presenter {

		void setNewUser(UserData newUser);
		
	}

	void setPresenter(Presenter loginPresenter);

}
