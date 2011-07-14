package org.sagebionetworks.web.client.view;

import java.util.Date;

import org.sagebionetworks.web.client.PlaceChanger;

import com.google.gwt.user.client.ui.IsWidget;

public interface ProjectView extends IsWidget{
	
	/**
	 * Set this view's presenter
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);
		
	/**
	 * Sets the project detials in the view
	 * @param name
	 * @param description
	 * @param creator
	 * @param creationDate
	 * @param status
	 */
	public void setProjectDetails(String id, String name, String description,
			String creator, Date creationDate, String status, boolean isAdministrator, boolean canEdit);
	
	/**
	 * The view pops-up an error dialog.
	 * @param message
	 */
	public void showErrorMessage(String message);
	
	/**
	 * Shows a message in the lower right of the page
	 * @param message
	 */
	public void showInfo(String title, String message);
	
	
	public interface Presenter {
		
		/**
		 * Refreshes the object for the page
		 */
		public void refresh();

		public PlaceChanger getPlaceChanger();
		
		/**
		 * Deletes this project and redirects to the projects home page
		 */
		public void delete(); 
	}

}
