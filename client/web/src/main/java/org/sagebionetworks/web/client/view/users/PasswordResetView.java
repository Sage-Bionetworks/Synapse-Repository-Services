package org.sagebionetworks.web.client.view.users;

import com.google.gwt.user.client.ui.IsWidget;

public interface PasswordResetView extends IsWidget{
	
	/**
	 * Set this view's presenter
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);
		
	public void showRequestForm();
	
	public void showResetForm();
	
	public void showMessage(String message);
	
	public void showPasswordResetSuccess();
	
	public void showRequestSentSuccess();
	
	public void showInfo(String infoMessage);
	
	public void showErrorMessage(String errorMessage);
	
	public void clear();
	
	public interface Presenter {
		
		public void requestPasswordReset(String emailAddress);
		
		public void resetPassword(String newPassword);
	}

}
