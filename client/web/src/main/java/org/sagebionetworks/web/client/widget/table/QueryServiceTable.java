package org.sagebionetworks.web.client.widget.table;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.web.client.SearchServiceAsync;
import org.sagebionetworks.web.client.view.RowData;
import org.sagebionetworks.web.shared.HeaderData;
import org.sagebionetworks.web.shared.SearchParameters;
import org.sagebionetworks.web.shared.WhereCondition;
import org.sagebionetworks.web.shared.SearchParameters.FromType;
import org.sagebionetworks.web.shared.TableResults;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

/***
 * This is the presenter with the business logic for this table
 * @author jmhill
 *
 */
public class QueryServiceTable implements IsWidget, QueryServiceTableView.Presenter {
	
	@Inject
	public QueryServiceTable(QueryServiceTableView view, SearchServiceAsync service){
		this.view = view;
		this.service = service;
		this.view.setPresenter(this);
	}
	
	private QueryServiceTableView view;
	private SearchServiceAsync service;

	private String sortKey = null;
	private boolean ascending = false;
	
	// This keeps track of which page we are on.
	public static final int DEFAULT_OFFSET = 0;
	public static final int DEFAULT_LIMIT = 10;
	private int paginationOffset = DEFAULT_OFFSET;
	private int paginationLength = DEFAULT_LIMIT;
	private List<HeaderData> currentColumns = null;
	private FromType type;
	private List<String> visibleColumnIds;
	private WhereCondition where;
	
	public void setUsePager(boolean usePager) {
		this.view.usePager(usePager);
	}

	public FromType getType() {
		return type;
	}
	
	/**
	 * Get the columns to display.
	 * @return
	 */
	public List<String> getDisplayColumns(){
		if(this.visibleColumnIds == null){
			// Return an empty list, which will be interpreted as the default
			return new LinkedList<String>();
		}else{
			return this.visibleColumnIds;
		}
	}
	
	/**
	 * Helper for getting the search parameters that will be used.
	 * This is allows tests to make the parameters used by this class.
	 * @return
	 */
	public SearchParameters getCurrentSearchParameters(){
		return new SearchParameters(getDisplayColumns(), this.type.name(), this.where, paginationOffset, paginationLength, sortKey, ascending);
	}

	public WhereCondition getWhere() {
		return where;
	}

	/**
	 * Asynchronous call that will execute the current query and set the results.
	 */
	public void refreshFromServer() {
		service.executeSearch(getCurrentSearchParameters(), new AsyncCallback<TableResults>() {
			
			@Override
			public void onSuccess(TableResults result) {
				setTableResults(result);
			}
			
			@Override
			public void onFailure(Throwable caught) {
				view.showMessage(caught.getMessage());
			}
		});
	}

	public void setTableResults(TableResults result) {
		// First, set the columns
		setCurrentColumns(result.getColumnInfoList());
		// Now set the rows
		RowData data = new RowData(result.getRows(), paginationOffset, paginationLength, result.getTotalNumberResults(), sortKey, ascending);
		view.setRows(data);
	}

	/**
	 * Set the current columns
	 * @param columnInfoList
	 */
	public void setCurrentColumns(List<HeaderData> columnInfoList) {
		// If this list has changed then we need to let the view know
		// about the new columns
		if(!matchesCurrentColumns(columnInfoList)){
			// The columns have change so we need to update the view
			this.currentColumns = columnInfoList;
			view.setColumns(columnInfoList);
		}
	}
	
	/**
	 * Does the current column list match the new list
	 * @param other
	 * @return
	 */
	public boolean matchesCurrentColumns(List<HeaderData> other){
		if (currentColumns == null) {
			if (other != null)
				return false;
		}
		if(currentColumns.size() != other.size()) return false;
		// If a new type of HeaderData is added, and they do
		// not implement equals() (a very likely scenario) we
		// do not want to rebuild the whole table each time.
		// Rather we only want to rebuild the table when
		// a column id has changed.
		for(int i=0; i<currentColumns.size(); i++){
			String thisKey = currentColumns.get(i).getId();
			String otherKey = other.get(i).getId();
			if(!thisKey.equals(otherKey)) return false;
		}
		return true;
	}
	
	@Override
	public void pageTo(int start, int length) {
		this.paginationOffset = start;
		this.paginationLength = length;
		refreshFromServer();
	}

	@Override
	public void toggleSort(String columnKey) {
		// We need to resynch
		sortKey = columnKey;
		ascending = !ascending;
		refreshFromServer();
	}

	public String getSortKey() {
		return sortKey;
	}

	public boolean isAscending() {
		return ascending;
	}

	public int getPaginationOffset() {
		return paginationOffset;
	}

	public int getPaginationLength() {
		return paginationLength;
	}

	@Override
	public void setDispalyColumns(List<String> visibileColumns) {
		this.visibleColumnIds = visibileColumns;
		refreshFromServer();
	}
	
	@Override
	public Widget asWidget() {
		if(type == null) throw new IllegalStateException("The type must be set before this table can be used.");
		return this.view.asWidget();
	}

	@Override
	public void setWhereCondition(WhereCondition where) {
		this.where = where;
		this.refreshFromServer();
	}

	@Override
	public void initialize(FromType type, boolean usePager) {
		this.type = type;
		this.view.usePager(usePager);
		if(usePager){
			// A pager will be used.
			this.paginationOffset = DEFAULT_OFFSET;
			this.paginationLength = DEFAULT_LIMIT;
		}else{
			// Since there is no pager, the limit should be maxed
			this.paginationOffset = DEFAULT_OFFSET;
			this.paginationLength = Integer.MAX_VALUE;
		}
	}


}
