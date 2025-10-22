package org.sagebionetworks.repo.manager.file;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.table.cluster.utils.CSVUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

import au.com.bytecode.opencsv.CSVReader;

@Service
public class CsvFileHandleProviderImpl implements CsvFileHandleProvider {

	private final FileHandleManager fileHandleManager;
	private final BucketObjectReaderProvider fileReaderProvider;

	public CsvFileHandleProviderImpl(FileHandleManager fileHandleManager, BucketObjectReaderProvider fileReaderProvider) {
		this.fileHandleManager = fileHandleManager;
		this.fileReaderProvider = fileReaderProvider;
	}

	@Override
	public CSVReader getCsvReader(UserInfo user, String fileHandleId, CsvTableDescriptor csvDescriptor) {
		ValidateArgument.required(user, "The user");
		ValidateArgument.required(fileHandleId, "The fileHandleId");
		return getCsvReader(fileHandleManager.getRawFileHandle(user, fileHandleId), csvDescriptor);
	}
	
	@Override
	public CSVReader getCsvReader(FileHandle fileHandle, CsvTableDescriptor csvDescriptor) {
		ValidateArgument.required(fileHandle, "The fileHandle");
		ValidateArgument.required(csvDescriptor, "The csvDescriptor");
		ValidateArgument.requirement(fileHandle instanceof CloudProviderFileHandleInterface, "Only S3 and Google Cloud Storage files that Synapse can access are supported.");
		
		CloudProviderFileHandleInterface cpFileHandle = (CloudProviderFileHandleInterface) fileHandle;
		
		BucketObjectReader fileReader = fileReaderProvider.getBucketObjectReader(cpFileHandle.getClass());
		
		InputStream is = fileReader.openStream(cpFileHandle.getBucketName(), cpFileHandle.getKey());

		return CSVUtils.createCSVReader(new InputStreamReader(is, StandardCharsets.UTF_8), csvDescriptor, null);
	}

}
