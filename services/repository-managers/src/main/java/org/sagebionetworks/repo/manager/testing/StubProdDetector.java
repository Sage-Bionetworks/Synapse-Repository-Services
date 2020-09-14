package org.sagebionetworks.repo.manager.testing;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.stack.ProdDetector;

public class StubProdDetector implements ProdDetector {

	private static final Logger LOG = LogManager.getLogger(StubProdDetector.class);
	
	private final boolean isProd;

	StubProdDetector(boolean isProd) {
		this.isProd = isProd;
	}
	
	@Override
	public Optional<Boolean> isProductionStack() {
		LOG.info("Prod detection invoked (isProd: {})", isProd);
		return Optional.of(isProd);
	}

}
