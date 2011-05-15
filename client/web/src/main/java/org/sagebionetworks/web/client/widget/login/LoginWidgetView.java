package org.sagebionetworks.web.client.widget.login;

import com.google.gwt.user.client.ui.IsWidget;

public interface LoginWidgetView extends IsWidget {

	/**
	 * Set the presenter.
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);
	
	
	/**
	 * Presenter interface
	 */
	public interface Presenter {
		
		public void setUsernameAndPassword(String username, String password);
	}

	public void showAuthenticationFailed();
	
	public void showError(String message);
	
	public void clear();
}
