package org.sagebionetworks.web.client.view.table;

import com.google.gwt.inject.client.AbstractGinModule;

public class ColumnFactoryGinModule extends AbstractGinModule {

	@Override
	protected void configure() {
		// Bind the link column to itself
//		bind(LinkColumn.class).to(LinkColumn.class);
	}

}
