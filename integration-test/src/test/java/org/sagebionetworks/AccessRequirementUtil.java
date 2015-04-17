package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;

public class AccessRequirementUtil {
	// check that a paginated results wrapping a ToU matches a given ToU
	public static void checkTOUlist(PaginatedResults<AccessRequirement> pagingatedResults, TermsOfUseAccessRequirement tou) {
		assertEquals(1L, pagingatedResults.getTotalNumberOfResults());
		List<AccessRequirement> ars = pagingatedResults.getResults();
		assertEquals(1, ars.size());
		AccessRequirement ar = ars.iterator().next();
		assertTrue(ar instanceof TermsOfUseAccessRequirement);
		TermsOfUseAccessRequirement tou2 = (TermsOfUseAccessRequirement)ar;
		assertEquals(tou.getAccessType(), tou2.getAccessType());
		assertEquals(tou.getSubjectIds(), tou2.getSubjectIds());	
	}
	

}
