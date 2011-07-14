package org.sagebionetworks.web.client.view;

import org.sagebionetworks.web.client.PlaceChanger;

import com.google.gwt.user.client.ui.IsWidget;

public interface ProfileView extends IsWidget {
	
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
	 * Shows the user that their password change succeeded
	 */
	public void showPasswordChangeSuccess();
	
	/**
	 * Alerts the view that the password change failed
	 */
	public void passwordChangeFailed();	
	
	/**
	 * Show the user that thier email has been sent
	 */
	public void showRequestPasswordEmailSent();
	
	/**
	 * Alerts the view that the password request email failed to send.
	 */
	public void requestPasswordEmailFailed();
	
	
	/**
	 * The view pops-up an error dialog.
	 * @param message
	 */
	public void showErrorMessage(String message);
	
	void showInfo(String infoMessage);
	
	public interface Presenter {

		PlaceChanger getPlaceChanger();

		void resetPassword(String existingPassword, String newPassword);

		void createSynapsePassword();
	}

}
