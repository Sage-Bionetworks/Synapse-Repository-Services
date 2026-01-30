package org.sagebionetworks.repo.manager.grid.internal.replica.export;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.LocalFileUploadRequest;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.GridReplicaSupport;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.dbo.schema.EntitySchemaValidationResultDao;
import org.sagebionetworks.repo.model.grid.DownloadFromGridRequest;
import org.sagebionetworks.repo.model.grid.DownloadFromGridResult;
import org.sagebionetworks.repo.model.grid.GridRecordSetExportRequest;
import org.sagebionetworks.repo.model.grid.GridRecordSetExportResponse;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.model.schema.ValidationSummaryStatistics;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.util.csv.CSVWriterProvider;
import org.springframework.stereotype.Service;

import au.com.bytecode.opencsv.CSVWriter;

@Service
public class GridRecordSetExporterImpl implements GridRecordSetExporter {
	
	private static final String[] VALIDATION_CSV_HEADERS = new String[] {
		"row_index", "is_valid", "validation_error_message", "all_validation_messages"
	};

	private final GridManager gridManager;
	private final GridReplicaSupport gridReplicaSupport;
	private final EntityManager entityManager;
	private final GridReplicaCsvExporter csvExporter;
	private final EntitySchemaValidationResultDao validationResultDao;
	private final CSVWriterProvider csvWriterProvider;
    private final FileHandleManager fileHandleManager;
	
	public GridRecordSetExporterImpl(GridManager gridManager, GridReplicaSupport gridReplicaSupport, EntityManager entityManager, GridReplicaCsvExporter csvExporter, EntitySchemaValidationResultDao validationResultDao, CSVWriterProvider csvWriterProvider, FileHandleManager fileHandleManager) {
		this.gridManager = gridManager;
		this.gridReplicaSupport = gridReplicaSupport;
		this.entityManager = entityManager;
		this.csvExporter = csvExporter;
		this.validationResultDao = validationResultDao;
		this.csvWriterProvider = csvWriterProvider;
		this.fileHandleManager = fileHandleManager;
	}
	
