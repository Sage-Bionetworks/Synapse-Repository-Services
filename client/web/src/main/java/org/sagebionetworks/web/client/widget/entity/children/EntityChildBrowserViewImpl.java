package org.sagebionetworks.web.client.widget.entity.children;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.widget.statictable.StaticTable;
import org.sagebionetworks.web.client.widget.statictable.StaticTableColumn;
import org.sagebionetworks.web.client.widget.statictable.StaticTableView;
import org.sagebionetworks.web.client.widget.statictable.StaticTableViewImpl;
import org.sagebionetworks.web.client.widget.table.QueryTableFactory;
import org.sagebionetworks.web.shared.EntityType;
import org.sagebionetworks.web.shared.FileDownload;
import org.sagebionetworks.web.shared.WhereCondition;

import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Html;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.TabItem;
import com.extjs.gxt.ui.client.widget.TabPanel;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.MarginData;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class EntityChildBrowserViewImpl extends LayoutContainer implements
		EntityChildBrowserView {
	
	private final static int MAX_IMAGE_PREVIEW_WIDTH_PX = 800;
	private static final int BASE_PANEL_HEIGHT_PX = 270;
	private static final int TALL_PANEL_HEIGHT_PX = 500;
	private static final int SLIDER_HEIGHT_PX = 25;
	private Presenter presenter;
	private SageImageBundle sageImageBundle;
	private IconsImageBundle iconsImageBundle;
	private TabPanel tabPanel;
	private QueryTableFactory queryTableFactory;	
	private TabItem previewTab;
	private ContentPanel previewLoading;
	private int tabPanelHeight;
	private boolean addStaticTable;
	private boolean imagePreviewExpanded;
	private Integer originalImageWidth;
	private Integer originalImageHeight;

	@Inject
	public EntityChildBrowserViewImpl(SageImageBundle sageImageBundle,
			IconsImageBundle iconsImageBundle, QueryTableFactory queryTableFactory, StaticTable staticTable) {
		this.sageImageBundle = sageImageBundle;
		this.iconsImageBundle = iconsImageBundle;
		this.queryTableFactory = queryTableFactory;		
		
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
		tabPanel.setAutoWidth(true);		
		// determine tabPanel height
		final LocationData location = presenter.getMediaLocationData();
		if(location != null && location.getPath() != null) {
			tabPanelHeight = TALL_PANEL_HEIGHT_PX;
		} else {
			tabPanelHeight = BASE_PANEL_HEIGHT_PX;
		}
		tabPanel.setHeight(tabPanelHeight);
				
		List<EntityType> skipTypes = presenter.getContentsSkipTypes();		
		for(final EntityType child : entityType.getValidChildTypes()) {
			if(skipTypes.contains(child)) continue; // skip some types
				
			final String childDisplay = DisplayUtils.uppercaseFirstLetter(child.getName());
			final TabItem tab = new TabItem(childDisplay);			
			tab.addStyleName("pad-text");			
			final ContentPanel loading = DisplayUtils.getLoadingWidget(sageImageBundle);
			loading.setHeight(tabPanelHeight);		
			tab.add(loading);
			tab.addListener(Events.Render, new Listener<BaseEvent>() {
				@Override
				public void handleEvent(BaseEvent be) {
					if("preview".equals(child.getName())) {									
						// let presenter create preview info when data is loaded
						previewTab = tab;
						previewLoading = loading;
						
						// Synchronous previews						
						if(location != null && location.getPath() != null) {
							setMediaPreview(location);
						}
						
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
		
		previewTab.removeAll();
		if (rows != null && rows.size() > 0 && columnDescriptions != null
				&& columnUnits != null && columnDisplayOrder != null) {
			// create static table columns
			StaticTableView view = new StaticTableViewImpl();
			StaticTable staticTable = new StaticTable(view);
			staticTable.setHeight(tabPanelHeight-SLIDER_HEIGHT_PX); // sub slider height
			staticTable.setShowTitleBar(false);

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
				previewTab.add(staticTable.asWidget());
			}
		} else {			
			setNoPreview();
		}
		
		previewTab.layout(true);
	}
		
	private void setMediaPreview(LocationData locationData) {
		if(previewTab == null)
			return;		
		
		previewTab.removeAll();
		if(locationData != null) {
			// handle image preview
			final ContentPanel cp = new ContentPanel();						
			cp.setHeaderVisible(false);			
			cp.setAutoWidth(true);
			cp.setHeight(tabPanelHeight - SLIDER_HEIGHT_PX);			
			cp.setScrollMode(Scroll.ALWAYS);
			
			final Image previewImageWidget = new Image(locationData.getPath());
			imagePreviewExpanded = false; // start collapsed
			
			final Button expandImagePreviewButton = new Button("View Full Size", AbstractImagePrototype.create(iconsImageBundle.magnifyZoomIn16()));
			expandImagePreviewButton.addSelectionListener(new SelectionListener<ButtonEvent>() {
				@Override
				public void componentSelected(ButtonEvent ce) {
					if(imagePreviewExpanded) {
						previewImageWidget.setWidth(MAX_IMAGE_PREVIEW_WIDTH_PX + "px");							
						previewImageWidget.setHeight(originalImageHeight * MAX_IMAGE_PREVIEW_WIDTH_PX / originalImageWidth + "px");
						imagePreviewExpanded = false;
						expandImagePreviewButton.setText("View Full Size");
						expandImagePreviewButton.setIcon(AbstractImagePrototype.create(iconsImageBundle.magnifyZoomIn16()));
					} else {
						previewImageWidget.setHeight(originalImageHeight + "px");
						previewImageWidget.setWidth(originalImageWidth + "px");
						imagePreviewExpanded = true;
						expandImagePreviewButton.setText("Zoom Out");
						expandImagePreviewButton.setIcon(AbstractImagePrototype.create(iconsImageBundle.magnifyZoomOut16()));
					}
				}
			});

			final ToolBar toolbar = new ToolBar();

			// scale image if it needs it
			previewImageWidget.addLoadHandler(new LoadHandler() {					
				@Override
				public void onLoad(LoadEvent event) {
					if(originalImageHeight == null && originalImageWidth == null) {							
						originalImageHeight = previewImageWidget.getHeight();
						originalImageWidth = previewImageWidget.getWidth();
					}						
					if(originalImageHeight > MAX_IMAGE_PREVIEW_WIDTH_PX) {
						// scale image
						previewImageWidget.setWidth(MAX_IMAGE_PREVIEW_WIDTH_PX + "px");							
						previewImageWidget.setHeight(originalImageHeight * MAX_IMAGE_PREVIEW_WIDTH_PX / originalImageWidth + "px");
						imagePreviewExpanded = false;
						toolbar.add(expandImagePreviewButton);							
					}
				}
			});

			
			cp.setTopComponent(toolbar);
			cp.add(previewImageWidget);
			previewTab.add(cp);
			cp.layout(true);
		} else {
			setNoPreview();
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
		        	cp.setHeight(tabPanelHeight-25);
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

	private void setNoPreview() {
		// no data in preview
		previewTab.add(new Html(DisplayConstants.LABEL_NO_PREVIEW_DATA),
				new MarginData(10));
		previewTab.layout(true);
	}

}

