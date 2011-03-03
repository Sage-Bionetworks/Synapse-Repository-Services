package org.sagebionetworks.web.client.presenter;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.web.client.SearchServiceAsync;
import org.sagebionetworks.web.client.view.DynamicTableView;
import org.sagebionetworks.web.client.view.RowData;
import org.sagebionetworks.web.shared.HeaderData;
import org.sagebionetworks.web.shared.SearchParameters;
import org.sagebionetworks.web.shared.SearchParameters.FromType;
import org.sagebionetworks.web.shared.TableResults;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;


public class DynamicTablePresenter extends AbstractActivity implements DynamicTableView.Presenter{
	
	private DynamicTableView view;
	private SearchServiceAsync service;

	private String sortKey = null;
	private boolean ascending = false;
	
	// This keeps track of which page we are on.
	private int paginationOffest = 0;
	private int paginationLength = 10;
	private List<HeaderData> currentColumns = null;
	private FromType type;
	private List<String> visibleColumnIds;
	

	public FromType getType() {
		return type;
	}

	public void setType(FromType type) {
		this.type = type;
	}

	@Inject
	public DynamicTablePresenter(DynamicTableView view, SearchServiceAsync service){
		this.view = view;
		this.service = service;
		this.view.setPresenter(this);
	}
	
	/**
	 * This flavor should be called if this presenter is contain in another location.
	 */
	public void start() {
		// Refresh the view
		refreshFromServer();
	}

	/**
	 * This flavor of start will be called if this presenter is a true location.
	 */
	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		start();
		// Add the view to the main container
		panel.setWidget(view.asWidget());	
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
		return new SearchParameters(getDisplayColumns(), this.type.name(), paginationOffest, paginationLength, sortKey, ascending);
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
		RowData data = new RowData(result.getRows(), paginationOffest, paginationLength, result.getTotalNumberResults(), sortKey, ascending);
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
		this.paginationOffest = start;
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

	public int getPaginationOffest() {
		return paginationOffest;
	}

	public int getPaginationLength() {
		return paginationLength;
	}

	@Override
	public void setDispalyColumns(List<String> visibileColumns) {
		this.visibleColumnIds = visibileColumns;
	}
	
}
