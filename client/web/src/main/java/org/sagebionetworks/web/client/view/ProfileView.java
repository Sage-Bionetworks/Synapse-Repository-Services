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
	 * Show the user that their email has been sent
	 */
	public void showRequestPasswordEmailSent();
	
	/**
	 * Alerts the view that the password request email failed to send.
	 */
	public void requestPasswordEmailFailed();
	
	/**
	 * Show the user that the user's information has been updated.
	 */
	public void showUserUpdateSuccess();
	
	/**
	 * Alerts the view that updating the user's information failed.
	 */
	public void userUpdateFailed();
	
	/**
	 * The view pops-up an error dialog.
	 * @param message
	 */
	public void showErrorMessage(String message);
	
	public void showInfo(String infoMessage);

	public void refreshHeader();
	
	public interface Presenter {

		PlaceChanger getPlaceChanger();

		void resetPassword(String existingPassword, String newPassword);

		void createSynapsePassword();
		
		void updateProfile(String firstName, String lastName);

	}


}
