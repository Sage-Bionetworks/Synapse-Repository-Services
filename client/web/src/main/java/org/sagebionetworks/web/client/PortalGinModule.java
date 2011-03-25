package org.sagebionetworks.web.client;

import org.sagebionetworks.web.client.cookie.CookieProvider;
import org.sagebionetworks.web.client.cookie.GWTCookieImpl;
import org.sagebionetworks.web.client.view.CellTableProvider;
import org.sagebionetworks.web.client.view.CellTableProviderImpl;
import org.sagebionetworks.web.client.view.ColumnsPopupView;
import org.sagebionetworks.web.client.view.ColumnsPopupViewImpl;
import org.sagebionetworks.web.client.view.DatasetView;
import org.sagebionetworks.web.client.view.DatasetViewImpl;
import org.sagebionetworks.web.client.view.DatasetsHomeView;
import org.sagebionetworks.web.client.view.DatasetsHomeViewImpl;
import org.sagebionetworks.web.client.view.LayerView;
import org.sagebionetworks.web.client.view.LayerViewImpl;
import org.sagebionetworks.web.client.view.table.ColumnFactory;
import org.sagebionetworks.web.client.view.table.ColumnFactoryImpl;
import org.sagebionetworks.web.client.widget.filter.QueryFilterView;
import org.sagebionetworks.web.client.widget.filter.QueryFilterViewImpl;
import org.sagebionetworks.web.client.widget.licenseddownloader.LicensedDownloaderView;
import org.sagebionetworks.web.client.widget.licenseddownloader.LicensedDownloaderViewImpl;
import org.sagebionetworks.web.client.widget.statictable.StaticTableView;
import org.sagebionetworks.web.client.widget.statictable.StaticTableViewImpl;
import org.sagebionetworks.web.client.widget.table.QueryServiceTableView;
import org.sagebionetworks.web.client.widget.table.QueryServiceTableViewGxtImpl;
import org.sagebionetworks.web.client.widget.table.QueryServiceTableViewImpl;
import com.google.gwt.cell.client.widget.CustomWidgetImageBundle;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.Singleton;

public class PortalGinModule extends AbstractGinModule {

	@Override
	protected void configure() {
		
		// The home page for all datasets
		bind(DatasetsHomeViewImpl.class).in(Singleton.class);
		bind(DatasetsHomeView.class).to(DatasetsHomeViewImpl.class);
		
		// DatasetView
		bind(DatasetViewImpl.class).in(Singleton.class);
		bind(DatasetView.class).to(DatasetViewImpl.class);
		
		// LayerView
		bind(LayerViewImpl.class).in(Singleton.class);
		bind(LayerView.class).to(LayerViewImpl.class);

		// QueryService View
		//bind(QueryServiceTableView.class).to(QueryServiceTableViewImpl.class);
		bind(QueryServiceTableView.class).to(QueryServiceTableViewGxtImpl.class);
		
		// StaticTable
		bind(StaticTableView.class).to(StaticTableViewImpl.class);
		
		// LicenseBox
		bind(LicensedDownloaderView.class).to(LicensedDownloaderViewImpl.class);
		
		// Bind the cookie provider
		bind(GWTCookieImpl.class).in(Singleton.class);
		bind(CookieProvider.class).to(GWTCookieImpl.class);

		// ColumnFactory
		bind(ColumnFactory.class).to(ColumnFactoryImpl.class);
		
		// The ImagePrototySingleton should be...well a singleton
		bind(ImagePrototypeSingleton.class).in(Singleton.class);
		
		// ClientBundle for Custom widgets
		bind(CustomWidgetImageBundle.class).in(Singleton.class);
		
		// The runtime provider
		bind(CellTableProvider.class).to(CellTableProviderImpl.class);
		
		// The column popup
		bind(ColumnsPopupViewImpl.class).in(Singleton.class);
		bind(ColumnsPopupView.class).to(ColumnsPopupViewImpl.class);
		
		// Query filter
		bind(QueryFilterViewImpl.class).in(Singleton.class);
		bind(QueryFilterView.class).to(QueryFilterViewImpl.class);

	}

}
