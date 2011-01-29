package org.sagebionetworks.web.client;

import org.sagebionetworks.web.client.view.AllDatasetsView;
import org.sagebionetworks.web.client.view.AllDatasetsViewImpl;
import org.sagebionetworks.web.client.view.DatasetView;
import org.sagebionetworks.web.client.view.DatasetViewImpl;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.Singleton;

public class PortalGinModule extends AbstractGinModule {

	@Override
	protected void configure() {
		// We want the view implementation to be a singleton.
		bind(AllDatasetsViewImpl.class).in(Singleton.class);
		bind(DatasetViewImpl.class).in(Singleton.class);
		bind(AllDatasetsView.class).to(AllDatasetsViewImpl.class);
		bind(DatasetView.class).to(DatasetViewImpl.class);
	}

}
