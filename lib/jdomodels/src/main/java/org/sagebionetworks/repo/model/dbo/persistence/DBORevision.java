package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_OWNER_NODE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * The DatabaseObject for Revision.
 * 
 * @author jmhill
 *
 */
public class DBORevision implements DatabaseObject<DBORevision> {
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("owner", COL_REVISION_OWNER_NODE, true),
		new FieldColumn("revisionNumber", COL_REVISION_NUMBER, true),
		new FieldColumn("label", COL_REVISION_LABEL),
		new FieldColumn("comment", COL_REVISION_COMMENT),
		new FieldColumn("modifiedBy", COL_REVISION_MODIFIED_BY),
		new FieldColumn("modifiedOn", COL_REVISION_MODIFIED_ON),
		new FieldColumn("annotations", COL_REVISION_ANNOS_BLOB),
		new FieldColumn("references", COL_REVISION_REFS_BLOB),
		};

	@Override
	public TableMapping<DBORevision> getTableMapping() {
		return new TableMapping<DBORevision>(){
			@Override
			public DBORevision mapRow(ResultSet rs, int rowNum)	throws SQLException {
				DBORevision rev = new DBORevision();
				rev.setOwner(rs.getLong(COL_REVISION_OWNER_NODE));
				rev.setRevisionNumber(rs.getLong(COL_REVISION_NUMBER));
				rev.setLabel(rs.getString(COL_REVISION_LABEL));
				rev.setComment(rs.getString(COL_REVISION_COMMENT));
				rev.setModifiedBy(rs.getLong(COL_REVISION_MODIFIED_BY));
				rev.setModifiedOn(rs.getLong(COL_REVISION_MODIFIED_ON));
				java.sql.Blob blob = rs.getBlob(COL_REVISION_ANNOS_BLOB);
				if(blob != null){
					rev.setAnnotations(blob.getBytes(1, (int) blob.length()));
				}
				blob = rs.getBlob(COL_REVISION_REFS_BLOB);
				if(blob != null){
					rev.setReferences(blob.getBytes(1, (int) blob.length()));
				}
				return rev;
			}

			@Override
			public String getTableName() {
				return TABLE_REVISION;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_REVISION;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBORevision> getDBOClass() {
				return DBORevision.class;
			}};
	}
	
	private Long owner;
	private Long revisionNumber;
	private String label;
	private String comment;
	private Long modifiedBy;
	private Long modifiedOn;
	private byte[] annotations;
	private byte[] references;

	public Long getOwner() {
		return owner;
	}
	public void setOwner(Long owner) {
		this.owner = owner;
	}
	public Long getRevisionNumber() {
		return revisionNumber;
	}
	public void setRevisionNumber(Long revisionNumber) {
		this.revisionNumber = revisionNumber;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	public Long getModifiedBy() {
		return modifiedBy;
	}
	public void setModifiedBy(Long modifiedBy) {
		this.modifiedBy = modifiedBy;
	}
	public Long getModifiedOn() {
		return modifiedOn;
	}
	public void setModifiedOn(Long modifiedOn) {
		this.modifiedOn = modifiedOn;
	}
	public byte[] getAnnotations() {
		return annotations;
	}
	public void setAnnotations(byte[] annotations) {
		this.annotations = annotations;
	}
	public byte[] getReferences() {
		return references;
	}
	public void setReferences(byte[] references) {
		this.references = references;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(annotations);
		result = prime * result + ((comment == null) ? 0 : comment.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result
				+ ((modifiedBy == null) ? 0 : modifiedBy.hashCode());
		result = prime * result
				+ ((modifiedOn == null) ? 0 : modifiedOn.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		result = prime * result + Arrays.hashCode(references);
		result = prime * result
				+ ((revisionNumber == null) ? 0 : revisionNumber.hashCode());
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
		DBORevision other = (DBORevision) obj;
		if (!Arrays.equals(annotations, other.annotations))
			return false;
		if (comment == null) {
			if (other.comment != null)
				return false;
		} else if (!comment.equals(other.comment))
			return false;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
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
		if (owner == null) {
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
			return false;
		if (!Arrays.equals(references, other.references))
			return false;
		if (revisionNumber == null) {
			if (other.revisionNumber != null)
				return false;
		} else if (!revisionNumber.equals(other.revisionNumber))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "DBORevision [owner=" + owner + ", revisionNumber="
				+ revisionNumber + ", label=" + label + ", comment=" + comment
				+ ", modifiedBy=" + modifiedBy + ", modifiedOn=" + modifiedOn
				+ ", annotations=" + Arrays.toString(annotations)
				+ ", references=" + Arrays.toString(references) + "]";
	}

}
