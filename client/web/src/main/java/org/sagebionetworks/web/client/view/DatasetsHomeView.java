package org.sagebionetworks.web.client.view;

import com.google.gwt.user.client.ui.IsWidget;

public interface DatasetsHomeView extends IsWidget{
	
	/**
	 * Set this view's presenter
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);
	
	/**
	 * This will be called after the presenter has been started.
	 */
	public void onStart();
	
	public interface Presenter {
		
		/**
		 * Called when the edit columns button is pushed.
		 */
		public void onEditColumns();
		
	}

}
