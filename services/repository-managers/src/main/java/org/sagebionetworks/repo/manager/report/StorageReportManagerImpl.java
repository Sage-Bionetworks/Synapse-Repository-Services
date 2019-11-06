package org.sagebionetworks.repo.manager.report;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.report.DownloadStorageReportRequest;
import org.sagebionetworks.repo.model.report.StorageReportType;
import org.sagebionetworks.repo.model.report.SynapseStorageProjectStats;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.util.Callback;
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
			throws NotFoundException, LockUnavilableException {
		// Verify that the user is in the authorized group.
		UserInfo.validateUserInfo(user);
		if (!authorizationManager.isReportTeamMemberOrAdmin(user)) {
			throw new UnauthorizedException("Only administrators or members of the Synapse Report Team can generate storage reports.");
		}

		TableIndexDAO tableIndexDAO = connectionFactory.getFirstConnection();

		if (request.getReportType() == null) {
			request.setReportType(StorageReportType.ALL_PROJECTS);
		}

		switch (request.getReportType()) {
			case ALL_PROJECTS:
				String[] header = {"projectId", "projectName", "sizeInBytes"};
				writer.writeNext(header);

				Callback<SynapseStorageProjectStats> callback = value -> {
					String[] row = new String[3];
					row[0] = "syn" + value.getId();
					row[1] = value.getProjectName();
					row[2] = value.getSizeInBytes().toString();
					writer.writeNext(row);
				};

				tableIndexDAO.streamSynapseStorageStats(callback);
				break;
			default:
				throw new IllegalArgumentException("Only storage reports of type \"ALL_PROJECTS\" are currently supported.");
		}
	}
}
