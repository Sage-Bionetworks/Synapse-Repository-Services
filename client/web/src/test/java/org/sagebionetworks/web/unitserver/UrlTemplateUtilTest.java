package org.sagebionetworks.web.unitserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.web.server.UrlTemplateUtil;
import org.sagebionetworks.web.shared.ColumnInfo.Type;
import org.sagebionetworks.web.shared.HeaderData;
import org.sagebionetworks.web.shared.UrlTemplate;

public class UrlTemplateUtilTest {
	
	String templateId;
	String tempalte;
	String datasetId;
	String sampleId;
	UrlTemplate meta;
	
	
	@Before
	public void setup(){
		templateId = "templateId";
		tempalte = "http://example.com/datasets/{datasetsId}/samples?sample={sampleId}";
		datasetId = "datasetsId";
		sampleId = "sampleId";
		meta = new UrlTemplate(tempalte, templateId, Type.String.name(), "display", "desc");
	}
	
	@Test
	public void testGetTempateDependencyIds(){
		// First off this template should depend on two other column ids
		List<String> variables = UrlTemplateUtil.getTempateDependencyIds(meta); 
		assertNotNull(variables);
		assertTrue(variables.contains(datasetId));
		assertTrue(variables.contains(sampleId));
	}
	
	@Test
	public void testProcessUrlTempalte(){
		List<HeaderData> headerList = new ArrayList<HeaderData>();	
		headerList.add(meta);
		// Now fill in a map with a few rows
		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
		for(int i=0; i<2; i++){
			HashMap<String, Object> row = new HashMap<String, Object>();
			row.put(datasetId, "dsid"+i);
			row.put(sampleId, "sample"+i);
			rows.add(row);
		}
		// Send off for processing
		UrlTemplateUtil.processUrlTemplates(headerList, rows);
		// Now make sure the url was added to each row
		Map<String, Object> row = rows.get(0);
		assertNotNull(row);
		String urlResult = (String) row.get(templateId);
		assertNotNull(urlResult);
		System.out.println("Resulting url: "+urlResult);
		assertEquals("http://example.com/datasets/dsid0/samples?sample=sample0", urlResult);
		// Check one more row
		row = rows.get(1);
		assertNotNull(row);
		urlResult = (String) row.get(templateId);
		assertNotNull(urlResult);
		System.out.println("Resulting url: "+urlResult);
		assertEquals("http://example.com/datasets/dsid1/samples?sample=sample1", urlResult);
	}

}
