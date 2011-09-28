package org.sagebionetworks.web.client.view;

import java.util.Date;

import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.SynapsePresenter;
import org.sagebionetworks.web.client.SynapseView;

import com.google.gwt.user.client.ui.IsWidget;

public interface ProjectView extends IsWidget, SynapseView {
	
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
		
	
	public interface Presenter extends SynapsePresenter {		
		/**
		 * Refreshes the object for the page
		 */
		public void refresh();
		
		/**
		 * Deletes this project and redirects to the projects home page
		 */
		public void delete(); 
	}

}
