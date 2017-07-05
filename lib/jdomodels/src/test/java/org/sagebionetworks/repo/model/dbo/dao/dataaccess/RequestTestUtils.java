package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.util.Arrays;
import java.util.Date;

import org.sagebionetworks.repo.model.dataaccess.AccessType;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.Renewal;
import org.sagebionetworks.repo.model.dataaccess.Request;

public class RequestTestUtils {

	public static Request createNewRequest() {
		Request dto = new Request();
		dto.setId("1");
		dto.setAccessRequirementId("2");
		dto.setResearchProjectId("3");
		dto.setCreatedBy("4");
		dto.setCreatedOn(new Date());
		dto.setModifiedBy("5");
		dto.setModifiedOn(new Date());
		dto.setEtag("etag");
		AccessorChange add = new AccessorChange();
		add.setUserId("6");
		add.setType(AccessType.GAIN_ACCESS);
		AccessorChange renew = new AccessorChange();
		renew.setType(AccessType.RENEW_ACCESS);
		renew.setUserId("7");
		AccessorChange revoke = new AccessorChange();
		revoke.setType(AccessType.REVOKE_ACCESS);
		revoke.setUserId("8");
		dto.setAccessorChanges(Arrays.asList(add, renew, revoke));
		dto.setDucFileHandleId("9");
		dto.setIrbFileHandleId("10");
		dto.setAttachments(Arrays.asList("11", "12"));
		return dto;
	}

	public static Renewal createNewRenewal() {
		Renewal dto = new Renewal();
		dto.setId("1");
		dto.setAccessRequirementId("2");
		dto.setResearchProjectId("3");
		dto.setCreatedBy("4");
		dto.setCreatedOn(new Date());
		dto.setModifiedBy("5");
		dto.setModifiedOn(new Date());
		dto.setEtag("etag");
		AccessorChange add = new AccessorChange();
		add.setUserId("6");
		add.setType(AccessType.GAIN_ACCESS);
		AccessorChange renew = new AccessorChange();
		renew.setType(AccessType.RENEW_ACCESS);
		renew.setUserId("7");
		AccessorChange revoke = new AccessorChange();
		revoke.setType(AccessType.REVOKE_ACCESS);
		revoke.setUserId("8");
		dto.setAccessorChanges(Arrays.asList(add, renew, revoke));
		dto.setDucFileHandleId("9");
		dto.setIrbFileHandleId("10");
		dto.setAttachments(Arrays.asList("11", "12"));
		dto.setPublication("publication");
		dto.setSummaryOfUse("summaryOfUse");
		dto.setConcreteType(Renewal.class.getName());
		return dto;
	}

}
