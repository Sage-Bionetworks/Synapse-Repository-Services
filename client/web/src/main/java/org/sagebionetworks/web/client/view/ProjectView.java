package org.sagebionetworks.web.client.view;

import java.util.Date;

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
			String creator, Date creationDate, String status);
	
	/**
	 * The view pops-up an error dialog.
	 * @param message
	 */
	public void showErrorMessage(String message);
	
	
	public interface Presenter {
		
	}

}
