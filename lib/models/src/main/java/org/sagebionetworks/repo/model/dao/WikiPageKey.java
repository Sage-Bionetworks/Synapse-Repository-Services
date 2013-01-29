package org.sagebionetworks.repo.model.dao;

import org.sagebionetworks.repo.model.message.ObjectType;

/**
 * It takes a three part key to access a wiki page.  This object encapsulates all three parts of a key in an immutable object.
 * @author John
 *
 */
public class WikiPageKey {
	
	private String ownerObjectId;
	private ObjectType ownerObjectType;
	private String wikiPageId;
	
	/**
	 * 
	 * @param ownerObjectId
	 * @param ownerObjectType
	 * @param wikiPageId
	 */
	public WikiPageKey(String ownerObjectId, ObjectType ownerObjectType, String wikiPageId) {
		super();
		if(ownerObjectId == null) throw new IllegalArgumentException("OwnerObjectId cannot be null");
		if(ownerObjectType == null) throw new IllegalArgumentException("ownerObjectType cannot be null");
		if(wikiPageId == null) throw new IllegalArgumentException("wikiPageId cannot be null");
		this.ownerObjectId = ownerObjectId;
		this.ownerObjectType = ownerObjectType;
		this.wikiPageId = wikiPageId;
	}
	public String getOwnerObjectId() {
		return ownerObjectId;
	}
	public ObjectType getOwnerObjectType() {
		return ownerObjectType;
	}
	public String getWikiPageId() {
		return wikiPageId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((ownerObjectId == null) ? 0 : ownerObjectId.hashCode());
		result = prime * result
				+ ((ownerObjectType == null) ? 0 : ownerObjectType.hashCode());
		result = prime * result
				+ ((wikiPageId == null) ? 0 : wikiPageId.hashCode());
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
		WikiPageKey other = (WikiPageKey) obj;
		if (ownerObjectId == null) {
			if (other.ownerObjectId != null)
				return false;
		} else if (!ownerObjectId.equals(other.ownerObjectId))
			return false;
		if (ownerObjectType != other.ownerObjectType)
			return false;
		if (wikiPageId == null) {
			if (other.wikiPageId != null)
				return false;
		} else if (!wikiPageId.equals(other.wikiPageId))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "WikiPageKey [ownerObjectId=" + ownerObjectId
				+ ", ownerObjectType=" + ownerObjectType + ", wikiPageId="
				+ wikiPageId + "]";
	}
	
}
