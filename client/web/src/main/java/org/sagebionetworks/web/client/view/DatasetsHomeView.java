package org.sagebionetworks.web.client.view;

import java.util.List;

import com.google.gwt.user.client.ui.IsWidget;

public interface DatasetsHomeView extends IsWidget{
	
	/**
	 * Set this view's presenter
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);
	
	public void setVisibleColumns(List<String> visible);
	
	
	public interface Presenter {
		
		/**
		 * Called when the edit columns button is pushed.
		 */
		public void onEditColumns();
		
	}

}
