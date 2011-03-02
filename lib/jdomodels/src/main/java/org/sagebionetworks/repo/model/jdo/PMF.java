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
	private static PersistenceManagerFactory pmfInstance = null;
	private static String persistenceImpl = "memorydb-transactions-optional";

	private PMF() {
	}

	public static PersistenceManager get() {
		if (null == pmfInstance) {
			synchronized (PMF.class) {
				pmfInstance = JDOHelper
						.getPersistenceManagerFactory(persistenceImpl);
			}
		}
		return pmfInstance.getPersistenceManager();
	}
}