package org.sagebionetworks.web.client.widget.table;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.SearchServiceAsync;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.view.table.ColumnFactory;
import org.sagebionetworks.web.client.view.table.DateColumn;
import org.sagebionetworks.web.shared.EntityType;
import org.sagebionetworks.web.shared.HeaderData;
import org.sagebionetworks.web.shared.QueryConstants.ObjectType;
import org.sagebionetworks.web.shared.WhereCondition;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.data.BasePagingLoader;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.data.PagingLoadResult;
import com.extjs.gxt.ui.client.data.RpcProxy;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.toolbar.PagingToolBar;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;

public class QueryTableFactory {

	private static final int PAGINATION_LIMIT_DEFAULT = 10; 
	
	private SearchServiceAsync searchService;	
	private AuthenticationController authenticationController;
	private ColumnFactory columnFactory;
	
	@Inject
	public QueryTableFactory(SearchServiceAsync searchService, AuthenticationController authenticationController, ColumnFactory columnFactory) {
		this.searchService = searchService;
		this.authenticationController = authenticationController;
		this.columnFactory = columnFactory;
	}
				
	public void createColumnModel(EntityType entityType, final PlaceChanger placeChanger, final AsyncCallback<ColumnModel> callback) {
		QueryTableFactoryPresenter presenter = new QueryTableFactoryPresenter(searchService, authenticationController, columnFactory);
		
		presenter.getTableColumns(entityType, placeChanger, new AsyncCallback<List<HeaderData>>() {
			@Override
			public void onSuccess(List<HeaderData> list) {				
				// create columns
				List<ColumnConfig> columns = new ArrayList<ColumnConfig>();
				for (final HeaderData meta : list) {
					if (meta != null && meta.getId() != null
							&& meta.getDisplayName() != null) {
						// Now create the column.
						final Column<Map<String, Object>, ?> columnRenderer = columnFactory.createColumn(meta);						
						final String columnId = meta.getId().replaceFirst("\\.", "_");	
						ColumnConfig colConfig = new ColumnConfig(columnId, meta.getDisplayName(), meta.getColumnWidth());			
						
						GridCellRenderer<BaseModelData> cellRenderer = configureGridCellRenderer(columnRenderer);
						colConfig.setRenderer(cellRenderer);				
						columns.add(colConfig);
					}		
				}							
				callback.onSuccess(new ColumnModel(columns));
			}
			
			@Override
			public void onFailure(Throwable caught) {
				callback.onFailure(caught);
			}
		});
	}	
	
	public ContentPanel createGridPanel(final EntityType entityType,
			ColumnModel cm, final List<WhereCondition> where,
			final PlaceChanger placeChanger, Integer paginationLimit) {
		if (cm == null || cm.getColumns() == null
				|| cm.getColumns().size() <= 0) {
			return null;
		}
		
		final QueryTableFactoryPresenter presenter = new QueryTableFactoryPresenter(searchService, authenticationController, columnFactory);		 
		
        RpcProxy<PagingLoadResult<BaseModelData>> proxy = new RpcProxy<PagingLoadResult<BaseModelData>>() {
            @Override
            public void load(Object loadConfig, final AsyncCallback<PagingLoadResult<BaseModelData>> callback) {
            	presenter.loadData(entityType, loadConfig, where, placeChanger, callback);
            }
        };
		
        // create a paging loader from the proxy
        BasePagingLoader<PagingLoadResult<ModelData>> loader = new BasePagingLoader<PagingLoadResult<ModelData>>(proxy);
        loader.setRemoteSort(true);
        loader.setReuseLoadConfig(true);                
                
		// add the real store with built in loader
		ListStore<BaseModelData> store = new ListStore<BaseModelData>(loader);
		Grid<BaseModelData> grid = new Grid<BaseModelData>(store, cm);
		grid.setLayoutData(new FitLayout());
		grid.setStateful(false);		
		grid.setLoadMask(true);
		grid.getView().setForceFit(true);
		grid.setAutoWidth(false);
		grid.setStyleAttribute("borderTop", "none");
		grid.setBorders(false);
		grid.setStripeRows(true);
		
		ContentPanel cp = new ContentPanel();
		cp.setLayout(new FitLayout());
		cp.setBodyBorder(true);
		cp.setButtonAlign(HorizontalAlignment.CENTER);		

		// create bottom paging toolbar				
		if(paginationLimit == null || paginationLimit < 0) 
			paginationLimit = PAGINATION_LIMIT_DEFAULT;
	    PagingToolBar toolBar = new PagingToolBar(paginationLimit);        
        toolBar.bind(loader);
        toolBar.setSpacing(2);
        toolBar.insert(new SeparatorToolItem(), toolBar.getItemCount() - 2);
        
    	cp.setBottomComponent(toolBar);
		cp.add(grid);		
		
		// start initial data load from server
		loader.load();
		
        return cp;
	}

	private Map<String, Object> convertBackToDot(Map<String, Object> properties) {
		Map<String, Object> dotMap = new HashMap<String, Object>();
		for(String key : properties.keySet()) {
			String dotKey = key.replaceFirst("_", ".");
			dotMap.put(dotKey, properties.get(key));
		}		
		return dotMap;
	}

	private GridCellRenderer<BaseModelData> configureGridCellRenderer(
			final Column<Map<String, Object>, ?> columnRenderer) {
		// configure cell renderer
		GridCellRenderer<BaseModelData> cellRenderer = new GridCellRenderer<BaseModelData>() {
			public String render(BaseModelData model, String property, ColumnData config, 
					int rowIndex, int colIndex, ListStore<BaseModelData> store, Grid<BaseModelData> grid) {
				// render column with appropriate renderer
				Map<String, Object> dotMap = convertBackToDot(model.getProperties());
				Object value = columnRenderer.getValue(dotMap);							
				// determine type of value and return reasonable string
				if (columnRenderer instanceof DateColumn) {
					 // Date
					 Date dateValue = (Date) value;
					 if(dateValue == null) {
						 return "";
					 } else {
						 DateTimeFormat formatter = DateTimeFormat.getFormat("dd-MMM-yyyy");
						 return formatter.format(dateValue);
					 }						
				} else {
					// catch all for types that don't need specific rendering beyond string
					if(value != null)
						return "<div style=\"white-space: normal;\">"+ value.toString() + "</div>"; // allow text to wrap to multiple lines
					else
						return null;
				}
			}
		};
		return cellRenderer;
	}			

}
