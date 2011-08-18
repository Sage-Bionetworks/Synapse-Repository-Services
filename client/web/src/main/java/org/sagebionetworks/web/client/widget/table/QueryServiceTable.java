package org.sagebionetworks.web.client.widget.table;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.SearchServiceAsync;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.view.RowData;
import org.sagebionetworks.web.shared.HeaderData;
import org.sagebionetworks.web.shared.QueryConstants.ObjectType;
import org.sagebionetworks.web.shared.SearchParameters;
import org.sagebionetworks.web.shared.TableResults;
import org.sagebionetworks.web.shared.WhereCondition;

import com.extjs.gxt.ui.client.Style.SortDir;
import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.data.BasePagingLoadResult;
import com.extjs.gxt.ui.client.data.BasePagingLoader;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.data.PagingLoadConfig;
import com.extjs.gxt.ui.client.data.PagingLoadResult;
import com.extjs.gxt.ui.client.data.RpcProxy;
import com.extjs.gxt.ui.client.store.ListStore;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

/***
 * This is the presenter with the business logic for this table
 * @author jmhill
 *
 */
public class QueryServiceTable implements QueryServiceTableView.Presenter {

	private QueryServiceTableView view;
	private SearchServiceAsync searchService;

	private String sortKey = null;
	private boolean ascending = false;
	
	// This keeps track of which page we are on.
	private boolean usePager = true;
	public static final int DEFAULT_OFFSET = 0;
	public static final int DEFAULT_LIMIT = 10;
	private int paginationOffset = DEFAULT_OFFSET;
	private int paginationLimit = DEFAULT_LIMIT;
	public static final int DEFAULT_WIDTH = 600;
	public static final int DEFAULT_HEIGHT = 300;
	private List<HeaderData> currentColumns = null;
	private ObjectType type;
	private List<String> visibleColumnIds;
	private List<WhereCondition> where;
	private BasePagingLoader<PagingLoadResult<ModelData>> loader;
	private ListStore<BaseModelData> store;	
	private PagingLoadResult<BaseModelData> loadResultData;
	private AuthenticationController authenticationController; 
	
	@Inject
	public QueryServiceTable() {		
	}
		
	public QueryServiceTable(QueryServiceTableResourceProvider provider, ObjectType type, boolean usePager, PlaceChanger placeChanger) {
		this(provider, type, null, usePager, DEFAULT_WIDTH, DEFAULT_HEIGHT, placeChanger);
	}
	
	public QueryServiceTable(QueryServiceTableResourceProvider provider, ObjectType type, boolean usePager, int width, int height, PlaceChanger placeChanger) {
		this(provider, type, null, usePager, width, height, placeChanger);
	}
	
	public QueryServiceTable(QueryServiceTableResourceProvider provider, ObjectType type, String tableTitle, boolean usePager, int width, int height, final PlaceChanger placeChanger){
		this.view = provider.getView();
		this.searchService = provider.getService();
		this.authenticationController = provider.getAuthenticationController();

		this.view.setPresenter(this);		
		this.view.setTitle(tableTitle);
		this.view.usePager(usePager);
		this.view.setPaginationOffsetAndLength(paginationOffset, paginationLimit);
		this.view.setDimensions(width, height);
		initialize(type, usePager);
		
        RpcProxy<PagingLoadResult<BaseModelData>> proxy = new RpcProxy<PagingLoadResult<BaseModelData>>() {
            @Override
            public void load(Object loadConfig, final AsyncCallback<PagingLoadResult<BaseModelData>> callback) {
            	setCurrentSearchParameters((PagingLoadConfig) loadConfig);            	
            	SearchParameters searchParams = getCurrentSearchParameters();
        		searchService.executeSearch(searchParams, new AsyncCallback<TableResults>() {
        			
        			@Override
        			public void onSuccess(TableResults result) {
        				if(result.getException() != null) {
        					if(!DisplayUtils.handleServiceException(result.getException(), placeChanger, authenticationController.getLoggedInUser())) {
        						// alert user
        						onFailure(null);        					
        					} else {
        						// if the user has already been alerted, just call the callback
        						callback.onFailure(null);
        					}
        					return;
        				}
        				setTableResults(result, callback);
        				view.setPaginationOffsetAndLength(paginationOffset, paginationLimit);        				
        				loadResultData.setOffset(paginationOffset);
        				callback.onSuccess(loadResultData);
        			}
        			
        			@Override
        			public void onFailure(Throwable caught) {
        				view.showMessage("An error occured. Please try reloading the page.");
//        				view.showMessage(caught.getMessage());
//        				DisplayUtils.logger.log(Level.SEVERE, caught.getMessage());        				
        				callback.onFailure(caught);
        			}
        		});
            }
        };
		
        // create a paging loader from the proxy
        loader = new BasePagingLoader<PagingLoadResult<ModelData>>(proxy);
        loader.setRemoteSort(true);
        loader.setReuseLoadConfig(true);                      
        // create the data store from the loader
        store = new ListStore<BaseModelData>(loader);

        // send to view
        view.setStoreAndLoader(store, loader);
        //refreshFromServer();		
	}	
	
