package org.sagebionetworks.web.client.view.table;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;

@GinModules(ColumnFactoryGinModule.class)
public interface ColumnFactoryGinInjector extends Ginjector {
	
	public LinkColumn getLinkColumn();
	
	public LayerColumn getLayerColumn();

}
