package org.sagebionetworks.repo.model;

public class AccessClassHelper {
	public static Class<? extends AccessRequirement> getClass(AccessRequirementType type) {
		switch(type) {
		case TOU_Agreement:
			return TermsOfUseAccessRequirement.class;
	
//	    Not yet defined			
//		case ACT_Approval:
//			return ????AccessRequirement.class;
			
		default:
			throw new IllegalArgumentException("Unexpected type: "+type);
		}	
	}

	public static Class<? extends AccessApproval> getClass(AccessApprovalType type) {
		switch(type) {
		case TOU_Agreement:
			return TermsOfUseAccessApproval.class;
	
//	    Not yet defined			
//		case ACT_Approval:
//			return ????AccessApproval.class;
			
		default:
			throw new IllegalArgumentException("Unexpected type: "+type);
		}	
	}


}
