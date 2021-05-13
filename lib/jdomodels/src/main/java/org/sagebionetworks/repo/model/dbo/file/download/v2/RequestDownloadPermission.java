package org.sagebionetworks.repo.model.dbo.file.download.v2;

import java.util.Objects;

/**
 * The user will need to gain the download permission in oder to download this
 * file.
 *
 */
public class RequestDownloadPermission implements RequiredAction {

	/**
	 * The Id of the benefactor that currently governs the download of the entity.
	 */
	private long benefactorId;

	/**
	 * @return the benefactorId
	 */
	public long getBenefactorId() {
		return benefactorId;
	}

	/**
	 * @param benefactorId the benefactorId to set
	 */
	public RequestDownloadPermission withBenefactorId(long benefactorId) {
		this.benefactorId = benefactorId;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(benefactorId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof RequestDownloadPermission)) {
			return false;
		}
		RequestDownloadPermission other = (RequestDownloadPermission) obj;
		return benefactorId == other.benefactorId;
	}

	@Override
	public String toString() {
		return "RequestDownloadPermission [benefactorId=" + benefactorId + "]";
	}
	
	
}
