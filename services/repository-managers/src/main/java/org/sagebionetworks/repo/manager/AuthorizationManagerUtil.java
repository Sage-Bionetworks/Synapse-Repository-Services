package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;

public class AuthorizationManagerUtil {

	private static final String FILE_HANDLE_ID_IS_NOT_ASSOCIATED_TEMPLATE = "FileHandleId: %1s is not associated with objectId: %2s of type: %3s";

	public static final AuthorizationStatus AUTHORIZED = new AuthorizationStatus(true, "");
	
	//  convenience for testing.  In production we don't leave the reason field blank.
	public static final AuthorizationStatus ACCESS_DENIED = new AuthorizationStatus(false, "");
	
	public static void checkAuthorizationAndThrowException(AuthorizationStatus auth) throws UnauthorizedException {
		if (!auth.getAuthorized()) throw new UnauthorizedException(auth.getReason());
	}

	public static AuthorizationStatus accessDenied(String reason) {
		return new AuthorizationStatus(false, reason);
	}

	
	/**
	 * Create an access denied status for a file handle not associated with the requested object.
	 * @param fileHandleId
	 * @param associatedObjectId
	 * @param associationType
	 * @return
	 */
	public static AuthorizationStatus accessDeniedFileNotAssociatedWithObject(String fileHandleId, String associatedObjectId, FileHandleAssociateType associateType){
		return AuthorizationManagerUtil.accessDenied(String
				.format(FILE_HANDLE_ID_IS_NOT_ASSOCIATED_TEMPLATE,
						fileHandleId, associatedObjectId,
						associateType));
	}
	
}
