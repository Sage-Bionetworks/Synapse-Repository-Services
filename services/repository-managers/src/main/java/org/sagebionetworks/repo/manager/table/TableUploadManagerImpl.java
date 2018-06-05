package org.sagebionetworks.repo.manager.table;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.table.CSVToRowIterator;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.TableUpdateResponse;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.table.cluster.utils.CSVUtils;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.bytecode.opencsv.CSVReader;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;

public class TableUploadManagerImpl implements TableUploadManager {
	
	@Autowired
	private TableManagerSupport tableManagerSupport;
	@Autowired
	private FileHandleManager fileHandleManager;
	@Autowired
	private AmazonS3 s3Client;

	@Override
	public TableUpdateResponse uploadCSV(ProgressCallback progressCallback, UserInfo user, UploadToTableRequest request, UploadRowProcessor rowProcessor) {
		CSVReader reader = null;
		try{
			// Get the filehandle
			S3FileHandle fileHandle = (S3FileHandle) fileHandleManager.getRawFileHandle(user, request.getUploadFileHandleId());
			// Get the schema for the table
			List<ColumnModel> tableSchema = tableManagerSupport.getColumnModelsForTable(request.getTableId());
			// Open a stream to the file in S3.
			S3Object s3Object = s3Client.getObject(fileHandle.getBucketName(), fileHandle.getKey());
			// Create a reader from the passed parameters
			// Note: The CSVToRowIterator handles linesToSkip so we pass null linesToSkip for the reader.
			reader = CSVUtils.createCSVReader(new InputStreamReader(s3Object.getObjectContent(), "UTF-8"), request.getCsvTableDescriptor(), null);
			
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
		}
	}

}
