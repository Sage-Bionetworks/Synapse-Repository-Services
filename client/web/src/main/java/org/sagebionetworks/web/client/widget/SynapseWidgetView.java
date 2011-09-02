package org.sagebionetworks.web.client.widget;

public interface SynapseWidgetView {
	/**
	 * Shows a loading view
	 */
	public void showLoading();
	
	/**
	 * Clears out old elements
	 */
	public void clear();
	
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

}
