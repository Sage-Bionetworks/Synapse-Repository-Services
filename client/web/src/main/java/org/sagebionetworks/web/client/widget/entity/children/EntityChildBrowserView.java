package org.sagebionetworks.web.client.widget.entity.children;

import java.util.List;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.widget.SynapseWidgetView;
import org.sagebionetworks.web.shared.EntityType;
import org.sagebionetworks.web.shared.WhereCondition;

import com.google.gwt.user.client.ui.IsWidget;

public interface EntityChildBrowserView extends IsWidget, SynapseWidgetView {

	/**
	 * Set the presenter.
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);

	/**
	 * Create child browser panes
	 * @param entity
	 * @param entityType
	 * @param canEdit
	 */
	public void createBrowser(Entity entity, EntityType entityType, boolean canEdit);
	
	/**
	 * Sets the Preview Table's details. Preview table only if Entity has "preview" EntityType child
	 * @param previewData
	 */
	void setPreviewTable(PreviewData previewData);

	
	/**
	 * Presenter interface
	 */
	public interface Presenter {

		void refresh();

		PlaceChanger getPlaceChanger();

		List<WhereCondition> getProjectContentsWhereContidions();

		List<EntityType> getContentsSkipTypes();

	}

}
