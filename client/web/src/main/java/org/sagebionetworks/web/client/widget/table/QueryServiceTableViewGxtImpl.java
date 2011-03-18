package org.sagebionetworks.web.client.widget.table;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.ImagePrototypeSingleton;
import org.sagebionetworks.web.client.view.RowData;
import org.sagebionetworks.web.client.view.table.ColumnFactory;
import org.sagebionetworks.web.client.view.table.DateColumn;
import org.sagebionetworks.web.shared.HeaderData;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.data.BasePagingLoader;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.data.PagingLoadResult;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.FlowLayout;
import com.extjs.gxt.ui.client.widget.toolbar.PagingToolBar;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class QueryServiceTableViewGxtImpl extends LayoutContainer implements QueryServiceTableView {
	
	ImagePrototypeSingleton prototype;

	int columnCount = 0; 	// How many columns are we currently rendering
	private boolean usePager = false;
	private Presenter presenter;
	private ColumnFactory columnFactory;
	private ContentPanel cp;
	private Grid<BaseModelData> grid;
    private PagingToolBar toolBar;
    private ColumnModel columnModel;
    private BasePagingLoader<PagingLoadResult<ModelData>> loader;
    private ListStore<BaseModelData> store;
    private String tableTitle;
	public static final int DEFAULT_WIDTH = 600;
	public static final int DEFAULT_HEIGHT = 300;
    private int gridWidth = DEFAULT_WIDTH;
    private int gridHeight = DEFAULT_HEIGHT;
    private int paginationOffset;
    private int paginationLimit;
 	
	/**
	 * Gin will inject all of the params.
	 * 
	 * @param cellTableResource
	 */
	@Inject
	public QueryServiceTableViewGxtImpl(ImagePrototypeSingleton prototype, ColumnFactory columnFactory) { 
		this.prototype = prototype;
		this.columnFactory = columnFactory;

		// configure a generic initial column model and store. replaced by presenter when Service is first called
        List<ColumnConfig> columns = new ArrayList<ColumnConfig>();
        columns.add(new ColumnConfig("columnId", "Columns", 150));
        columnModel = new ColumnModel(columns);
        store = new ListStore<BaseModelData>();         
		cp = new ContentPanel();		
	}
	
	@Override
	protected void onRender(Element parent, int index) {
		super.onRender(parent, index);		
		setLayout(new FlowLayout(10));
		
		// setup grid and components for first time
		if(tableTitle != null) {
			cp.setHeading(tableTitle);
		} else {
			cp.setHeaderVisible(false);
		}
		
		cp.setBodyBorder(true);
		cp.setButtonAlign(HorizontalAlignment.CENTER);
		cp.setLayout(new FitLayout());
		cp.getHeader().setIconAltText("Grid Icon");
		cp.setSize(gridWidth, gridHeight);

		// create toolbar
		int initialPaginationLimit = paginationLimit > 0 ? paginationLimit : 10;
        toolBar = new PagingToolBar(initialPaginationLimit); // default initial value       
        toolBar.bind(loader);
        toolBar.setSpacing(2);
        toolBar.insert(new SeparatorToolItem(), toolBar.getItemCount() - 2);
        if(usePager) {
        	cp.setBottomComponent(toolBar);
        }	       
			        
		// create grid
		grid = new Grid<BaseModelData>(store, columnModel);

		// grid.setStateId(filterViewController.getGroupId()); // give this
		// grid a unique id to be commanded by the filter view. should go
		// through presenter?
		grid.setStateful(true);
		
		grid.setLoadMask(true);
		grid.getView().setForceFit(true);
		grid.setAutoWidth(false);

		grid.setStyleAttribute("borderTop", "none");
		grid.setBorders(false);
		grid.setStripeRows(true);
		
//		grid.addListener(Events.Attach,
//				new Listener<GridEvent<BaseModelData>>() {
//					public void handleEvent(GridEvent<BaseModelData> be) {
//						PagingLoadConfig config = createLoadConfig();
//						loader.load(config);
//					}
//				});
		
		cp.add(grid);		
       
		add(cp);
	}	
	
	
//    private BasePagingLoadConfig createLoadConfig() {
//        BasePagingLoadConfig config = new BasePagingLoadConfig();
//        config.setOffset(this.paginationOffset);
//        config.setLimit(this.paginationLimit); 
//
//        Map<String, Object> state = grid.getState();
//        if (state.containsKey("offset")) {
//            int offset = (Integer) state.get("offset");
//            int limit = (Integer) state.get("limit");
//            config.setOffset(offset);
//            config.setLimit(limit);
//        }
//        if (state.containsKey("sortField")) {
//            config.setSortField((String) state.get("sortField"));
//            config.setSortDir(SortDir.valueOf((String) state.get("sortDir")));
//        }
//
//        return config;
//    }
//	

	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void setRows(RowData data) {
		// TODO : delete
		// This is not needed with the loader 
	}
	
	public void updateSortColumns(String sortKey, boolean ascending){
		// TODO : not sure this is needed anymore
	}

	@Override
	public void showMessage(String message) {
		MessageBox.info("Message", message, null);
	}

	@Override
	public void setDimensions(int width, int height) {
		this.gridWidth = width;
		this.gridHeight = height;		
	}
	
	@Override
	public void setColumns(List<HeaderData> list) {
		// create columns
		List<ColumnConfig> columns = new ArrayList<ColumnConfig>();
		for (HeaderData meta : list) {
			if (meta != null && meta.getId() != null
					&& meta.getDisplayName() != null) {
				// Now create the column.
				final Column<Map<String, Object>, ?> columnRenderer = columnFactory.createColumn(meta);
				// TODO : check if column is allowed to be sortable?				
				
				String columnId = meta.getId().replaceAll("\\.", "_");	
				ColumnConfig colConfig = new ColumnConfig(columnId, meta.getDisplayName(), meta.getColumnWidth());			
				
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
				colConfig.setRenderer(cellRenderer);				
				columns.add(colConfig);
			}		
		}
		// Keep the column count
		columnCount = list.size();
		columnModel = new ColumnModel(columns);
		// update the grid with the new columnModel
		grid.reconfigure(store, columnModel);
		
		updateToolBar();
	}
	
	private Map<String, Object> convertBackToDot(Map<String, Object> properties) {
		Map<String, Object> dotMap = new HashMap<String, Object>();
		for(String key : properties.keySet()) {
			String dotKey = key.replaceAll("_", ".");
			dotMap.put(dotKey, properties.get(key));
		}		
		return dotMap;
	}

	
	public int getColumnCount(){
		return columnCount;
	}

	@Override
	public void usePager(boolean use) {
		this.usePager = use;
	}

	@Override
	public void setStoreAndLoader(ListStore<BaseModelData> store, BasePagingLoader<PagingLoadResult<ModelData>> loader) {
		this.store = store;
		this.loader = loader;
		
		// update the grid with the new store
		if (grid != null) {
			grid.reconfigure(store, columnModel);
			updateToolBar();
		}
	}

	@Override
	public void setPaginationOffsetAndLength(int offset, int length) {
		paginationOffset = offset;
		paginationLimit = length;
	}

	@Override
	public Widget asWidget() {
		return this;
	}
	
	/* 
	 * Private Methods 
	 */
	private void updateToolBar() {		
		// reconnects and refreshes the paging toolbar
		if(toolBar != null) {
			toolBar.setPageSize(paginationLimit);
			toolBar.bind(loader);
	        toolBar.recalculate();
	        toolBar.layout();
		}
	}

	@Override
	public void setTitle(String title) {
		this.tableTitle = title;		
	}
	
}
