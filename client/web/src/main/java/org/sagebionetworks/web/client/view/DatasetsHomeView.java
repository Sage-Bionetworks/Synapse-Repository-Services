package org.sagebionetworks.web.client.view;

import java.util.List;

import org.sagebionetworks.web.client.PlaceChanger;

import com.google.gwt.user.client.ui.IsWidget;

public interface DatasetsHomeView extends IsWidget{
	
	/**
	 * Set this view's presenter
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);
	
	public void setVisibleColumns(List<String> visible);
	
	/**
	 * The view pops-up an error dialog.
	 * @param message
	 */
	public void showErrorMessage(String message);
	
	
	public interface Presenter {
		
		/**
		 * Called when the edit columns button is pushed.
		 */
		public void onEditColumns();
		
		public PlaceChanger getPlaceChanger();
		
	}

}
