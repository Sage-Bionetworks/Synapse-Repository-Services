package org.sagebionetworks.web.client.view;

import org.sagebionetworks.web.client.presenter.DatasetRow;
import org.sagebionetworks.web.client.presenter.LayerRow;

import com.google.gwt.user.client.ui.IsWidget;



/**
 * Defines the communication between the view and presenter for a view of a single datasets.
 * 
 * @author jmhill
 *
 */
public interface LayerView extends IsWidget {
	
	/**
	 * This how the view communicates with the presenter.
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);
	
	/**
	 * The view pops-up an error dialog.
	 * @param message
	 */
	public void showErrorMessage(String message);
	
	public void setLayerRow(LayerRow row);
	
	/**
	 * Defines the communication with the presenter.
	 *
	 */
	public interface Presenter {

		
	}

}
