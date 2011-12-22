package org.sagebionetworks.web.client.view;

import java.util.ArrayList;

import org.sagebionetworks.web.client.SynapsePresenter;
import org.sagebionetworks.web.client.SynapseView;
import org.sagebionetworks.web.client.view.ProfileView.Presenter;
import com.google.gwt.user.client.ui.IsWidget;

public interface PublicProfileView extends IsWidget, SynapseView {

	/**
	 * Set this view's presenter
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);
	
	/**
	 * Renders the view for a given presenter
	 */
	public void render();
	
	/**
	 * Adds the user's information to the public profile
	 */
	public void updateWithUserInfo(String name, ArrayList<String> userInfo);
	
	/**
	 * Display an error message in the view
	 */
	public void showErrorMessage(String message);

	public interface Presenter extends SynapsePresenter {

		public void getUserInfo();

	}

}