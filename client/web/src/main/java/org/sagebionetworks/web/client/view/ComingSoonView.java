package org.sagebionetworks.web.client.view;

import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.SynapsePresenter;
import org.sagebionetworks.web.client.SynapseView;

import com.google.gwt.user.client.ui.IsWidget;

public interface ComingSoonView extends IsWidget, SynapseView {
	
	/**
	 * Set this view's presenter
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);	
	
	public interface Presenter extends SynapsePresenter {

	}

}
