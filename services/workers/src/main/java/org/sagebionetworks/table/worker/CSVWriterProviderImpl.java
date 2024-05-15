package org.sagebionetworks.table.worker;

import java.io.FileWriter;

import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.table.cluster.utils.CSVUtils;
import org.springframework.stereotype.Service;

import au.com.bytecode.opencsv.CSVWriter;

@Service
public class CSVWriterProviderImpl implements CSVWriterProvider {

	@Override
	public CSVWriter createWriter(FileWriter fileWriter, CsvTableDescriptor csvTableDescriptor) {
		return CSVUtils.createCSVWriter(fileWriter, csvTableDescriptor);
	}

}