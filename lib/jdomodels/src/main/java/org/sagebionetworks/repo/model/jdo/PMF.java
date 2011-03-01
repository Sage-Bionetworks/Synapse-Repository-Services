package org.sagebionetworks.repo.model.jdo;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

/**
 * A factory for persistence managers.
 * 
 * @author bhoff
 * 
 */
public final class PMF {
	private static final PersistenceManagerFactory pmfInstance = JDOHelper
			.getPersistenceManagerFactory("transactions-optional");

	private PMF() {
	}

	public static PersistenceManager get() {
		return pmfInstance.getPersistenceManager();
	}
}