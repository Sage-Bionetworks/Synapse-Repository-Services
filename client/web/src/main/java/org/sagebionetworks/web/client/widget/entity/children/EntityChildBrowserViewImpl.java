package org.sagebionetworks.web.client.widget.entity.children;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
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
	private Presenter presenter;
	private SageImageBundle sageImageBundle;
	private IconsImageBundle iconsImageBundle;
	private TabPanel tabPanel;
	private QueryTableFactory queryTableFactory;

	@Inject
	public EntityChildBrowserViewImpl(SageImageBundle sageImageBundle,
			IconsImageBundle iconsImageBundle, QueryTableFactory queryTableFactory) {
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
		tabPanel.setHeight(PANEL_HEIGHT_PX);
		tabPanel.setAutoWidth(true);
		
		List<EntityType> skipTypes = presenter.getProjectContentsSkipTypes();		
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

	/*
	 * Private Methods
	 */
}
