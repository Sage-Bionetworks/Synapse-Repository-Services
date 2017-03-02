package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.util.Arrays;
import java.util.Date;

import org.sagebionetworks.repo.model.dataaccess.DataAccessRenewal;

public class DataAccessRequestTestUtils {

	public static DataAccessRenewal createNewDataAccessRenewal() {
		DataAccessRenewal dto = new DataAccessRenewal();
		dto.setId("1");
		dto.setAccessRequirementId("2");
		dto.setResearchProjectId("3");
		dto.setCreatedBy("4");
		dto.setCreatedOn(new Date());
		dto.setModifiedBy("5");
		dto.setModifiedOn(new Date());
		dto.setEtag("etag");
		dto.setAccessors(Arrays.asList("6", "7", "8"));
		dto.setDucFileHandleId("9");
		dto.setIrbFileHandleId("10");
		dto.setAttachments(Arrays.asList("11", "12"));
		dto.setPublication("publication");
		dto.setSummaryOfUse("summaryOfUse");
		return dto;
	}

}
