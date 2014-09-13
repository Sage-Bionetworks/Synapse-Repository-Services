package org.sagebionetworks.table.worker;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewRequest;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewResult;
import org.sagebionetworks.util.csv.CsvNullReader;

public class UploadPreviewBuilderTest {
	
	ProgressReporter mockReporter;
	
	@Before
	public void before(){
		mockReporter = Mockito.mock(ProgressReporter.class);
	}
	
	@Test
	public void testBuildResultsWithHeader() throws IOException{
		List<String[]> input = new ArrayList<String[]>(3);
		input.add(new String[] { "string", "int", "boolean", "double" });
		input.add(new String[] { "CCC", null, "false", "1.1" });
		// include empties
		input.add(new String[] { "", "", "", "", });
		// included nulls
		input.add(new String[] { null, null, null, null, });
		input.add(new String[] { "FFF", "4", "true", "2.2", });
		String eachTypeCSV = TableModelTestUtils.createCSVString(input);
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(true);
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		request.setCsvTableDescriptor(descriptor);
		
		List<ColumnModel> expectedSchema = new ArrayList<ColumnModel>();
		// 0
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.STRING);
		cm.setName("string");
		cm.setMaximumSize(3L);
		expectedSchema.add(cm);
		// 1
		cm = new ColumnModel();
		cm.setColumnType(ColumnType.INTEGER);
		cm.setName("int");
		expectedSchema.add(cm);
		// 2
		cm = new ColumnModel();
		cm.setColumnType(ColumnType.BOOLEAN);
		cm.setName("boolean");
		expectedSchema.add(cm);
		// 3
		cm = new ColumnModel();
		cm.setColumnType(ColumnType.DOUBLE);
		cm.setName("double");
		expectedSchema.add(cm);
		
		StringReader sReader = new StringReader(eachTypeCSV);
		CsvNullReader reader = new CsvNullReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockReporter, request);
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertEquals(new Long(4), result.getRowCount());
		assertEquals(expectedSchema, result.getHeaderColumns());
		verify(mockReporter, times(4)).tryReportProgress(anyInt());;
	}
	
	@Test
	public void testBuildResultsNoHeader() throws IOException{
		List<String[]> input = new ArrayList<String[]>(3);
		input.add(new String[] { "CCC", "3" });
		input.add(new String[] { "FFF", "4" });
		String eachTypeCSV = TableModelTestUtils.createCSVString(input);
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(false);
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		request.setCsvTableDescriptor(descriptor);
		
		List<ColumnModel> expectedSchema = new ArrayList<ColumnModel>();
		// 0
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.STRING);
		cm.setName(null);
		cm.setMaximumSize(3L);
		expectedSchema.add(cm);
		// 1
		cm = new ColumnModel();
		cm.setColumnType(ColumnType.INTEGER);
		cm.setName(null);
		expectedSchema.add(cm);
		
		StringReader sReader = new StringReader(eachTypeCSV);
		CsvNullReader reader = new CsvNullReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockReporter, request);
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertEquals(new Long(2), result.getRowCount());
		assertEquals(expectedSchema, result.getHeaderColumns());
		verify(mockReporter, times(2)).tryReportProgress(anyInt());;
	}

}
