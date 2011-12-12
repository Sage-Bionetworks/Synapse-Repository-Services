package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;

/**
 * Helpers for BackupRestoreStatus.
 * @author John
 *
 */
public class DaemonStatusUtil {

	/**
	 * Pretty print status.
	 * @param status
	 * @return
	 */
	public static String printStatus(BackupRestoreStatus status){
		double percent = 0.0;
		if(status.getProgresssTotal() > 0){
			percent = ((double)status.getProgresssCurrent()/(double)status.getProgresssTotal())*100.0;
		}
		return 	String.format("%5$-10s %2$10d/%3$-10d %4$8.2f %% Message: %1$-30s", status.getProgresssMessage(), status.getProgresssCurrent(), status.getProgresssTotal(), percent, status);

	}
}
