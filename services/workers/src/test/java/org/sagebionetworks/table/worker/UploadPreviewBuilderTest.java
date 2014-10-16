package org.sagebionetworks.table.worker;

import static org.junit.Assert.*;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.TableConstants;
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
		
		List<Row> expectedRows = new ArrayList<Row>();
		// 0
		Row row = new Row();
		row.setValues(Arrays.asList(input.get(1)));
		expectedRows.add(row);
		// 1
		row = new Row();
		row.setValues(Arrays.asList(input.get(2)));
		expectedRows.add(row);
		// 2
		row = new Row();
		row.setValues(Arrays.asList(input.get(3)));
		expectedRows.add(row);
		// 3
		row = new Row();
		row.setValues(Arrays.asList(input.get(4)));
		expectedRows.add(row);
		
		StringReader sReader = new StringReader(eachTypeCSV);
		CsvNullReader reader = new CsvNullReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockReporter, request);
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertEquals(new Long(4), result.getRowsScanned());
		assertEquals(expectedSchema, result.getSuggestedColumns());
		assertEquals(expectedRows, result.getSampleRows());
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
		
		List<Row> expectedRows = new ArrayList<Row>();
		// 0
		Row row = new Row();
		row.setValues(Arrays.asList(input.get(0)));
		expectedRows.add(row);
		// 1
		row = new Row();
		row.setValues(Arrays.asList(input.get(1)));
		expectedRows.add(row);
		
		StringReader sReader = new StringReader(eachTypeCSV);
		CsvNullReader reader = new CsvNullReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockReporter, request);
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertEquals(new Long(2), result.getRowsScanned());
		assertEquals(expectedSchema, result.getSuggestedColumns());
		assertEquals(expectedRows, result.getSampleRows());
		verify(mockReporter, times(2)).tryReportProgress(anyInt());
	}

	
	@Test
	public void testBuildResultsWithRowIdAndVersion() throws IOException{
		List<String[]> input = new ArrayList<String[]>(3);
		// This CSV has ROW_ID and ROW_VERSION
		input.add(new String[] {TableConstants.ROW_ID, TableConstants.ROW_VERSION, "boolean", "double" });
		input.add(new String[] { "1", "0", "false", "1.1" });
		input.add(new String[] { "2", "0", "true", "2.2", });
		String eachTypeCSV = TableModelTestUtils.createCSVString(input);
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(true);
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		request.setCsvTableDescriptor(descriptor);
		// RowId and version should not be included as suggested columns.
		List<ColumnModel> expectedSchema = new ArrayList<ColumnModel>();
		// 0
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.BOOLEAN);
		cm.setName("boolean");
		expectedSchema.add(cm);
		// 1
		cm = new ColumnModel();
		cm.setColumnType(ColumnType.DOUBLE);
		cm.setName("double");
		expectedSchema.add(cm);
		
		List<Row> expectedRows = new ArrayList<Row>();
		// 0
		Row row = new Row();
		row.setValues(Arrays.asList("false", "1.1"));
		row.setRowId(1L);
		row.setVersionNumber(0L);
		expectedRows.add(row);
		// 1
		row = new Row();
		row.setValues(Arrays.asList("true", "2.2"));
		row.setRowId(2L);
		row.setVersionNumber(0L);;
		expectedRows.add(row);
		
		StringReader sReader = new StringReader(eachTypeCSV);
		CsvNullReader reader = new CsvNullReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockReporter, request);
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertEquals(new Long(2), result.getRowsScanned());
		assertEquals(expectedRows, result.getSampleRows());
		assertEquals(expectedSchema, result.getSuggestedColumns());
	}
	
	@Test
	public void testBuildResultsFullScan() throws IOException{
		List<String[]> input = new ArrayList<String[]>(3);
		input.add(new String[] { "CCC", "3" });
		input.add(new String[] { "FFF", "4" });
		input.add(new String[] { "GGG", "5" });
		String eachTypeCSV = TableModelTestUtils.createCSVString(input);
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(false);
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		request.setCsvTableDescriptor(descriptor);
		// A full scan should include all rows.
		request.setDoFullFileScan(true);
		
		StringReader sReader = new StringReader(eachTypeCSV);
		CsvNullReader reader = new CsvNullReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockReporter, request);
		// Set the max beyond the number of rows we have.
		builder.setMaxRowsInpartialScan(input.size()+1);
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertEquals(new Long(3), result.getRowsScanned());
	}
	
	@Test
	public void testBuildResultsPartialScan() throws IOException{
		List<String[]> input = new ArrayList<String[]>(3);
		input.add(new String[] { "CCC", "3" });
		input.add(new String[] { "FFF", "4" });
		input.add(new String[] { "GGG", "5" });
		String eachTypeCSV = TableModelTestUtils.createCSVString(input);
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(false);
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		request.setCsvTableDescriptor(descriptor);
		// We do not want all row scanned.
		request.setDoFullFileScan(false);
		
		StringReader sReader = new StringReader(eachTypeCSV);
		CsvNullReader reader = new CsvNullReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockReporter, request);
		// Set the max beyond the number of rows we have.
		builder.setMaxRowsInpartialScan(input.size()-1);
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertEquals(new Long(2), result.getRowsScanned());
	}
	
	/**
	 * Test for the case where there is no data in one of the columns.
	 * @throws IOException
	 */
	@Test
	public void testPLFM_3032() throws IOException{
		List<String[]> input = new ArrayList<String[]>(4);
		input.add(new String[] { "A", "B","C"});
		input.add(new String[] { "CCC", "3", null });
		input.add(new String[] { "FFF", "4", null });
		input.add(new String[] { "GGG", "5", null });
		String eachTypeCSV = TableModelTestUtils.createCSVString(input);
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(true);
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		request.setCsvTableDescriptor(descriptor);
		
		StringReader sReader = new StringReader(eachTypeCSV);
		CsvNullReader reader = new CsvNullReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockReporter, request);
		// Set the max beyond the number of rows we have.
		builder.setMaxRowsInpartialScan(input.size()-1);
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertEquals(new Long(3), result.getRowsScanned());
	}

}
