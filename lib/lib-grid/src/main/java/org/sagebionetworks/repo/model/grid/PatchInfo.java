package org.sagebionetworks.repo.model.grid;

import java.sql.Timestamp;
import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

/**
 * Information about a grid patch.
 */
public class PatchInfo {

	private String sessionId;
	private LogicalTimestamp patchId;
	private Timestamp createdOn;
	private Timestamp expiresOn;
	private String s3Key;
	private Long sizeBytes;
	private Long span;
	private String bodySha256;

	public String getSessionId() {
		return sessionId;
	}

	public PatchInfo setSessionId(String sessionId) {
		this.sessionId = sessionId;
		return this;
	}

	public LogicalTimestamp getPatchId() {
		return patchId;
	}

	public PatchInfo setPatchId(LogicalTimestamp patchId) {
		this.patchId = patchId;
		return this;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public PatchInfo setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
		return this;
	}

	public Timestamp getExpiresOn() {
		return expiresOn;
	}

	public PatchInfo setExpiresOn(Timestamp expiresOn) {
		this.expiresOn = expiresOn;
		return this;
	}

	public String getS3Key() {
		return s3Key;
	}

	public PatchInfo setS3Key(String s3Key) {
		this.s3Key = s3Key;
		return this;
	}

	public Long getSizeBytes() {
		return sizeBytes;
	}

	public PatchInfo setSizeBytes(Long sizeBytes) {
		this.sizeBytes = sizeBytes;
		return this;
	}

	/**
	 * @return The number of sequence numbers consumed by this patch's operations.
	 *         Null for patches saved before span was tracked.
	 */
	public Long getSpan() {
		return span;
	}

	public PatchInfo setSpan(Long span) {
		this.span = span;
		return this;
	}

	/**
	 * @return Hex SHA-256 of the UTF-8 patch body. Null for patches saved before
	 *         the hash was tracked.
	 */
	public String getBodySha256() {
		return bodySha256;
	}

	public PatchInfo setBodySha256(String bodySha256) {
		this.bodySha256 = bodySha256;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(bodySha256, createdOn, expiresOn, patchId, s3Key, sessionId, sizeBytes, span);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PatchInfo other = (PatchInfo) obj;
		return Objects.equals(bodySha256, other.bodySha256) && Objects.equals(createdOn, other.createdOn)
				&& Objects.equals(expiresOn, other.expiresOn) && Objects.equals(patchId, other.patchId)
				&& Objects.equals(s3Key, other.s3Key) && Objects.equals(sessionId, other.sessionId)
				&& Objects.equals(sizeBytes, other.sizeBytes) && Objects.equals(span, other.span);
	}

	@Override
	public String toString() {
		return "PatchInfo [sessionId=" + sessionId + ", patchId=" + patchId + ", createdOn=" + createdOn
				+ ", expiresOn=" + expiresOn + ", s3Key=" + s3Key + ", sizeBytes=" + sizeBytes + ", span=" + span
				+ ", bodySha256=" + bodySha256 + "]";
	}

}
