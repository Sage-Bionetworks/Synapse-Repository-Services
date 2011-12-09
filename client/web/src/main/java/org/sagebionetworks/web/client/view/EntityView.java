package org.sagebionetworks.web.client.view;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.registry.EntityTypeMetadata;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.SynapseView;

import com.google.gwt.user.client.ui.IsWidget;

public interface EntityView extends IsWidget, SynapseView {
	
	/**
	 * Set this view's presenter
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);
		
	public interface Presenter {

		PlaceChanger getPlaceChanger();

		/**
		 * refreshes the entity from the service and redraws the view
		 */
		public void refresh();
	}

	/**
	 * Set entity to display
	 * @param entity
	 * @param entityMetadata 
	 */
	public void setEntity(Entity entity);

	
}
