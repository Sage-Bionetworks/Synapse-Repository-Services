package org.sagebionetworks.web.client.view;

import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.shared.NodeType;

import com.google.gwt.user.client.ui.IsWidget;

public interface LookupView extends IsWidget{
	
	/**
	 * Set this view's presenter
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);
		
	/**
	 * The view pops-up an error dialog.
	 * @param message
	 */
	public void showErrorMessage(String message);

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
	
	public void clear();
	
	public interface Presenter {

		PlaceChanger getPlaceChanger();
	}





}
