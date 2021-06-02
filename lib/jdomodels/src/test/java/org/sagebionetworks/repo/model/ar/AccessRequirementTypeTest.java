package org.sagebionetworks.repo.model.ar;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.LockAccessRequirement;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.RestrictionLevel;
import org.sagebionetworks.repo.model.SelfSignAccessRequirement;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.ar.AccessRequirementType;

@ExtendWith(MockitoExtension.class)
public class AccessRequirementTypeTest {
	
	@Test
	public void testLookupClassNameWithNullValue() {
		String message = assertThrows(IllegalArgumentException.class, ()->{
			AccessRequirementType.lookupClassName(null);
		}).getMessage();
		assertEquals("className is required.", message);
	}
	
	@Test
	public void testLookupClassNameWithUnknownType() {
		String message = assertThrows(IllegalArgumentException.class, ()->{
			AccessRequirementType.lookupClassName("does not exist");
		}).getMessage();
		assertEquals("Unknown type: 'does not exist'", message);
	}

	@Test
	public void testTermsOfUse() {
		AccessRequirementType type = AccessRequirementType.lookupClassName(TermsOfUseAccessRequirement.class.getName());
		assertEquals(AccessRequirementType.TOU, type);
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, type.getRestrictionLevel());
		assertFalse(type.hasACT());
		assertTrue(type.hasToU());
		assertFalse(type.hasLock());
	}
	
	@Test
	public void testSelfSigned() {
		AccessRequirementType type = AccessRequirementType.lookupClassName(SelfSignAccessRequirement.class.getName());
		assertEquals(AccessRequirementType.SELF_SIGNED, type);
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, type.getRestrictionLevel());
		assertFalse(type.hasACT());
		assertTrue(type.hasToU());
		assertFalse(type.hasLock());
	}
	
	@Test
	public void testATC() {
		AccessRequirementType type = AccessRequirementType.lookupClassName(ACTAccessRequirement.class.getName());
		assertEquals(AccessRequirementType.ATC, type);
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, type.getRestrictionLevel());
		assertTrue(type.hasACT());
		assertFalse(type.hasToU());
		assertFalse(type.hasLock());
	}
	
	@Test
	public void testManagedATC() {
		AccessRequirementType type = AccessRequirementType.lookupClassName(ManagedACTAccessRequirement.class.getName());
		assertEquals(AccessRequirementType.MANAGED_ATC, type);
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, type.getRestrictionLevel());
		assertTrue(type.hasACT());
		assertFalse(type.hasToU());
		assertFalse(type.hasLock());
	}
	
	@Test
	public void testLock() {
		AccessRequirementType type = AccessRequirementType.lookupClassName(LockAccessRequirement.class.getName());
		assertEquals(AccessRequirementType.LOCK, type);
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, type.getRestrictionLevel());
		assertFalse(type.hasACT());
		assertFalse(type.hasToU());
		assertTrue(type.hasLock());
	}
}
