package org.sagebionetworks.web.client.widget.table;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.SearchServiceAsync;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.view.table.ColumnFactory;
import org.sagebionetworks.web.shared.EntityType;
import org.sagebionetworks.web.shared.HeaderData;
import org.sagebionetworks.web.shared.SearchParameters;
import org.sagebionetworks.web.shared.TableResults;
import org.sagebionetworks.web.shared.WhereCondition;

import com.extjs.gxt.ui.client.Style.SortDir;
import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.data.BasePagingLoadResult;
import com.extjs.gxt.ui.client.data.PagingLoadConfig;
import com.extjs.gxt.ui.client.data.PagingLoadResult;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class QueryTableFactoryPresenter {	
	
	public class TableContext {
		public static final int DEFAULT_OFFSET = 0;
		public static final int DEFAULT_LIMIT = 10;
		
		public int paginationOffset = DEFAULT_OFFSET;
		public int paginationLimit = DEFAULT_LIMIT; 
		public String sortKey = null;
		public boolean ascending = false;
		public List<HeaderData> currentColumns = null;		
		public List<String> visibleColumnIds;
		public BasePagingLoadResult<BaseModelData> loadResultData;	
	}
	
	private SearchServiceAsync searchService;	
	private AuthenticationController authenticationController;
	private ColumnFactory columnFactory;
		
	public QueryTableFactoryPresenter(SearchServiceAsync searchService, AuthenticationController authenticationController, ColumnFactory columnFactory) {
		this.searchService = searchService;
		this.authenticationController = authenticationController;
		this.columnFactory = columnFactory;
	}

	// can go into presenter class
	public void getTableColumns(EntityType entityType, final PlaceChanger placeChanger, final AsyncCallback<List<HeaderData>> callback) {
		// super simple params just to get columns
		SearchParameters searchParams = new SearchParameters(new ArrayList<String>(), entityType, null, 1, 1, null, false);
		searchService.executeSearch(searchParams, new AsyncCallback<TableResults>() {			
			@Override
			public void onSuccess(TableResults result) {
				if(result.getException() != null) {
					DisplayUtils.handleServiceException(result.getException(), placeChanger, authenticationController.getLoggedInUser());				
					onFailure(null);        										
					return;
				}
				// return columns
				callback.onSuccess(result.getColumnInfoList());
			}
			
			@Override
			public void onFailure(Throwable caught) {
				callback.onFailure(caught);
			}
		});
	}
	
	public void loadData(EntityType entityType, Object loadConfig, final List<WhereCondition> where, final PlaceChanger placeChanger, final AsyncCallback<PagingLoadResult<BaseModelData>> callback) {
		final TableContext context = new TableContext();
		setCurrentSearchParameters(context, (PagingLoadConfig) loadConfig);
		SearchParameters searchParams = getCurrentSearchParameters(context, entityType, where);
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
				setTableResults(context, result, callback);        				        			        				
				callback.onSuccess(context.loadResultData);        				
			}
			
			@Override
			public void onFailure(Throwable caught) {
				callback.onFailure(caught);
			}
		});
	}

	
	
	/*
	 * Private Methods
	 */	
	private void setCurrentSearchParameters(TableContext context, PagingLoadConfig loadConfig) {
		context.paginationOffset = loadConfig.getOffset();
		context.paginationLimit = loadConfig.getLimit();	
		if(loadConfig.getSortField() != null) {
			String sortColumn = loadConfig.getSortField().replaceFirst("_",".");
			context.sortKey = getSortColumnForColumnId(context, sortColumn);
		}

		if(loadConfig.getSortDir() != null)
			context.ascending = loadConfig.getSortDir() == SortDir.ASC ? true : false; // TODO : support NONE?		
	}

	/**
	 * Helper for getting the search parameters that will be used.
	 * This is allows tests to make the parameters used by this class.
	 * @param context 
	 * @return
	 */
	private SearchParameters getCurrentSearchParameters(TableContext context, EntityType entityType, List<WhereCondition> where){
		// Query Service is one-based, so adjusting the pagination offset when getting the search parameters
		int oneBasedOffset = context.paginationOffset + 1;
		
		// If the sortKey is based on creator/createdBy or creationDate/createdOn, we want to be able to find AND sort the columns correctly
		// Else, just leave the sortKey alone.
		List<String> visibleColumnIds = context.visibleColumnIds == null ? new LinkedList<String>() : context.visibleColumnIds;
		return new SearchParameters(visibleColumnIds, entityType, where, oneBasedOffset, context.paginationLimit, context.sortKey, context.ascending);
	}
	
	private void setTableResults(TableContext context, TableResults result, AsyncCallback<PagingLoadResult<BaseModelData>> callback) {
		// First, set the columns
		context.currentColumns = result.getColumnInfoList();
		
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
		context.loadResultData = new BasePagingLoadResult<BaseModelData>(dataList);
		context.loadResultData.setTotalLength(result.getTotalNumberResults());
		context.loadResultData.setOffset(context.paginationOffset);
	}

	/**
	 * Get the sort Id for a given column
	 * @param res 
	 * @param sortColumn
	 * @return
	 */
	private String getSortColumnForColumnId(TableContext context, String sortColumn) {
		// Look at the current columns and determine what the sort key is
		if(context.currentColumns != null) {
			for(HeaderData header: context.currentColumns){
				if(header.getId().equals(sortColumn)){
					return header.getSortId();
				}
			}
		}
		throw new IllegalArgumentException("Cannot find HeaderData for column id: "+sortColumn);
	}


}
