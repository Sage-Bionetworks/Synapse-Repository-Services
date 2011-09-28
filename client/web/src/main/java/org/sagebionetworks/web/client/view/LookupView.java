package org.sagebionetworks.web.client.view;

import org.sagebionetworks.web.client.SynapsePresenter;
import org.sagebionetworks.web.client.SynapseView;

import com.google.gwt.user.client.ui.IsWidget;

public interface LookupView extends IsWidget, SynapseView {
	
	/**
	 * Set this view's presenter
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);
		
	/**
	 * Show the user that there was a failure in finding the 
	 * given entity
	 * @param entityId
	 */
	public void showLookupFailed(String entityId);
	
	public void showLooking(String entityId);

	public void showForwarding();

	public void doneLooking();
	
	public void showUnknownType(String type, String id);
	
	
	public interface Presenter extends SynapsePresenter {

	}





}
