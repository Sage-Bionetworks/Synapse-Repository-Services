package org.sagebionetworks.web.client;

import org.sagebionetworks.web.client.presenter.DatasetPresenter;
import org.sagebionetworks.web.client.presenter.DatasetsHomePresenter;
import org.sagebionetworks.web.client.presenter.DynamicTablePresenter;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;

/**
 * The root portal dependency injection root.
 * 
 * @author jmhill
 *
 */
@GinModules(PortalGinModule.class)
public interface ProtalGinInjector extends Ginjector {
	
	public DatasetsHomePresenter getDatasetsHomePresenter();
	
	public DatasetPresenter getDatasetPresenter();
	
	public DynamicTablePresenter getDynamicTableTest();
	

}
