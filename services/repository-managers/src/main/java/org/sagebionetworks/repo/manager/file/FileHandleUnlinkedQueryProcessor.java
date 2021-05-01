package org.sagebionetworks.repo.manager.file;

import java.util.List;

import org.sagebionetworks.repo.manager.athena.RecurrentAthenaQueryProcessor;
import org.sagebionetworks.repo.model.athena.RowMapper;
import org.sagebionetworks.repo.model.dao.FileHandleStatus;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.athena.model.Row;

/**
 * Processor for the recurrent query that computes the unlinked file handles
 */
@Service
public class FileHandleUnlinkedQueryProcessor implements RecurrentAthenaQueryProcessor<Long> {
	
	static RowMapper<Long> ROW_MAPPER = (Row row) -> {
		return Long.valueOf(row.getData().get(0).getVarCharValue());
	};

	private FileHandleDao fileHandleDao;
	
	@Autowired
	public FileHandleUnlinkedQueryProcessor(FileHandleDao fileHandleDao) {
		this.fileHandleDao = fileHandleDao;
	}

	@Override
	public String getQueryName() {
		return "UnlinkedFileHandles";
	}

	@Override
	public RowMapper<Long> getRowMapper() {
		return ROW_MAPPER;
	}

	@Override
	public void processQueryResultsPage(List<Long> resultsPage) {
		fileHandleDao.updateBatchStatus(resultsPage, FileHandleStatus.UNLINKED, FileHandleStatus.AVAILABLE);
	}

}
