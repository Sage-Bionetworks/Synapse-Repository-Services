package org.sagebionetworks.repo.web;

import org.junit.Ignore;
import org.junit.Test;

// this is an integration test, to be enabled in that suite
@Ignore
public class PersistenceInitializerTest {

	
		@Test
		public void testAll() throws Exception {
			PersistenceInitializer pi = new PersistenceInitializer();
			pi.contextInitialized("org.sagebionetworks.repo.model.jdo.JDODAOFactoryImpl", 
					"org.sagebionetworks.repo.model.jdo.JDOBootstrapperImpl", 
					true);
		}
}
