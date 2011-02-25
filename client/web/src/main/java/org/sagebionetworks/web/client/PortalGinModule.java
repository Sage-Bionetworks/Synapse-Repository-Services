package org.sagebionetworks.web.client;

import org.sagebionetworks.web.client.cookie.CookieProvider;
import org.sagebionetworks.web.client.cookie.GWTCookieImpl;
import org.sagebionetworks.web.client.view.CellTableProvider;
import org.sagebionetworks.web.client.view.CellTableProviderImpl;
import org.sagebionetworks.web.client.view.DatasetView;
import org.sagebionetworks.web.client.view.DatasetViewImpl;
import org.sagebionetworks.web.client.view.DatasetsHomeView;
import org.sagebionetworks.web.client.view.DatasetsHomeViewImpl;
import org.sagebionetworks.web.client.view.DynamicTableView;
import org.sagebionetworks.web.client.view.DynamicTableViewImpl;
import org.sagebionetworks.web.client.view.table.ColumnFactory;
import org.sagebionetworks.web.client.view.table.ColumnFactoryImpl;

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
		// For now the dynamic table is a singleton. If we need
		// to have more than one on a page at a time then we will need
		// to change this.
		bind(DynamicTableViewImpl.class).in(Singleton.class);
		bind(DynamicTableView.class).to(DynamicTableViewImpl.class);
		
		// Bind the cookie provider
		bind(GWTCookieImpl.class).in(Singleton.class);
		bind(CookieProvider.class).to(GWTCookieImpl.class);

		// ColumnFactory
		bind(ColumnFactory.class).to(ColumnFactoryImpl.class);
		
		// The ImagePrototySingleton should be...well a singleton
		bind(ImagePrototypeSingleton.class).in(Singleton.class);
		
		// The runtime provider
		bind(CellTableProvider.class).to(CellTableProviderImpl.class);
	}

}
