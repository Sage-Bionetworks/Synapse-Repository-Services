package org.sagebionetworks.repo.model;

import org.json.JSONException;
import org.json.JSONObject;

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

	// TODO this should be extracted from or reconciled with the JSON schema for AccessRequirement
	private static final String ACCESS_REQUIREMENT_TYPE_FIELD_NAME = "accessRequirementType";
	
	public static AccessRequirementType getAccessRequirementTypeFromJSON(String jsonString) {
		try {
			JSONObject obj = new JSONObject(jsonString);
			String typeString = obj.getString(ACCESS_REQUIREMENT_TYPE_FIELD_NAME);
			return AccessRequirementType.valueOf(typeString);
		} catch (JSONException e) {
			throw new IllegalArgumentException(e);
		}
	}

	// TODO this should be extracted from or reconciled with the JSON schema for AccessApproval
	private static final String ACCESS_APPROVAL_TYPE_FIELD_NAME = "approvalType";
	
	public static AccessApprovalType getAccessApprovalTypeFromJSON(String jsonString) {
		try {
			JSONObject obj = new JSONObject(jsonString);
			String typeString = obj.getString(ACCESS_APPROVAL_TYPE_FIELD_NAME);
			return AccessApprovalType.valueOf(typeString);
		} catch (JSONException e) {
			throw new IllegalArgumentException(e);
		}
	}


}