	public void setUsePager(boolean usePager) {
		this.view.usePager(usePager);
	}

	public ObjectType getType() {
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
		return new SearchParameters(getDisplayColumns(), this.type.name(), this.where, paginationOffset, paginationLimit, sortKey, ascending);
	}

	private void setCurrentSearchParameters(PagingLoadConfig loadConfig) {
		paginationOffset = loadConfig.getOffset();
		paginationLimit = loadConfig.getLimit();
		view.setPaginationOffsetAndLength(paginationOffset, paginationLimit);
		if(loadConfig.getSortField() != null) {
			String sortColumn = loadConfig.getSortField().replaceFirst("_",".");
			sortKey = getSortColumnForColumnId(sortColumn);
		}

		if(loadConfig.getSortDir() != null)
			ascending = loadConfig.getSortDir() == SortDir.ASC ? true : false; // TODO : support NONE?		
	}
	
	/**
	 * Get the sort Id for a given column
	 * @param sortColumn
	 * @return
	 */
	private String getSortColumnForColumnId(String sortColumn) {
		// Look at the current columns and determine what the sort key is
		for(HeaderData header: currentColumns){
			if(header.getId().equals(sortColumn)){
				return header.getSortId();
			}
		}
		throw new IllegalArgumentException("Cannot find HeaderData for column id: "+sortColumn);
	}

	public List<WhereCondition> getWhere() {
		return where;
	}
	
	public void setTableResults(TableResults result) {
		// First, set the columns
		setCurrentColumns(result.getColumnInfoList());
		// Now set the rows
		RowData data = new RowData(result.getRows(), paginationOffset, paginationLimit, result.getTotalNumberResults(), sortKey, ascending);
		view.setRows(data);
	}	

	public void setTableResults(TableResults result, AsyncCallback<PagingLoadResult<BaseModelData>> callback) {
		// First, set the columns
		setCurrentColumns(result.getColumnInfoList());
		
		List<BaseModelData> dataList = new ArrayList<BaseModelData>();
		for(Map<String,Object> rowMap : result.getRows()) {			
			BaseModelData dataPt = new BaseModelData();
			for(String key : rowMap.keySet()) {
				String cleanKey = key.replaceFirst("\\.", "_");
				Object value = rowMap.get(key); 								
				dataPt.set(cleanKey, value);
			}
			dataList.add(dataPt);
		}
		loadResultData = new BasePagingLoadResult<BaseModelData>(dataList);
		loadResultData.setTotalLength(result.getTotalNumberResults());
		

		// TODO : not needed with GXT loader
		// Now set the rows
		RowData data = new RowData(result.getRows(), paginationOffset, paginationLimit, result.getTotalNumberResults(), sortKey, ascending);		
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
	 *  Reloads the data from the server given the current parameters
	 */
	public void refreshFromServer() {
		loader.load(paginationOffset, paginationLimit);
	}
	
	/**
	 * Does the current column list match the new list
	 * @param other
	 * @return
	 */
	public boolean matchesCurrentColumns(List<HeaderData> other){
		if (currentColumns == null && other != null) {
			return false;
		} 
		if(other == null && currentColumns != null) {
			return false;
		}
		if(currentColumns == null && other == null) {
			return true;
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
		this.paginationLimit = length;
		this.refreshFromServer();
	}

	@Override
	public void toggleSort(String columnKey) {
		// We need to resynch
		sortKey = columnKey;
		ascending = !ascending;
		this.refreshFromServer();		
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
		return paginationLimit;
	}

	@Override
	public void setDispalyColumns(List<String> visibileColumns) {
		setDispalyColumns(visibileColumns, true);
	}		

	public void setDispalyColumns(List<String> visibileColumns, boolean refreshData) {
		this.visibleColumnIds = visibileColumns;
		if(refreshData)
			this.refreshFromServer();
	}
		
	public Widget asWidget() {
		if(type == null) throw new IllegalStateException("The type must be set before this table can be used.");
		view.setPresenter(this);
		return this.view.asWidget();
	}
	
	@Override
	public void setWhereCondition(List<WhereCondition> where) {
		this.where = where;
		this.refreshFromServer();
	}

	private void initialize(ObjectType type, boolean usePager) {
		this.type = type;
		this.view.usePager(usePager);
		if(usePager){
			// A pager will be used.
			this.paginationOffset = DEFAULT_OFFSET;
			this.paginationLimit = DEFAULT_LIMIT;
		}else{
			// Since there is no pager, the limit should be maxed
			this.paginationOffset = DEFAULT_OFFSET;
			this.paginationLimit = Integer.MAX_VALUE;
		}
	}

}
