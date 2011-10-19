package org.sagebionetworks.web.client.widget.filter;

import java.util.List;

import org.sagebionetworks.web.shared.WhereCondition;

import com.google.gwt.user.client.ui.IsWidget;

/**
 * Interface defining the relationship between the filter view and presenter.
 * 
 * @author jmhill
 *
 */
public interface QueryFilterView extends IsWidget{
	
	/**
	 * 
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);
	
	/**
	 * The Presenter
	 *
	 */
	public interface Presenter {
		/**
		 * Tell the presenter to change the selection.
		 * @param id
		 * @param selectedIndex
		 */
		void setSelectionChanged(String id, int selectedIndex);
		
		
	}

	/**
	 * Show an error dialog
	 * @param message
	 */
	public void showError(String message);

	/**
	 * Set the data to render
	 * @param viewData
	 */
	public void setDisplayData(List<DropdownData> viewData, List<WhereCondition> currentFilters);

}
