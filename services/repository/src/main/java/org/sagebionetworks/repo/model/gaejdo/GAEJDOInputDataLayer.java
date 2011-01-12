package org.sagebionetworks.repo.model.gaejdo;

import java.util.Date;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

@PersistenceCapable(detachable = "true")
public class GAEJDOInputDataLayer extends GAEJDODatasetLayer {

	public enum DataType { PHENOTYPE, EXPRESSION, GENOTYPE, SEQUENCE }
	
	@Persistent
	private DataType type;
	
	@Persistent
	private String tissueType;
	
	@Persistent
	private String platform; // e.g. Affymetrix, Illumina
	
	@Persistent
	private String processingFacility;
	
	@Persistent
	private String qcBy;
	
	@Persistent
	private Date qcDate;
	
	public DataType getType() {
		return type;
	}
	public void setType(DataType type) {
		this.type = type;
	}
	public String getTissueType() {
		return tissueType;
	}
	public void setTissueType(String tissueType) {
		this.tissueType = tissueType;
	}
	public String getPlatform() {
		return platform;
	}
	public void setPlatform(String platform) {
		this.platform = platform;
	}
	public String getProcessingFacility() {
		return processingFacility;
	}
	public void setProcessingFacility(String processingFacility) {
		this.processingFacility = processingFacility;
	}
	public String getQcBy() {
		return qcBy;
	}
	public void setQcBy(String qcBy) {
		this.qcBy = qcBy;
	}
	public Date getQcDate() {
		return qcDate;
	}
	public void setQcDate(Date qcDate) {
		this.qcDate = qcDate;
	}
	
}
