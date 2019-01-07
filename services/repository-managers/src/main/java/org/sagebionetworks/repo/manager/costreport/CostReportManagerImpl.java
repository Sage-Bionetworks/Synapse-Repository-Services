package org.sagebionetworks.repo.manager.costreport;

import java.util.List;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.SynapseStorageProjectStats;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.costreport.DownloadCostReportRequest;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.util.csv.CSVWriterStream;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;

public class CostReportManagerImpl implements CostReportManager {

	@Autowired
	ConnectionFactory connectionFactory;

	@Override
	public void writeCostReport(ProgressCallback progressCallback, UserInfo user,
													DownloadCostReportRequest request, CSVWriterStream writer)
			throws NotFoundException, LockUnavilableException{
		// Verify that the user is in the authorized group.

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
