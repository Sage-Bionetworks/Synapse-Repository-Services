package org.sagebionetworks.web.client.view;

import java.util.List;

import org.sagebionetworks.web.client.presenter.DatasetRow;

import com.google.gwt.user.client.ui.IsWidget;

/**
 * The all datasets view shows a paginated table of all datasests that meet some filter criteria.
 * 
 * @author jmhill
 *
 */
public interface AllDatasetsView extends IsWidget {
	
	public void setPresenter(Presenter presenter);
	
	/**
	 * Contains the table data.
	 * @param rows
	 */
	public void setDatasetRows(List<DatasetRow> rows, int offset, int limit, int totalCount, String sortKey, boolean ascending);
	
	public void showErrorMessage(String message);
	
	/**
	 * Defines the communication with the presenter.
	 *
	 */
	public interface Presenter {
		/**
		 * Change the page.
		 * @param start
		 * @param length
		 */
		void pageTo(int start, int length);

		/**
		 * Toggle the sort on a column
		 * @param columnKey
		 */
		void toggleSort(String columnKey);
		
	}


}
