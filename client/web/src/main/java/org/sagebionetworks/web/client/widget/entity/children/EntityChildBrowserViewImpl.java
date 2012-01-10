package org.sagebionetworks.web.client.widget.entity.children;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.widget.statictable.StaticTable;
import org.sagebionetworks.web.client.widget.statictable.StaticTableColumn;
import org.sagebionetworks.web.client.widget.table.QueryTableFactory;
import org.sagebionetworks.web.shared.EntityType;
import org.sagebionetworks.web.shared.WhereCondition;

import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Html;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.TabItem;
import com.extjs.gxt.ui.client.widget.TabPanel;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.MarginData;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class EntityChildBrowserViewImpl extends LayoutContainer implements
		EntityChildBrowserView {
	
	private static final int PANEL_HEIGHT_PX = 270;
	private static final int SLIDER_HEIGHT_PX = 25;
	private Presenter presenter;
	private SageImageBundle sageImageBundle;
	private IconsImageBundle iconsImageBundle;
	private TabPanel tabPanel;
	private QueryTableFactory queryTableFactory;
	private StaticTable staticTable;
	private TabItem previewTab;
	private ContentPanel previewLoading;
	private boolean addStaticTable;

	@Inject
	public EntityChildBrowserViewImpl(SageImageBundle sageImageBundle,
			IconsImageBundle iconsImageBundle, QueryTableFactory queryTableFactory, StaticTable staticTable) {
		this.sageImageBundle = sageImageBundle;
		this.iconsImageBundle = iconsImageBundle;
		this.queryTableFactory = queryTableFactory;
		this.staticTable = staticTable;
		staticTable.setHeight(PANEL_HEIGHT_PX-SLIDER_HEIGHT_PX); // sub slider height
		staticTable.setShowTitleBar(false);
		addStaticTable = true;
		
		this.setLayout(new FitLayout());
	}

	@Override
	public void createBrowser(final Entity entity, EntityType entityType,
			boolean canEdit) {
		if(tabPanel != null) {
			// out with the old
			this.remove(tabPanel);
			tabPanel.removeAll();
		}		
		tabPanel = new TabPanel();
		tabPanel.setLayoutData(new FitLayout());
		tabPanel.setPlain(true);
		tabPanel.setHeight(PANEL_HEIGHT_PX);
		tabPanel.setAutoWidth(true);
		
		List<EntityType> skipTypes = presenter.getContentsSkipTypes();		
		for(final EntityType child : entityType.getValidChildTypes()) {
			if(skipTypes.contains(child)) continue; // skip some types
				
			final String childDisplay = DisplayUtils.uppercaseFirstLetter(child.getName());
			final TabItem tab = new TabItem(childDisplay);			
			tab.addStyleName("pad-text");			
			final ContentPanel loading = DisplayUtils.getLoadingWidget(sageImageBundle);
			loading.setHeight(PANEL_HEIGHT_PX);		
			tab.add(loading);
			tab.addListener(Events.Render, new Listener<BaseEvent>() {
				@Override
				public void handleEvent(BaseEvent be) {
					if("preview".equals(child.getName())) {									
						// let presenter create preview table when data is loaded
						previewTab = tab;
						previewLoading = loading;
					} else {
						// loading is embedded into the query table widget
						addQueryTable(child, tab, loading);
					}
				}
			});
			tabPanel.add(tab);			
		}

		add(tabPanel);
	}

	@Override
	public Widget asWidget() {
		return this;
	}

	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void showErrorMessage(String message) {
		DisplayUtils.showErrorMessage(message);
	}

	@Override
	public void showLoading() {
	}

	@Override
	public void showInfo(String title, String message) {
		DisplayUtils.showInfo(title, message);
	}

	@Override
	public void clear() {
	}

	@Override
	public void setPreviewTable(PreviewData previewData) {
		if(previewData == null) previewData = new PreviewData();
		// add static table view of preview		
		List<Map<String, String>> rows = previewData.getRows();
		Map<String, String> columnUnits = previewData.getColumnUnits();
		Map<String, String> columnDescriptions = previewData
				.getColumnDescriptions();
		List<String> columnDisplayOrder = previewData.getColumnDisplayOrder();
		
		if(previewTab == null)
			return;		
		if(previewLoading != null)
			previewTab.remove(previewLoading);

		if (rows != null && rows.size() > 0 && columnDescriptions != null
				&& columnUnits != null && columnDisplayOrder != null) {
			// create static table columns
			List<StaticTableColumn> stColumns = new ArrayList<StaticTableColumn>();
			for (String key : previewData.getColumnDisplayOrder()) {
				StaticTableColumn stCol = new StaticTableColumn();
				stCol.setId(key);
				stCol.setName(key);

				// add units to column if available
				if (columnUnits.containsKey(key)) {
					stCol.setUnits(columnUnits.get(key));
				}

				// add description if available
				if (columnDescriptions.containsKey(key)) {
					stCol.setTooltip(columnDescriptions.get(key));
				}

				stColumns.add(stCol);

				staticTable.setDataAndColumnsInOrder(previewData.getRows(),
						stColumns);
				if(addStaticTable) {
					previewTab.add(staticTable.asWidget());
					addStaticTable = false;
				}
			}
		} else {
			previewTab.remove(staticTable.asWidget());
			addStaticTable = true;
			// no data in preview
			previewTab.add(new Html(DisplayConstants.LABEL_NO_PREVIEW_DATA),
					new MarginData(10));
			previewTab.layout(true);
		}
		
		previewTab.layout(true);
	}
	
	/*
	 * Private Methods
	 */
	private void addQueryTable(final EntityType child,
			final TabItem tab, final ContentPanel loading) {
		// add query table
		queryTableFactory.createColumnModel(child, presenter.getPlaceChanger(), new AsyncCallback<ColumnModel>() {
			@Override
			public void onSuccess(ColumnModel cm) {					   		        
				// limit view to just this entity's children
				final List<WhereCondition> where = presenter.getProjectContentsWhereContidions();
				ContentPanel cp = queryTableFactory.createGridPanel(child, cm, where, presenter.getPlaceChanger(), null);
		        if(cp != null && cp.getElement() != null) {
		        	cp.setHeight(PANEL_HEIGHT_PX-25);
		        	cp.setHeaderVisible(false);
					tab.remove(loading);
					tab.add(cp);
					tab.layout(true);
		        } else {
		        	onFailure(null);
		        }
			}
			
			@Override
			public void onFailure(Throwable caught) {
				tab.remove(loading);
				tab.add(new Html(DisplayConstants.ERROR_GENERIC_RELOAD), new MarginData(10));
				tab.layout(true);
			}
		});
	}

}

