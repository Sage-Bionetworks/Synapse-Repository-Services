package org.sagebionetworks.web.client.view;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.shared.ColumnMetadata;
import org.sagebionetworks.web.shared.HeaderData;

import com.google.gwt.user.client.ui.IsWidget;

public interface DynamicTableView extends IsWidget {
	
	
	/**
	 * Set the presenter.
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);
	

	/**
	 * Display a message in a pop-up dialog.
	 * @param message
	 */
	public void showMessage(String message);
	
	/**
	 * Set the current rows to display
	 * @param rows
	 * @param offset
	 * @param limit
	 * @param totalCount
	 * @param sortKey
	 * @param ascending
	 */
	public void setRows(RowData data);
	

	/**
	 * The view should synch with the given columns.
	 * @param columnInfoList
	 */
	public void setColumns(List<HeaderData> columnInfoList);
	
	
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
