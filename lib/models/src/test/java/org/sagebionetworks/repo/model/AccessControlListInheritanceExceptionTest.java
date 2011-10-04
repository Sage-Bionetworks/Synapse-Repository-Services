package org.sagebionetworks.repo.model;

import static org.junit.Assert.*;

import org.junit.Test;

public class AccessControlListInheritanceExceptionTest {
	
	@Test
	public void testUrlConstructor(){
		String message = ACLInheritanceException.DEFAULT_MSG_PREFIX+"http://localhost:8080/repo/v1/project/45/acl";
		ACLInheritanceException e = new ACLInheritanceException(message);
		assertEquals(ObjectType.project, e.getBenefactorType());
		assertEquals("45", e.getBenefactorId());
	}

}
