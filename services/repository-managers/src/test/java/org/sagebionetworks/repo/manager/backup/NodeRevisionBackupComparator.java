package org.sagebionetworks.repo.manager.backup;

import java.util.Comparator;

import org.sagebionetworks.repo.model.NodeRevisionBackup;

public class NodeRevisionBackupComparator implements Comparator<NodeRevisionBackup> {

	@Override
	public int compare(NodeRevisionBackup one, NodeRevisionBackup two) {
		// TODO Auto-generated method stub
		return one.getRevisionNumber().compareTo(two.getRevisionNumber());
	}

}
