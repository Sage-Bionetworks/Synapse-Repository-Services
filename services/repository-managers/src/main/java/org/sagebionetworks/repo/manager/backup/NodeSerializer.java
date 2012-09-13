package org.sagebionetworks.repo.manager.backup;

import java.io.InputStream;

import org.sagebionetworks.repo.model.NodeBackup;

/**
 * This interface only exist so we can profile this operation using Spring's AoP.
 * @author jmhill
 *
 */
public interface NodeSerializer {
	
	public NodeBackup readNodeBackup(InputStream in);

}
