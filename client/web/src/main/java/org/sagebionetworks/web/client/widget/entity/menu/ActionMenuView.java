package org.sagebionetworks.web.client.widget.entity.menu;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.events.EntityUpdatedHandler;
import org.sagebionetworks.web.client.widget.SynapseWidgetView;
import org.sagebionetworks.web.shared.EntityType;

import com.google.gwt.user.client.ui.IsWidget;

public interface ActionMenuView extends IsWidget, SynapseWidgetView {

	/**
	 * Set the presenter.
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);


	/**
	 * Build menus for this entity
	 * @param entity
	 * @param entityType 
	 * @param canEdit 
	 * @param isAdministrator 
	 */
	public void createMenu(Entity entity, EntityType entityType, boolean isAdministrator, boolean canEdit);
	
	/**
	 * Presenter interface
	 */
	public interface Presenter {

		void fireEntityUpdatedEvent();

		PlaceChanger getPlaceChanger();

		void deleteEntity();

	}
}
