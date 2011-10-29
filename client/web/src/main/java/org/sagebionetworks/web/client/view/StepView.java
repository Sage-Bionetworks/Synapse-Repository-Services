package org.sagebionetworks.web.client.view;

import java.util.Date;
import java.util.Set;

import org.sagebionetworks.web.shared.EnvironmentDescriptor;
import org.sagebionetworks.web.shared.Reference;
import org.sagebionetworks.web.client.SynapsePresenter;
import org.sagebionetworks.web.client.SynapseView;

import com.google.gwt.user.client.ui.IsWidget;

/**
 * @author deflaux
 *
 */
public interface StepView extends IsWidget, SynapseView {

	/**
	 * Set this view's presenter
	 * 
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);

	/**
	 * Sets the step details in the view
	 * @param id 
	 * 
	 * @param name
	 * @param description
	 * @param createdBy 
	 * @param creationDate
	 * @param startDate 
	 * @param endDate 
	 * @param commandLine 
	 * @param code 
	 * @param input 
	 * @param output 
	 * @param environmentDescriptors 
	 * @param isAdministrator 
	 * @param canEdit 
	 */
	public void setStepDetails(String id, String name, String description,
			String createdBy, Date creationDate, Date startDate, Date endDate,
			String commandLine, Set<Reference> code, Set<Reference> input,
			Set<Reference> output,
			Set<EnvironmentDescriptor> environmentDescriptors,
			boolean isAdministrator, boolean canEdit);

	/**
	 * @author deflaux
	 *
	 */
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
