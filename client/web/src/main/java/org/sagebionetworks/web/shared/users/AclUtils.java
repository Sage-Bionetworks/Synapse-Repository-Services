package org.sagebionetworks.web.shared.users;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AclUtils {	 
	
	public static PermissionLevel getPermissionLevel(List<AclAccessType> accessTypes) {		
		// TODO : this should be updated to be more generic
		if(accessTypes.contains(AclAccessType.READ)
			&& accessTypes.contains(AclAccessType.CREATE)
			&& accessTypes.contains(AclAccessType.UPDATE)
			&& accessTypes.contains(AclAccessType.DELETE)
			&& accessTypes.contains(AclAccessType.CHANGE_PERMISSIONS)) {
			return PermissionLevel.CAN_ADMINISTER;
		} else if(accessTypes.contains(AclAccessType.READ) 
				&& accessTypes.contains(AclAccessType.CREATE)
				&& accessTypes.contains(AclAccessType.UPDATE)
				&& !accessTypes.contains(AclAccessType.DELETE)
				&& !accessTypes.contains(AclAccessType.CHANGE_PERMISSIONS)) {
			return PermissionLevel.CAN_EDIT;
		} else if(accessTypes.contains(AclAccessType.READ) 
				&& !accessTypes.contains(AclAccessType.CREATE)
				&& !accessTypes.contains(AclAccessType.UPDATE)
				&& !accessTypes.contains(AclAccessType.DELETE)
				&& !accessTypes.contains(AclAccessType.CHANGE_PERMISSIONS)) {
			return PermissionLevel.CAN_VIEW;
		} else {
			return null;
		}
	}
	
	public static List<AclAccessType> getAclAccessTypes(PermissionLevel permissionLevel) {
		return getPermissionLevelMap().get(permissionLevel);
	}
	
	public static AclAccessType getAclAccessType(String accessType) {
		if(AclAccessType.READ.toString().equals(accessType)) return AclAccessType.READ;
		else if(AclAccessType.CREATE.toString().equals(accessType)) return AclAccessType.CREATE;
		else if(AclAccessType.DELETE.toString().equals(accessType)) return AclAccessType.DELETE;
		else if(AclAccessType.CHANGE_PERMISSIONS.toString().equals(accessType)) return AclAccessType.CHANGE_PERMISSIONS;
		else if(AclAccessType.UPDATE.toString().equals(accessType)) return AclAccessType.UPDATE;
		else return null;
	}


	private static Map<PermissionLevel, List<AclAccessType>> getPermissionLevelMap() {
		Map<PermissionLevel, List<AclAccessType>> permToAclAccessType = new HashMap<PermissionLevel, List<AclAccessType>>();
		permToAclAccessType.put(PermissionLevel.CAN_VIEW,
				Arrays.asList(new AclAccessType[] { AclAccessType.READ }));
		permToAclAccessType.put(
				PermissionLevel.CAN_EDIT,
				Arrays.asList(new AclAccessType[] { AclAccessType.READ,
						AclAccessType.READ, AclAccessType.CREATE,
						AclAccessType.UPDATE }));
		permToAclAccessType.put(PermissionLevel.CAN_ADMINISTER,
				Arrays.asList(new AclAccessType[] { AclAccessType.READ,
						AclAccessType.READ, AclAccessType.CREATE,
						AclAccessType.UPDATE, AclAccessType.DELETE,
						AclAccessType.CHANGE_PERMISSIONS }));
		
		return permToAclAccessType;
	}
	
	private static Map<AclAccessType, List<PermissionLevel>> getAclAccessTypeMap() {
		Map<AclAccessType, List<PermissionLevel>> accessTypeToPerm = new HashMap<AclAccessType, List<PermissionLevel>>();
		
		accessTypeToPerm.put(AclAccessType.READ,
				Arrays.asList(new PermissionLevel[] { PermissionLevel.CAN_VIEW, PermissionLevel.CAN_EDIT, PermissionLevel.CAN_ADMINISTER }));
		accessTypeToPerm.put(AclAccessType.CREATE,
				Arrays.asList(new PermissionLevel[] { PermissionLevel.CAN_EDIT, PermissionLevel.CAN_ADMINISTER }));
		accessTypeToPerm.put(AclAccessType.UPDATE,
				Arrays.asList(new PermissionLevel[] { PermissionLevel.CAN_EDIT, PermissionLevel.CAN_ADMINISTER }));
		accessTypeToPerm.put(AclAccessType.DELETE,
				Arrays.asList(new PermissionLevel[] { PermissionLevel.CAN_ADMINISTER }));
		accessTypeToPerm.put(AclAccessType.CHANGE_PERMISSIONS,
				Arrays.asList(new PermissionLevel[] { PermissionLevel.CAN_ADMINISTER }));
		
		return accessTypeToPerm;
	}
}
