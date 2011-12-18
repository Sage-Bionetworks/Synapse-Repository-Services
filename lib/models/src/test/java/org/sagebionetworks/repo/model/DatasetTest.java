package org.sagebionetworks.repo.model;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class DatasetTest {
	
	@Test
	public void testRoundTripDataset() throws JSONObjectAdapterException {
		Dataset ds1 = new Dataset();
		JSONObjectAdapter adapter1 = new JSONObjectAdapterImpl();
		JSONObjectAdapter adapter2 = new JSONObjectAdapterImpl();
		Date d = new Date();
		
		ds1.setAccessControlList("/acl");
		ds1.setAnnotations("annotations");
		ds1.setCreatedBy("createdBy");
		ds1.setCreatedOn(d);
		ds1.setDescription("description");
		ds1.setEtag("etag");
		ds1.setId("id");
		ds1.setLayers("/layers");
		ds1.setModifiedBy("modifiedBy");
		ds1.setModifiedOn(d);
		ds1.setName("name");
		ds1.setUri("uri");
		ds1.setVersion("1.0.0");
		ds1.setVersionComment("versionComment");
		ds1.setVersionLabel("versionLabel");
		ds1.setVersionNumber(1L);
		ds1.setVersionUrl("versionUrl");
		ds1.setVersions("versions");

		ds1.setEulaId("0");
		ds1.setHasClinicalData(Boolean.TRUE);
		ds1.setHasGeneticData(Boolean.TRUE);
		ds1.setHasExpressionData(Boolean.TRUE);

//		CurationTrackingData cdt = new CurationTrackingData();
//		cdt.setOwner("owner");
//		cdt.setStatus(CurationStatusNames.loaded);
//		ds1.setCurationStatus(cdt);

		List<LocationData> ldl = new ArrayList<LocationData>();
		LocationData ld = new LocationData();
		ld.setContentType("txt");
		ld.setMd5("abcdef");
		ld.setPath("path");
		ld.setType(LocationTypeNames.sage);
		ldl.add(ld);
		ds1.setLocations(ldl);
		
//		AcquisitionTrackingData adt = new AcquisitionTrackingData();
//		adt.setComments("comments");
//		adt.setDataAcquisitionReference("reference");
//		adt.setFollowupRequirements("followupRequirements");
//		adt.setRequestor("requestor");
//		adt.setStatus(AcquisitionStatusNames.denied);
//		
//		List<StatusHistoryRecord> lshr = new ArrayList<StatusHistoryRecord>();
//		StatusHistoryRecord shr = new StatusHistoryRecord();
//		shr.setStatusName("status");
//		lshr.add(shr);
//		adt.setHistory(lshr);
//		ds1.setAcquisitionStatus(adt);
		
		adapter1 = ds1.writeToJSONObject(adapter1);
		String s = adapter1.toJSONString();
		adapter2 = JSONObjectAdapterImpl.createAdapterFromJSONString(s);
		Dataset ds2 = new Dataset(adapter2);
		
		assertEquals(ds1, ds2);
	}
}
