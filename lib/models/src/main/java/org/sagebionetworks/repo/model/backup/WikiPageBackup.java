package org.sagebionetworks.repo.model.backup;

import java.util.List;

/**
 * A backup copy of a wiki page.
 * 
 * @author John
 *
 */
public class WikiPageBackup {

	private Long ownerId;
	private String ownerType;
    private Long createdOn;
    private Long id;
    private Long modifiedOn;
    private String markdown;
    private String title;
    private Long createdBy;
    private String etag;
    private Long modifiedBy;
    private List<WikiPageAttachmentBackup> attachmentFileHandles;
    private Long parentWikiId;
	public Long getOwnerId() {
		return ownerId;
	}
	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}
	public String getOwnerType() {
		return ownerType;
	}
	public void setOwnerType(String ownerType) {
		this.ownerType = ownerType;
	}
	public Long getCreatedOn() {
		return createdOn;
	}
	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getModifiedOn() {
		return modifiedOn;
	}
	public void setModifiedOn(Long modifiedOn) {
		this.modifiedOn = modifiedOn;
	}
	public String getMarkdown() {
		return markdown;
	}
	public void setMarkdown(String markdown) {
		this.markdown = markdown;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public Long getCreatedBy() {
		return createdBy;
	}
	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}
	public String getEtag() {
		return etag;
	}
	public void setEtag(String etag) {
		this.etag = etag;
	}
	public Long getModifiedBy() {
		return modifiedBy;
	}
	public void setModifiedBy(Long modifiedBy) {
		this.modifiedBy = modifiedBy;
	}
	public List<WikiPageAttachmentBackup> getAttachmentFileHandles() {
		return attachmentFileHandles;
	}
	public void setAttachmentFileHandles(
			List<WikiPageAttachmentBackup> attachmentFileHandles) {
		this.attachmentFileHandles = attachmentFileHandles;
	}
	public Long getParentWikiId() {
		return parentWikiId;
	}
	public void setParentWikiId(Long parentWikiId) {
		this.parentWikiId = parentWikiId;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((attachmentFileHandles == null) ? 0 : attachmentFileHandles
						.hashCode());
		result = prime * result
				+ ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result
				+ ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((markdown == null) ? 0 : markdown.hashCode());
		result = prime * result
				+ ((modifiedBy == null) ? 0 : modifiedBy.hashCode());
		result = prime * result
				+ ((modifiedOn == null) ? 0 : modifiedOn.hashCode());
		result = prime * result + ((ownerId == null) ? 0 : ownerId.hashCode());
		result = prime * result
				+ ((ownerType == null) ? 0 : ownerType.hashCode());
		result = prime * result
				+ ((parentWikiId == null) ? 0 : parentWikiId.hashCode());
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WikiPageBackup other = (WikiPageBackup) obj;
		if (attachmentFileHandles == null) {
			if (other.attachmentFileHandles != null)
				return false;
		} else if (!attachmentFileHandles.equals(other.attachmentFileHandles))
			return false;
		if (createdBy == null) {
			if (other.createdBy != null)
				return false;
		} else if (!createdBy.equals(other.createdBy))
			return false;
		if (createdOn == null) {
			if (other.createdOn != null)
				return false;
		} else if (!createdOn.equals(other.createdOn))
			return false;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (markdown == null) {
			if (other.markdown != null)
				return false;
		} else if (!markdown.equals(other.markdown))
			return false;
		if (modifiedBy == null) {
			if (other.modifiedBy != null)
				return false;
		} else if (!modifiedBy.equals(other.modifiedBy))
			return false;
		if (modifiedOn == null) {
			if (other.modifiedOn != null)
				return false;
		} else if (!modifiedOn.equals(other.modifiedOn))
			return false;
		if (ownerId == null) {
			if (other.ownerId != null)
				return false;
		} else if (!ownerId.equals(other.ownerId))
			return false;
		if (ownerType == null) {
			if (other.ownerType != null)
				return false;
		} else if (!ownerType.equals(other.ownerType))
			return false;
		if (parentWikiId == null) {
			if (other.parentWikiId != null)
				return false;
		} else if (!parentWikiId.equals(other.parentWikiId))
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "WikiPageBackup [ownerId=" + ownerId + ", ownerType="
				+ ownerType + ", createdOn=" + createdOn + ", id=" + id
				+ ", modifiedOn=" + modifiedOn + ", markdown=" + markdown
				+ ", title=" + title + ", createdBy=" + createdBy + ", etag="
				+ etag + ", modifiedBy=" + modifiedBy
				+ ", attachmentFileHandles=" + attachmentFileHandles
				+ ", parentWikiId=" + parentWikiId + "]";
	}
    
}
