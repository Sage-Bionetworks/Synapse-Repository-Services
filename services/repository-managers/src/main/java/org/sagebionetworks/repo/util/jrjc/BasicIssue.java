package org.sagebionetworks.repo.util.jrjc;

import java.util.Map;

public class BasicIssue {
    private String summary;
    private String projectId;
    private Long issueTypeId;
    private Map<String, Object> customFields;

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public Long getIssueTypeId() {
        return issueTypeId;
    }

    public void setIssueTypeId(Long issueTypeId) {
        this.issueTypeId = issueTypeId;
    }

    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((summary == null) ? 0 : summary.hashCode());
        result = prime * result + ((projectId == null) ? 0 : projectId.hashCode());
        result = prime * result + ((issueTypeId == null) ? 0 : issueTypeId.hashCode());
        result = prime * result + ((customFields == null) ? 0 : customFields.hashCode());
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
        BasicIssue other = (BasicIssue) obj;
        if (summary == null) {
            if (other.summary != null)
                return false;
        } else if (!summary.equals(other.summary))
            return false;
        if (projectId == null) {
            if (other.projectId != null)
                return false;
        } else if (!projectId.equals(other.projectId))
            return false;
        if (issueTypeId == null) {
            if (other.issueTypeId != null)
                return false;
        } else if (!issueTypeId.equals(other.issueTypeId))
            return false;
        if (customFields == null) {
            if (other.customFields != null)
                return false;
        } else if (!customFields.equals(other.customFields))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "BasicIssue [summary=" + summary + ", issueTypeId=" + issueTypeId
            + ", projectId=" + projectId + ", fields=" + customFields + "]";
    }

}


