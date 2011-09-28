package org.sagebionetworks.web.client;

public interface SynapseView {

	/**
	 * Shows a loading view
	 */
	public void showLoading();
	
	/**
	 * Shows user info
	 * @param title
	 * @param message
	 */
	public void showInfo(String title, String message);
	
	/**
	 * Show error message
	 * @param string
	 */
	public void showErrorMessage(String message);
	
	/**
	 * Clears out old elements
	 */
	public void clear();
	
}
