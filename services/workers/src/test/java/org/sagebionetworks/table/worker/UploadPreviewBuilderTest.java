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
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewResult;
import org.sagebionetworks.util.csv.CsvNullReader;

public class UploadPreviewBuilderTest {
	
	CsvTableDescriptor descriptor;
	ProgressReporter mockReporter;
	String eachTypeCSV;
	
	@Before
	public void before(){
		mockReporter = Mockito.mock(ProgressReporter.class);
		List<String[]> input = new ArrayList<String[]>(3);
		input.add(new String[] { "string", "int", "boolean", "double" });
		input.add(new String[] { "CCC", null, "false", "1.1" });
		input.add(new String[] { "FFF", "4", "true", "2.2", });
		eachTypeCSV = TableModelTestUtils.createCSVString(input);
		descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(true);
	}
	
	@Test
	public void testNullCurrentAlltypes() throws IOException{
		StringReader sReader = new StringReader(eachTypeCSV);
		CsvNullReader reader = new CsvNullReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, null, mockReporter, descriptor);
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertEquals(new Long(3), result.getRowCount());
		verify(mockReporter, times(3)).tryReportProgress(anyInt());;
	}

}
