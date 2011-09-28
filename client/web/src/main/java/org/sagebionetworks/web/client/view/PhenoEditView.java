package org.sagebionetworks.web.client.view;

import org.sagebionetworks.web.client.SynapsePresenter;
import org.sagebionetworks.web.client.SynapseView;

import com.google.gwt.user.client.ui.IsWidget;

public interface PhenoEditView extends IsWidget, SynapseView {
	
	/**
	 * Set Editor Details
	 * @param layerId
	 * @param layerName
	 * @param layerLink
	 * @param datasetLink
	 */
	public void setEditorDetails(String layerId, String layerName, String layerLink, String datasetLink);
	
	/**
	 * Set this view's presenter
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);
		
	
	public interface Presenter extends SynapsePresenter {

		void goBackToLayer();
	}

}
