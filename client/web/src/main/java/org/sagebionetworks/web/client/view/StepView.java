package org.sagebionetworks.web.client.view;

import java.util.Date;

import org.sagebionetworks.web.client.SynapsePresenter;
import org.sagebionetworks.web.client.SynapseView;

import com.google.gwt.user.client.ui.IsWidget;

public interface StepView extends IsWidget, SynapseView {
	
	/**
	 * Set this view's presenter
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);
		
	/**
	 * Sets the step details in the view
	 * @param name
	 * @param description
	 * @param creator
	 * @param creationDate
	 * @param status
	 */
	public void setStepDetails(String id, String name, String description,
			String creator, Date creationDate, String status, boolean isAdministrator, boolean canEdit);
		
	
	public interface Presenter extends SynapsePresenter {		
		/**
		 * Refreshes the object for the page
		 */
		public void refresh();
		
		/**
		 * Deletes this step and redirects to the steps home page
		 */
		public void delete(); 
	}

}
