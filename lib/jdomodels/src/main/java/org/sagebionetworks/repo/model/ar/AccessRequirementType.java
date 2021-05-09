package org.sagebionetworks.repo.model.ar;

import org.sagebionetworks.repo.model.*;
import org.sagebionetworks.repo.model.RestrictionLevel;
import org.sagebionetworks.util.ValidateArgument;

public enum AccessRequirementType {

	TOU(TermsOfUseAccessRequirement.class, RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, /* ATC */false, /* ToU */true, /* Lock */false),
	SELF_SIGNED(SelfSignAccessRequirement.class, RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, /* ATC */false, /* ToU */true, /* Lock */false),
	ATC(ACTAccessRequirement.class, RestrictionLevel.CONTROLLED_BY_ACT, /* ATC */true, /* ToU */false, /* Lock */false),
	MANAGED_ATC(ManagedACTAccessRequirement.class, RestrictionLevel.CONTROLLED_BY_ACT, /* ATC */ true, /* ToU */false,/* Lock */false),
	LOCK(LockAccessRequirement.class, RestrictionLevel.CONTROLLED_BY_ACT, /* ATC */false, /* ToU */false,/* Lock */true);

	private Class<? extends AccessRequirement> clazz;
	private RestrictionLevel restrictionLevel;
	private boolean hasACT;
	private boolean hasToU;
	private boolean hasLock;

	AccessRequirementType(Class<? extends AccessRequirement> clazz, RestrictionLevel restrictionLevel, boolean hasACT,
			boolean hasToU, boolean hasLock) {
		this.clazz = clazz;
		this.restrictionLevel = restrictionLevel;
		this.hasACT = hasACT;
		this.hasToU = hasToU;
		this.hasLock = hasLock;
	}

	/**
	 * Lookup AccessRequirementType from the given full class name.
	 * 
	 * @param className
	 * @return
	 */
	public static AccessRequirementType lookupClassName(String className) {
		ValidateArgument.required(className, "className");
		for (AccessRequirementType type : AccessRequirementType.values()) {
			if (type.clazz.getName().equals(className)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown type: '" + className+"'");
	}

	/**
	 * @return the restrictionLevel
	 */
	public RestrictionLevel getRestrictionLevel() {
		return restrictionLevel;
	}

	/**
	 * @return the hasACT
	 */
	public boolean hasACT() {
		return hasACT;
	}

	/**
	 * @return the hasToU
	 */
	public boolean hasToU() {
		return hasToU;
	}

	/**
	 * @return the hasLock
	 */
	public boolean hasLock() {
		return hasLock;
	}
	
}