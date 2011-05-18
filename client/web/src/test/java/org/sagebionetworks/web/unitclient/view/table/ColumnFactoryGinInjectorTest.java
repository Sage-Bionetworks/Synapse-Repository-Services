package org.sagebionetworks.web.unitclient.view.table;

import static org.junit.Assert.assertNotNull;

import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.web.client.view.table.ColumnFactoryGinInjector;

import com.google.inject.Inject;

@RunWith(JukitoRunner.class)
public class ColumnFactoryGinInjectorTest {
	
	// The implementation should be auto-injected
	@Inject
	ColumnFactoryGinInjector injector;
	
	@Test
	public void testInjector(){
		// Still trying to figure out how to test these objects
		// First make sure the injector was injected
		assertNotNull(injector);
		// Make sure we can get an instance of the link column
//		LinkColumn linkColumn = injector.getLinkColumn();
//		assertNotNull(linkColumn);
		
	}

}
