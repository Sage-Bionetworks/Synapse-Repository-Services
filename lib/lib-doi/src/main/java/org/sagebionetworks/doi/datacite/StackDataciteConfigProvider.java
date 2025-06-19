package org.sagebionetworks.doi.datacite;

import org.sagebionetworks.StackConfiguration;
import org.springframework.stereotype.Component;

/*
 * Use to configure parameters in the DataCite client
 */
@Component
public class StackDataciteConfigProvider implements DataciteClientConfig {

	private StackConfiguration stackConfiguration;
	
	public StackDataciteConfigProvider(StackConfiguration stackConfiguration) {
		this.stackConfiguration = stackConfiguration;
	}

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
