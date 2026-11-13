package org.sagebionetworks.repo.manager.file;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FileHandleAuthorizationManagerImpl implements FileHandleAuthorizationManager {

	private final FileHandleDao fileHandleDao;

	@Autowired
	public FileHandleAuthorizationManagerImpl(FileHandleDao fileHandleDao) {
		super();
		this.fileHandleDao = fileHandleDao;
	}

	@Override
	public AuthorizationStatus canAccessRawFileHandleById(UserInfo userInfo, String fileHandleId)
			throws NotFoundException {
		ValidateArgument.required(fileHandleId, "fileHandleId");
		return canAccessRawFileHandlesByIds(userInfo, List.of(fileHandleId));
	}

	@Override
	public AuthorizationStatus canAccessRawFileHandlesByIds(UserInfo userInfo, Collection<String> fileHandleIds)
			throws NotFoundException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(fileHandleIds, "fileHandleIds");
		if (fileHandleIds.isEmpty()) {
			return AuthorizationStatus.authorized();
		}
		// Existence is checked for admins too, so a missing file is never accepted.
		Map<String, FileHandle> handles = fileHandleDao.getFileHandlesOrThrow(fileHandleIds);
		return canAccessRawFileHandles(userInfo, fileHandleIds.stream().distinct().map(handles::get).toList());
	}

	@Override
	public AuthorizationStatus canAccessRawFileHandles(UserInfo userInfo, Collection<FileHandle> fileHandles) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(fileHandles, "fileHandles");
		List<String> denied = fileHandles.stream()
				.filter(handle -> !AuthorizationUtils.isUserCreatorOrAdmin(userInfo, handle.getCreatedBy()))
				.map(FileHandle::getId).distinct().toList();
		if (denied.isEmpty()) {
			return AuthorizationStatus.authorized();
		}
		return AuthorizationStatus.accessDenied(String.format(AuthorizationUtils.FILE_HANDLE_UNAUTHORIZED_TEMPLATE,
				String.join(", ", denied), userInfo.getId().toString()));
	}

}
