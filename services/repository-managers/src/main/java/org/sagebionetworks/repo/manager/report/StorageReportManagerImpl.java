package org.sagebionetworks.repo.manager.report;

import java.util.List;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.SynapseStorageProjectStats;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.report.DownloadStorageReportRequest;
import org.sagebionetworks.repo.model.report.StorageReportType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.util.csv.CSVWriterStream;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;

public class StorageReportManagerImpl implements StorageReportManager {

	@Autowired
	private ConnectionFactory connectionFactory;
	@Autowired
	private AuthorizationManager authorizationManager;

	@Override
	public void writeStorageReport(UserInfo user, DownloadStorageReportRequest request, CSVWriterStream writer)
			throws NotFoundException, LockUnavilableException{
		// Verify that the user is in the authorized group.
		UserInfo.validateUserInfo(user);
		if (!authorizationManager.isReportTeamMemberOrAdmin(user)) {
			throw new UnauthorizedException("Only administrators and members of the Synapse Report Team can generate storage reports");
		}

		TableIndexDAO tableIndexDAO = connectionFactory.getFirstConnection();

		if (request.getReportType() == null || request.getReportType().equals(StorageReportType.ALL_PROJECTS)) {
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
		} else {
			throw new IllegalArgumentException("Only storage reports of type \"ALL_PROJECTS\" are currently supported.");
		}
	}
}
