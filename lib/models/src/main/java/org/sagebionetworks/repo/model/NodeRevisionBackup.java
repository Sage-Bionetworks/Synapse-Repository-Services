package org.sagebionetworks.repo.model;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author jmhill
 *
 */
public class NodeRevisionBackup {
	
	// This is the first version.
	public static String XML_V_0	 = "0.0";
	public static String XML_V_1	 = "1.0";
	// We are currently on V1
	public static String CURRENT_XML_VERSION = XML_V_1;
	
	private String nodeId;
	private Long revisionNumber;
	private String label;
	private String comment;
	private String modifiedBy;
	private Long modifiedByPrincipalId;
	private Date modifiedOn;
	private String xmlVersion;
	/**
	 * Annotations now belong to a name-space so use namespaceAnnos.
	 * @deprecated since xml version 1.0
	 */
	@Deprecated 
	private Annotations annotations;
	/**
	 * Annotations now belong to a name-space so this map should be used instead 
	 * of the depreciated annotations.
	 */
	private NamedAnnotations namedAnnotations;
	private Map<String, Set<Reference>> references;

	/**
	 * The xml version that this object serialized to/from.
	 * @return
	 */
	public String getXmlVersion() {
		return xmlVersion;
	}
	
	/**
	 * The xml version that this object serialized to/from.
	 * @param xmlVersion
	 */
	public void setXmlVersion(String xmlVersion) {
		this.xmlVersion = xmlVersion;
	}
	
	public String getNodeId() {
		return nodeId;
	}
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
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
	public String getModifiedBy() {
		return modifiedBy;
	}
	public void setModifiedBy(String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}
	public Date getModifiedOn() {
		return modifiedOn;
	}
	public void setModifiedOn(Date modifiedOn) {
		this.modifiedOn = modifiedOn;
	}
	
	/**
	 * @deprecated use getNamespaceAnnos
	 * @return
	 */
	@Deprecated
	public Annotations getAnnotations() {
		return annotations;
	}
	/**
	 * @deprecated use #setNamespaceAnnos()
	 * @param annotations
	 */
	@Deprecated
	public void setAnnotations(Annotations annotations) {
		this.annotations = annotations;
	}
	
	public NamedAnnotations getNamedAnnotations() {
		return namedAnnotations;
	}
	public void setNamedAnnotations(NamedAnnotations namespaceAnnotations) {
		this.namedAnnotations = namespaceAnnotations;
	}
	
	/**
	 * @return the modifiedByPrincipalId
	 */
	public Long getModifiedByPrincipalId() {
		return modifiedByPrincipalId;
	}

	/**
	 * @param modifiedByPrincipalId the modifiedByPrincipalId to set
	 */
	public void setModifiedByPrincipalId(Long modifiedByPrincipalId) {
		this.modifiedByPrincipalId = modifiedByPrincipalId;
	}

	/**
	 * @return the references
	 */
	public Map<String, Set<Reference>> getReferences() {
		return references;
	}
	/**
	 * @param references the references to set
	 */
	public void setReferences(Map<String, Set<Reference>> references) {
		this.references = references;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((annotations == null) ? 0 : annotations.hashCode());
		result = prime * result + ((comment == null) ? 0 : comment.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result
				+ ((modifiedBy == null) ? 0 : modifiedBy.hashCode());
		result = prime
				* result
				+ ((modifiedByPrincipalId == null) ? 0 : modifiedByPrincipalId
						.hashCode());
		result = prime * result
				+ ((modifiedOn == null) ? 0 : modifiedOn.hashCode());
		result = prime
				* result
				+ ((namedAnnotations == null) ? 0 : namedAnnotations.hashCode());
		result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
		result = prime * result
				+ ((references == null) ? 0 : references.hashCode());
		result = prime * result
				+ ((revisionNumber == null) ? 0 : revisionNumber.hashCode());
		result = prime * result
				+ ((xmlVersion == null) ? 0 : xmlVersion.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NodeRevisionBackup other = (NodeRevisionBackup) obj;
		if (annotations == null) {
			if (other.annotations != null)
				return false;
		} else if (!annotations.equals(other.annotations))
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
		if (modifiedByPrincipalId == null) {
			if (other.modifiedByPrincipalId != null)
				return false;
		} else if (!modifiedByPrincipalId.equals(other.modifiedByPrincipalId))
			return false;
		if (modifiedOn == null) {
			if (other.modifiedOn != null)
				return false;
		} else if (!modifiedOn.equals(other.modifiedOn))
			return false;
		if (namedAnnotations == null) {
			if (other.namedAnnotations != null)
				return false;
		} else if (!namedAnnotations.equals(other.namedAnnotations))
			return false;
		if (nodeId == null) {
			if (other.nodeId != null)
				return false;
		} else if (!nodeId.equals(other.nodeId))
			return false;
		if (references == null) {
			if (other.references != null)
				return false;
		} else if (!references.equals(other.references))
			return false;
		if (revisionNumber == null) {
			if (other.revisionNumber != null)
				return false;
		} else if (!revisionNumber.equals(other.revisionNumber))
			return false;
		if (xmlVersion == null) {
			if (other.xmlVersion != null)
				return false;
		} else if (!xmlVersion.equals(other.xmlVersion))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "NodeRevisionBackup [annotations=" + annotations + ", comment="
				+ comment + ", label=" + label + ", modifiedBy=" + modifiedBy
				+ ", modifiedOn=" + modifiedOn + ", namedAnnotations="
				+ namedAnnotations + ", nodeId=" + nodeId + ", references="
				+ references + ", revisionNumber=" + revisionNumber
				+ ", xmlVersion=" + xmlVersion + "]";
	}
	
	
}
