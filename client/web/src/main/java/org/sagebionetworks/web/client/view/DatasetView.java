package org.sagebionetworks.web.client.view;

import org.sagebionetworks.web.client.presenter.DatasetRow;
import org.sagebionetworks.web.shared.SearchParameters.FromType;

import com.google.gwt.user.client.ui.IsWidget;



/**
 * Defines the communication between the view and presenter for a view of a single datasets.
 * 
 * @author jmhill
 *
 */
public interface DatasetView extends IsWidget {
	
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
	
	public void setDatasetRow(DatasetRow row);
	
	/**
	 * Defines the communication with the presenter.
	 *
	 */
	public interface Presenter {

		
	}


}
