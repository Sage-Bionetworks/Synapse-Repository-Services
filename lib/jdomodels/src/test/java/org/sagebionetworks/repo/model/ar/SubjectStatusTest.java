package org.sagebionetworks.repo.model.ar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.RestrictionLevel;

public class SubjectStatusTest {

	Long subjectId;
	Long userId;

	@BeforeEach
	public void before() {
		subjectId = 123L;
		userId = 456L;
	}

	@Test
	public void testGetMostRestrictiveLevelWithNoRestrictions() {
		SubjectStatus status = new SubjectStatus(subjectId, userId);
		assertEquals(RestrictionLevel.OPEN, status.getMostRestrictiveLevel());
	}

	@Test
	public void testGetMostRestrictiveLevelWithEachType() {
		SubjectStatus status = new SubjectStatus(subjectId, userId);
		status.addRestrictionStatus(new UsersRequirementStatus().withIsUnmet(true).withRequirementId(1L)
				.withRequirementType(AccessRequirementType.ATC));
		status.addRestrictionStatus(new UsersRequirementStatus().withIsUnmet(true).withRequirementId(2L)
				.withRequirementType(AccessRequirementType.TOU));
		status.addRestrictionStatus(new UsersRequirementStatus().withIsUnmet(true).withRequirementId(3L)
				.withRequirementType(AccessRequirementType.LOCK));
		status.addRestrictionStatus(new UsersRequirementStatus().withIsUnmet(true).withRequirementId(3L)
				.withRequirementType(AccessRequirementType.SELF_SIGNED));
		// call under test
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, status.getMostRestrictiveLevel());
	}
	
	@Test
	public void testGetMostRestrictiveLevelWithMultipleToU() {
		SubjectStatus status = new SubjectStatus(subjectId, userId);
		status.addRestrictionStatus(new UsersRequirementStatus().withIsUnmet(true).withRequirementId(1L)
				.withRequirementType(AccessRequirementType.TOU));
		status.addRestrictionStatus(new UsersRequirementStatus().withIsUnmet(true).withRequirementId(2L)
				.withRequirementType(AccessRequirementType.TOU));
		// call under test
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, status.getMostRestrictiveLevel());
	}
	
	@Test
	public void testGetMostRestrictiveLevelWithToUThenATC() {
		SubjectStatus status = new SubjectStatus(subjectId, userId);
		status.addRestrictionStatus(new UsersRequirementStatus().withIsUnmet(true).withRequirementId(1L)
				.withRequirementType(AccessRequirementType.TOU));
		status.addRestrictionStatus(new UsersRequirementStatus().withIsUnmet(true).withRequirementId(2L)
				.withRequirementType(AccessRequirementType.ATC));
		// call under test
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, status.getMostRestrictiveLevel());
	}
	
	@Test
	public void testGetMostRestrictiveLevelWithATCThenToU() {
		SubjectStatus status = new SubjectStatus(subjectId, userId);
		status.addRestrictionStatus(new UsersRequirementStatus().withIsUnmet(true).withRequirementId(1L)
				.withRequirementType(AccessRequirementType.ATC));
		status.addRestrictionStatus(new UsersRequirementStatus().withIsUnmet(true).withRequirementId(2L)
				.withRequirementType(AccessRequirementType.TOU));
		// call under test
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, status.getMostRestrictiveLevel());
	}

}
