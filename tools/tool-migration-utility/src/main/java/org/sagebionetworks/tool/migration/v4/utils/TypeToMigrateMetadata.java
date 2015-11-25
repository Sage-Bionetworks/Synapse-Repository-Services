package org.sagebionetworks.tool.migration.v4.utils;

import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;

public class TypeToMigrateMetadata {
	
	private MigrationType type;
	private Long srcMinId;
	private Long srcMaxId;
	private Long srcCount;
	private Long destMinId;
	private Long destMaxId;
	private Long destCount;
	
	public TypeToMigrateMetadata() {
		
	};
	
	public TypeToMigrateMetadata(MigrationType t, long sMin, long sMax, long sCnt, long dMin, long dMax, long dCnt) {
		this.setType(t);
		this.setSrcMinId(sMin);
		this.setSrcMaxId(sMax);
		this.setSrcCount(sCnt);
		this.setDestMinId(dMin);
		this.setDestMaxId(dMax);
		this.setDestCount(dCnt);
	}

	public MigrationType getType() {
		return type;
	}

	public void setType(MigrationType type) {
		this.type = type;
	}

	public Long getSrcMinId() {
		return srcMinId;
	}

	public void setSrcMinId(Long srcMinId) {
		this.srcMinId = srcMinId;
	}

	public Long getSrcMaxId() {
		return srcMaxId;
	}

	public void setSrcMaxId(Long srcMaxId) {
		this.srcMaxId = srcMaxId;
	}

	public Long getSrcCount() {
		return srcCount;
	}

	public void setSrcCount(Long srcCount) {
		this.srcCount = srcCount;
	}

	public Long getDestMinId() {
		return destMinId;
	}

	public void setDestMinId(Long destMinId) {
		this.destMinId = destMinId;
	}

	public Long getDestMaxId() {
		return destMaxId;
	}

	public void setDestMaxId(Long destMaxId) {
		this.destMaxId = destMaxId;
	}

	public Long getDestCount() {
		return destCount;
	}

	public void setDestCount(Long destCount) {
		this.destCount = destCount;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = ((prime*result)+((destCount == null)? 0 :destCount.hashCode()));
		result = ((prime*result)+((destMaxId == null)? 0 :destMaxId.hashCode()));
		result = ((prime*result)+((destMinId == null)? 0 :destMinId.hashCode()));
		result = ((prime*result)+((srcCount == null)? 0 :srcCount.hashCode()));
		result = ((prime*result)+((srcMaxId == null)? 0 :srcMaxId.hashCode()));
		result = ((prime*result)+((srcMinId == null)? 0 :srcMinId.hashCode()));
		result = ((prime*result)+((type == null)? 0 :type.hashCode()));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass()!= obj.getClass()) {
			return false;
		}
		
		TypeToMigrateMetadata other = ((TypeToMigrateMetadata) obj);
		if (destCount == null) {
			if (other.destCount!= null) {
				return false;
			}
		} else {
			if (!destCount.equals(other.destCount)) {
				return false;
			}
		}
		if (srcCount == null) {
			if (other.srcCount!= null) {
				return false;
			}
		} else {
			if (!srcCount.equals(other.srcCount)) {
				return false;
			}
		}
		
		if (destMaxId == null) {
			if (other.destMaxId!= null) {
				return false;
			}
		} else {
			if (!destMaxId.equals(other.destMaxId)) {
				return false;
			}
		}
		
		if (srcMaxId == null) {
			if (other.srcMaxId!= null) {
				return false;
			}
		} else {
			if (!srcMaxId.equals(other.srcMaxId)) {
				return false;
			}
		}
		
		if (destMinId == null) {
			if (other.destMinId!= null) {
				return false;
			}
		} else {
			if (!destMinId.equals(other.destMinId)) {
				return false;
			}
		}
		
		if (srcMinId == null) {
			if (other.srcMinId!= null) {
				return false;
			}
		} else {
			if (!srcMinId.equals(other.srcMinId)) {
				return false;
			}
		}
		
		
		if (type == null) {
			if (other.type!= null) {
				return false;
			}
		} else {
			if (!type.equals(other.type)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Adds toString method to pojo.
	 * returns a string
	 * 
	 * @return
	 */
	@Override
	public String toString() {
		StringBuilder result;
		result = new StringBuilder();
		result.append("");
		result.append("org.sagebionetworks.repo.model.migration.TypeToMigrateMetadata");
		result.append(" [");
		result.append("destCount=");
		result.append(destCount);
		result.append(" ");
		result.append("destMaxId=");
		result.append(destMaxId);
		result.append(" ");
		result.append("destMinId=");
		result.append(destMinId);
		result.append(" ");
		result.append("srcCount=");
		result.append(srcCount);
		result.append(" ");
		result.append("srcMaxId=");
		result.append(srcMaxId);
		result.append(" ");
		result.append("srcMinId=");
		result.append(srcMinId);
		result.append(" ");
		result.append("type=");
		result.append(type);
		result.append(" ");
		result.append("]");
		return result.toString();
	}

}
