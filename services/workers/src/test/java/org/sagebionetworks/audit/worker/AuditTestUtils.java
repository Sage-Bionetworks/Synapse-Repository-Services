package org.sagebionetworks.audit.worker;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.repo.model.audit.AccessRecord;

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
	public static List<AccessRecord> createList(int count, long startTimestamp){
		List<AccessRecord> list = new LinkedList<AccessRecord>();
		for(int i=0; i<count; i++){
			AccessRecord ar = new AccessRecord();
			ar.setUserId((long) i);
			ar.setElapseMS((long) (10*i));
			ar.setTimestamp(startTimestamp+i);
			ar.setMethod(Method.values()[i%4].toString());
			ar.setSuccess(i%2 > 0);
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
			list.add(ar);
		}
		return list;
	}
	
	enum Method{
		GET,POST,PUT,DELETE
	}
}
