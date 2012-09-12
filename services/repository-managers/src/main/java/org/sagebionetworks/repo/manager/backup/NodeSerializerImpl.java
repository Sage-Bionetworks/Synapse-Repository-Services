package org.sagebionetworks.repo.manager.backup;

import java.io.InputStream;

import org.sagebionetworks.repo.model.NodeBackup;

/**
 * This class only exist so we can profile this operation using Spring's AoP.
 * @author jmhill
 *
 */
public class NodeSerializerImpl implements NodeSerializer {

	@Override
	public NodeBackup readNodeBackup(InputStream in) {
		// This utility does the real work.
		return NodeSerializerUtil.readNodeBackup(in);
	}

}
