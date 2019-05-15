package org.sagebionetworks.repo.manager.table;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.table.CSVToRowIterator;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.file.FileConstants;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.TableUpdateResponse;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.table.cluster.utils.CSVUtils;
import org.sagebionetworks.util.FileProvider;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.model.GetObjectRequest;

import au.com.bytecode.opencsv.CSVReader;

public class TableUploadManagerImpl implements TableUploadManager {
	
	@Autowired
	private TableManagerSupport tableManagerSupport;
	@Autowired
	private FileHandleManager fileHandleManager;
	@Autowired
	private SynapseS3Client s3Client;
	@Autowired
	private FileProvider fileProvider;

	@Override
	public TableUpdateResponse uploadCSV(ProgressCallback progressCallback, UserInfo user, UploadToTableRequest request, UploadRowProcessor rowProcessor) {
		CSVReader reader = null;
		File tempFile = null;
		try{
			// Get the filehandle
			S3FileHandle fileHandle = (S3FileHandle) fileHandleManager.getRawFileHandle(user, request.getUploadFileHandleId());
			if(fileHandle.getContentSize() == null) {
				throw new IllegalArgumentException("File content size cannot be null.");
			}
			if(fileHandle.getContentSize() > FileConstants.BULK_FILE_DOWNLOAD_MAX_SIZE_BYTES) {
				throw new IllegalArgumentException("The provided CSV file exceeds the maximum size of "+FileConstants.MAX_FILE_SIZE_GB+" GB.");
			}
			IdAndVersion idAndVersion = IdAndVersion.parse(request.getTableId());
			// Get the schema for the table
			List<ColumnModel> tableSchema = tableManagerSupport.getColumnModelsForTable(idAndVersion);
			// download the CSV to a temp file (see PLFM-4975).
			tempFile = fileProvider.createTempFile("TableUploadManagerImpl", ".csv");
			s3Client.getObject(new GetObjectRequest(fileHandle.getBucketName(), fileHandle.getKey()), tempFile);
			// Create a reader from the passed parameters
			// Note: The CSVToRowIterator handles linesToSkip so we pass null linesToSkip for the reader.
			reader = CSVUtils.createCSVReader(new InputStreamReader(fileProvider.createFileInputStream(tempFile), "UTF-8"), request.getCsvTableDescriptor(), null);
			
			if(request.getColumnIds() != null && !request.getColumnIds().isEmpty()){
				throw new IllegalArgumentException("Unsupported columnIds");
			}
			
			// Create the iterator
			boolean isFirstLineHeader = CSVUtils.isFirstRowHeader(request.getCsvTableDescriptor());
			CSVToRowIterator iterator = new CSVToRowIterator(tableSchema, reader, isFirstLineHeader, request.getLinesToSkip());
			// Append the data to the table
			return rowProcessor.processRows(user, request.getTableId(),
					tableSchema, iterator, request.getUpdateEtag(), progressCallback);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}finally{
			if(reader != null){
				try {
					// Unconditionally close the stream to the S3 file.
					reader.close();
				} catch (IOException e) {}
			}
			if(tempFile != null) {
				tempFile.delete();
			}
		}
	}

}
