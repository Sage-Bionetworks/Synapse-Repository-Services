package org.sagebionetworks.schema.worker;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;
import org.sagebionetworks.repo.manager.schema.SynapseSchemaBootstrap;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A periodic singleton worker that ensures the Synapse schema objects are
 * translated and registered and available for the JSON Schema services
 *
 */
public class SynapseSchemaBootstrapWorker implements ProgressingRunner {
	
	private final SynapseSchemaBootstrap bootstrap;
	
	
	@Autowired
	public SynapseSchemaBootstrapWorker(SynapseSchemaBootstrap bootstrap) {
		this.bootstrap = bootstrap;
	}

	@Override
	public void run(ProgressCallback progressCallback) throws Exception {
		bootstrap.bootstrapSynapseSchemas();
	}

}
