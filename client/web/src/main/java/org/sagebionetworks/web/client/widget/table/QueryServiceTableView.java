package org.sagebionetworks.web.client.widget.table;

import java.util.List;

import org.sagebionetworks.web.client.view.RowData;
import org.sagebionetworks.web.shared.HeaderData;
import org.sagebionetworks.web.shared.SearchParameters.FromType;
import org.sagebionetworks.web.shared.WhereCondition;

import com.google.gwt.user.client.ui.IsWidget;

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
		 * Initialize the table without trigging a rebuild.
		 * 
		 * @param type
		 * @param usePager
		 * @param offest
		 * @param limit
		 */
		public void initialize(FromType type, boolean usePager);
		
		/**
		 * Set the where condition.  This will trigger a refresh.
		 * @param where
		 */
		public void setWhereCondition(WhereCondition where);
		
		/**
		 * Change the page.
		 * @param start
		 * @param length
		 */
		public void pageTo(int start, int length);

		/**
		 * Toggle the sort on a column
		 * @param columnKey
		 */
		public void toggleSort(String columnKey);
		
		/**
		 * Set the columns that should be displayed.
		 * @param visibileColumns
		 */
		public void setDispalyColumns(List<String> visibileColumns);
		
		
	}

}
