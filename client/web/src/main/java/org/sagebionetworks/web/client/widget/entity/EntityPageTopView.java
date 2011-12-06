package org.sagebionetworks.web.client.widget.entity;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.widget.SynapseWidgetView;

import com.google.gwt.user.client.ui.IsWidget;

public interface EntityPageTopView extends IsWidget, SynapseWidgetView {

	/**
	 * Set the presenter.
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);
	
	public void setEntityDetails(Entity entity, boolean isAdministrator, boolean canEdit);
	
	/**
	 * Alert the user that their entity delete failed 
	 */	
	public void showEntityDeleteFailure();
	
	/**
	 * Presenter interface
	 */
	public interface Presenter {

		PlaceChanger getPlaceChanger();

		void refresh();

		void delete();

	}
}
