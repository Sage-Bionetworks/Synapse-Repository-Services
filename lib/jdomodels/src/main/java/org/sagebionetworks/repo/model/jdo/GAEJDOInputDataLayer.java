package org.sagebionetworks.repo.model.jdo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

@PersistenceCapable(detachable = "false")
public class GAEJDOInputDataLayer extends
		GAEJDODatasetLayer<GAEJDOInputDataLayer> {

	// this is a link to the next revision of the layer (if any)
	@Persistent(dependent = "true")
	private GAEJDOInputDataLayer nextVersion;

	@Persistent
	private String type;

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

	public GAEJDOInputDataLayer getNextVersion() {
		return nextVersion;
	}

	public void setNextVersion(GAEJDOInputDataLayer nextVersion) {
		this.nextVersion = nextVersion;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
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

	public static Collection<String> getPrimaryFields() {
		List<String> ans = new ArrayList<String>(
				GAEJDODatasetLayer.getPrimaryFields());
		ans.addAll(Arrays.asList(new String[] { "type", "tissueType",
				"platform", "processingFacility", "qcBy", "qcDate" }));
		return ans;
	}

}
