package org.sagebionetworks.web.client.widget.table;

import java.util.List;

import org.sagebionetworks.web.client.view.RowData;
import org.sagebionetworks.web.shared.HeaderData;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.view.client.HasRows;

public interface QueryServiceTableView extends IsWidget {
	

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
	 * Should this view use a pager
	 * @param use
	 */
	public void usePager(boolean use);
	

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
		
		/**
		 * Set the columns that should be displayed.
		 * @param visibileColumns
		 */
		public void setDispalyColumns(List<String> visibileColumns);
		
		
	}

}
