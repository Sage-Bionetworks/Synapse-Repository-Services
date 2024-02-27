package org.sagebionetworks.repo.model.ar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.RestrictionLevel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
		UsersRestrictionStatus status = new UsersRestrictionStatus().withSubjectId(subjectId).withUserId(userId);
		assertEquals(RestrictionLevel.OPEN, status.getMostRestrictiveLevel());
	}

	@Test
	public void testGetMostRestrictiveLevelWithEachType() {
		UsersRestrictionStatus status = new UsersRestrictionStatus()
											.withSubjectId(subjectId)
											.withUserId(userId)
											.withRestrictionStatus(List.of(
													new UsersRequirementStatus().withIsUnmet(true).withRequirementId(1L)
															.withRequirementType(AccessRequirementType.ATC),
													new UsersRequirementStatus().withIsUnmet(true).withRequirementId(2L)
															.withRequirementType(AccessRequirementType.TOU),
													new UsersRequirementStatus().withIsUnmet(true).withRequirementId(3L)
															.withRequirementType(AccessRequirementType.LOCK),
													new UsersRequirementStatus().withIsUnmet(true).withRequirementId(3L)
															.withRequirementType(AccessRequirementType.SELF_SIGNED)
											));
		// call under test
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, status.getMostRestrictiveLevel());
	}
	
	@Test
	public void testGetMostRestrictiveLevelWithMultipleToU() {
		UsersRestrictionStatus status = new UsersRestrictionStatus()
												.withSubjectId(subjectId)
												.withUserId(userId)
												.withRestrictionStatus(List.of(
														new UsersRequirementStatus().withIsUnmet(true).withRequirementId(1L)
																.withRequirementType(AccessRequirementType.TOU),
														new UsersRequirementStatus().withIsUnmet(true).withRequirementId(2L)
																.withRequirementType(AccessRequirementType.TOU)
														));
		// call under test
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, status.getMostRestrictiveLevel());
	}
	
	@Test
	public void testGetMostRestrictiveLevelWithToUThenATC() {
		UsersRestrictionStatus status = new UsersRestrictionStatus()
											.withSubjectId(subjectId)
											.withUserId(userId)
											.withRestrictionStatus(List.of(
													new UsersRequirementStatus().withIsUnmet(true).withRequirementId(1L)
															.withRequirementType(AccessRequirementType.TOU),
													new UsersRequirementStatus().withIsUnmet(true).withRequirementId(2L)
															.withRequirementType(AccessRequirementType.ATC)
											));
		// call under test
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, status.getMostRestrictiveLevel());
	}
	
	@Test
	public void testGetMostRestrictiveLevelWithATCThenToU() {
		UsersRestrictionStatus status = new UsersRestrictionStatus()
											.withSubjectId(subjectId)
											.withUserId(userId)
											.withRestrictionStatus(List.of(
													new UsersRequirementStatus().withIsUnmet(true).withRequirementId(1L)
															.withRequirementType(AccessRequirementType.ATC),
													new UsersRequirementStatus().withIsUnmet(true).withRequirementId(2L)
															.withRequirementType(AccessRequirementType.TOU)
											));
		// call under test
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, status.getMostRestrictiveLevel());
	}

}
