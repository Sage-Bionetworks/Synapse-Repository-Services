package org.sagebionetworks.doi.datacite;

import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

/*
 * Use to configure parameters in the DataCite client
 */
public class StackDataciteConfigProvider implements DataciteClientConfig {

	@Autowired
	StackConfiguration stackConfiguration;

	public String getUsername(){
		return stackConfiguration.getDataciteUsername();
	}

	public String getPassword(){
		return stackConfiguration.getDatacitePassword();
	}

	public String getDataciteDomain(){
		return stackConfiguration.getDataciteAPIEndpoint();
	}
}
