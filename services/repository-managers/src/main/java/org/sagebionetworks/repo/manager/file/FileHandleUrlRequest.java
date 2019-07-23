package org.sagebionetworks.repo.manager.file;

import java.util.Objects;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.util.ValidateArgument;

public class FileHandleUrlRequest {

	private UserInfo userInfo;
	private FileHandleAssociateType associationType;
	private String associationId;
	private String fileHandleId;

	public FileHandleUrlRequest(UserInfo userInfo, String fileHandleId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(fileHandleId, "fileHandleId");
		this.userInfo = userInfo;
		this.fileHandleId = fileHandleId;
	}

	public FileHandleUrlRequest withAssociation(FileHandleAssociateType associationType, String associationId) {
		ValidateArgument.required(associationType, "associationType");
		ValidateArgument.required(associationId, "associationId");
		this.associationType = associationType;
		this.associationId = associationId;
		return this;
	}

	public UserInfo getUserInfo() {
		return userInfo;
	}

	public boolean hasAssociation() {
		return associationType != null && associationId != null;
	}

	public FileHandleAssociateType getAssociationType() {
		return associationType;
	}

	public String getAssociationId() {
		return associationId;
	}

	public String getFileHandleId() {
		return fileHandleId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(associationId, associationType, fileHandleId, userInfo);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		FileHandleUrlRequest other = (FileHandleUrlRequest) obj;
		return Objects.equals(associationId, other.associationId) && associationType == other.associationType
				&& Objects.equals(fileHandleId, other.fileHandleId)
				&& Objects.equals(userInfo, other.userInfo);
	}

}
