package org.sagebionetworks.web.client.widget.table;

import java.util.List;

import org.sagebionetworks.web.client.view.RowData;
import org.sagebionetworks.web.shared.HeaderData;
import org.sagebionetworks.web.shared.QueryConstants.ObjectType;
import org.sagebionetworks.web.shared.WhereCondition;

import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.data.BasePagingLoader;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.data.PagingLoadResult;
import com.extjs.gxt.ui.client.store.ListStore;
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
	 * Sets the title at the top of the table
	 * @param title
	 */
	public void setTitle(String title);
	
	/**
	 * Sets the width and height of the grid
	 * @param width
	 * @param height
	 */
	public void setDimensions(int width, int height);
	
	/**
	 * Set paging loader for store
	 * @param store Ext GWT ListStore for the query async service 
	 * @param loader BasePagingLoader for query async service
	 */
	public void setStoreAndLoader(ListStore<BaseModelData> store, BasePagingLoader<PagingLoadResult<ModelData>> loader);
		
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
	 * Set the current pagination offset and length
	 * @param offset which page is viewed
	 * @param length the number of rows shown
	 */
	public void setPaginationOffsetAndLength(int offset, int length);
	
	/**
	 * Defines the communication with the presenter.
	 *
	 */
	public interface Presenter {

				
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
