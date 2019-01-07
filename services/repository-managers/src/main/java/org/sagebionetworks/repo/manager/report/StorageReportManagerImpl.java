package org.sagebionetworks.repo.manager.report;

import java.util.List;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.SynapseStorageProjectStats;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.report.DownloadStorageReportRequest;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.util.csv.CSVWriterStream;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;

public class StorageReportManagerImpl implements StorageReportManager {

	@Autowired
	ConnectionFactory connectionFactory;
	@Autowired
	AuthorizationManager authorizationManager;

	@Override
	public void writeStorageReport(ProgressCallback progressCallback, UserInfo user,
								   DownloadStorageReportRequest request, CSVWriterStream writer)
			throws NotFoundException, LockUnavilableException{
		// Verify that the user is in the authorized group.
		UserInfo.validateUserInfo(user);
		if (!authorizationManager.isStorageReportTeamMemberOrAdmin(user)) {
			throw new UnauthorizedException("Only administrators and members of the Synapse Report Team can generate storage reports");
		}

		TableIndexDAO tableIndexDAO = connectionFactory.getFirstConnection();
		// Query the table
		List<SynapseStorageProjectStats> projectStatsList = tableIndexDAO.getSynapseStorageStats();
		// Create a CSV
		String[] header = {"projectId", "projectName", "sizeInBytes"};


		writer.writeNext(header);

		for (SynapseStorageProjectStats rowData : projectStatsList) {
			String[] row = new String[3];
			row[0] = rowData.getId();
			row[1] = rowData.getProjectName();
			row[2] = rowData.getSize().toString();
			writer.writeNext(row);
		}
	}
}
