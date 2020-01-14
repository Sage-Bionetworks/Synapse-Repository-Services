package org.sagebionetworks.trash.worker;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.worker.utils.StackStatusGate;
import org.sagebionetworks.workers.util.Gate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Special {@link Gate} implementation that checks if the purging of the trashcan is enabled
 * 
 * @author Marco
 *
 */
public class TrashStatusGate extends StackStatusGate {

	@Autowired
	private StackConfiguration stackConfiguration;
	
	@Override
	public boolean canRun() {
		return super.canRun() && stackConfiguration.getTrashCanPurgeEnabled();
	}
}
