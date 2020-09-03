package org.sagebionetworks.audit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

/**
 * 
 * @author jmhill
 *
 */
public class AuditTestUtils {

	/**
	 * Create a list for use in testing that is sorted on timestamp
	 * @param count
	 * @param startTimestamp
	 * @return
	 */
	public static List<AccessRecord> createAccessRecordList(int count, long startTimestamp){
		List<AccessRecord> list = new LinkedList<AccessRecord>();
		for(int i=0; i<count; i++){
			AccessRecord ar = new AccessRecord();
			ar.setUserId((long) i);
			ar.setElapseMS((long) (10*i));
			ar.setTimestamp(startTimestamp+i);
			ar.setMethod(Method.values()[i%4].toString());
			if(i%2 > 0){
				ar.setSuccess(true);
				ar.setResponseStatus(201L);
			}else{
				ar.setSuccess(false);
				ar.setResponseStatus(401L);
			}
			ar.setRequestURL("/url/"+i);
			ar.setSessionId(UUID.randomUUID().toString());
			ar.setHost("localhost:8080");
			ar.setOrigin("http://www.example-social-network.com");
			ar.setUserAgent("The bat-mobile OS");
			ar.setThreadId(Thread.currentThread().getId());
			ar.setVia("1 then two");
			ar.setStack("stack");
			ar.setInstance("0001");
			ar.setVmId("vmId");
			ar.setQueryString("value=bar");
			ar.setReturnObjectId("syn123");
			ar.setOauthClientId("99999");
			ar.setBasicAuthUsername("basicUsr");
			list.add(ar);
		}
		return list;
	}
	
	enum Method{
		GET,POST,PUT,DELETE
	}

	public static List<ObjectRecord> createUserProfileObjectRecordList(int numberOfRecords) {
		UserProfile up = new UserProfile();
		up.setCompany("Sage");
		up.setEmail("employee@sagebase.org");
		up.setEmails(Arrays.asList("employee@sagebase.org", "employee@gmail.com", "employee@yahoo.com"));
		up.setLocation("Seattle");
		List<ObjectRecord> list = new ArrayList<ObjectRecord>();
		for (int i = 0; i < numberOfRecords; i++) {
			ObjectRecord newRecord = new ObjectRecord();
			try {
				newRecord.setJsonString(EntityFactory.createJSONStringForEntity(up));
			} catch (JSONObjectAdapterException e) {
				newRecord.setJsonString("");
			}
			newRecord.setJsonClassName(up.getClass().getSimpleName());
			newRecord.setTimestamp(System.currentTimeMillis());
			list.add(newRecord);
		}
		return list;
	}
}