	@Override
	@WriteTransaction
	public GridRecordSetExportResponse exportGrid(UserInfo user, GridRecordSetExportRequest request, AsyncJobProgressCallback jobCallback) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getSessionId(), "request.sessionId");
		
		GridSession gridSession = gridManager.getGridSession(user, request.getSessionId());
		
		RecordSet recordSet = gridReplicaSupport.getRecordSetOrThrow(user, gridSession);
		
		String exportedFileId;
		File tmpValidationFile = null;
		ValidationSummaryStatistics validationSummary;
		String validationFileId;
		
		try {
			
			tmpValidationFile = File.createTempFile(jobCallback.getJobId() + "_validation_details", ".csv");
			
			try (CSVWriter validationCsvWriter = csvWriterProvider.createWriter(new FileWriter(tmpValidationFile, StandardCharsets.UTF_8), null)) {
				ValidationSummaryBuilder validationSummaryBuilder = new ValidationSummaryBuilder(recordSet.getId(), validationCsvWriter);
				
				// First export to a CSV file
				exportedFileId = exportToCsv(user, gridSession.getSessionId(), recordSet.getCsvDescriptor(), jobCallback, validationSummaryBuilder);
				
				validationSummary = validationSummaryBuilder.getValidationSummary();
			}
			
			// Uploads the validation details as a file handle
			validationFileId = fileHandleManager.uploadLocalFile(new LocalFileUploadRequest()
				.withUserId(user.getId().toString())
				.withFileName("grid_validation_details.csv")
				.withContentType("text/csv")
				.withFileToUpload(tmpValidationFile)					
			).getId();
		
		} catch (IOException e) {
			throw new IllegalStateException("Could not export the grid to a new record set version.", e);
		} finally {
			if (tmpValidationFile != null) {
				tmpValidationFile.delete();
			}
		}
		
		// Creates a new version of the record set that points to the new file and persist the validation summary
		recordSet = createNewVersion(user, recordSet, exportedFileId, validationSummary, validationFileId);
		
		return new GridRecordSetExportResponse()
			.setSessionId(request.getSessionId())
			.setRecordSetId(recordSet.getId())
			.setRecordSetVersionNumber(recordSet.getVersionNumber())
			.setValidationSummaryStatistics(recordSet.getValidationSummary())
			.setValidationFileHandleId(recordSet.getValidationFileHandleId());
	}

	String exportToCsv(UserInfo user, String sessionId, CsvTableDescriptor csvDescriptor, AsyncJobProgressCallback jobCallback, RowViewCallbackHandler rowCallback) {
		
		DownloadFromGridRequest request = new DownloadFromGridRequest()
			.setSessionId(sessionId)
			.setWriteHeader(true)
			.setIncludeEtag(false)
			.setIncludeRowIdAndRowVersion(false)
			.setCsvTableDescriptor(csvDescriptor);
		
		DownloadFromGridResult result;
		
		try {
			result = csvExporter.exportGridAsCsv(user, request, jobCallback, rowCallback);
		} catch (IOException e) {
			throw new IllegalStateException("Could not export the grid to a CSV file.", e);
		}
		
		return result.getResultsFileHandleId();
	}
	
	RecordSet createNewVersion(UserInfo user, RecordSet recordSet, String newFileHandleId, ValidationSummaryStatistics validationSummary, String validationFileHandleId) {
		recordSet.setDataFileHandleId(newFileHandleId);
		// The file handle with the validation details is stored in the revision table
		recordSet.setValidationFileHandleId(validationFileHandleId);
		recordSet.setVersionLabel(null);
		
		// Updates the entity (note that the manager skips service level validation and allows updating the validation file handle)
		entityManager.updateEntity(user, recordSet, true, null);
		
		RecordSet updated = entityManager.getEntity(user, recordSet.getId(), RecordSet.class);
		
		Long recordSetId = KeyFactory.stringToKey(recordSet.getId());
		Long recordSetVersion = updated.getVersionNumber();
		
		// Persists the validation summary 
		validationResultDao.setRecordSetValidationSummaryStatistics(recordSetId, recordSetVersion, validationSummary);
		
		// We set this manually since this is set at the service layer and not stored in the revision table
		updated.setValidationSummary(validationSummary);
		
		return updated;
	}
	
	final class ValidationSummaryBuilder implements RowViewCallbackHandler {

		private String recordSetId;
		private int totalCount = 0;
		private int validCount = 0;
		private int invalidCount = 0;
		private int unknownCount = 0;
		private CSVWriter validationCsvWriter;
		
		ValidationSummaryBuilder(String recordSetId, CSVWriter validationCsvWriter) throws IOException {
			this.recordSetId = recordSetId;
			this.validationCsvWriter = validationCsvWriter;
			this.validationCsvWriter.writeNext(VALIDATION_CSV_HEADERS);
		}
		
		@Override
		public void next(RowView rowView) {
			// row_index, is_valid, validation_error_message, all_validation_messages
			String[] rowValidationDetails = new String[] { String.valueOf(totalCount), null, null, null };
			
			totalCount++;
			
			ValidationResults validationResult = rowView.getRowValidationResults();
			
			if (validationResult == null) {
				unknownCount++;
			} else if (Boolean.TRUE.equals(validationResult.getIsValid())) {
				validCount++;
				rowValidationDetails[1] = "true";
			} else {
				invalidCount++;
				rowValidationDetails[1] = "false";
				rowValidationDetails[2] = validationResult.getValidationErrorMessage();
				rowValidationDetails[3] = JDOSecondaryPropertyUtils.writeStringListToJson(validationResult.getAllValidationMessages());
			}
			
			try {
				validationCsvWriter.writeNext(rowValidationDetails);
			} catch (IOException e) {
				throw new IllegalStateException("Could not write validation details to CSV file.", e);
			}
		}
		
		ValidationSummaryStatistics getValidationSummary() {
			return new ValidationSummaryStatistics()
				.setContainerId(recordSetId)
				.setTotalNumberOfChildren(Long.valueOf(totalCount))
				.setNumberOfValidChildren(Long.valueOf(validCount))
				.setNumberOfInvalidChildren(Long.valueOf(invalidCount))
				.setNumberOfUnknownChildren(Long.valueOf(unknownCount))
				.setGeneratedOn(new Date());
		}
		
	}
	
}
