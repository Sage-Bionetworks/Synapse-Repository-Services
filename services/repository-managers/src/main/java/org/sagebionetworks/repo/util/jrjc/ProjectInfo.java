package org.sagebionetworks.repo.util.jrjc;

public class ProjectInfo {

    private Long issueTypeId;
    private String projectId;

    public ProjectInfo(String projectId, Long issueTypeId) {
        this.setIssueTypeId(issueTypeId);
        this.setProjectId(projectId);
    }

    public Long getIssueTypeId() {
        return issueTypeId;
    }

    public void setIssueTypeId(Long issueTypeId) {
        this.issueTypeId = issueTypeId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((projectId == null) ? 0 : projectId.hashCode());
        result = prime * result + ((issueTypeId == null) ? 0 : issueTypeId.hashCode());
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
        ProjectInfo other = (ProjectInfo) obj;
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
        return true;
    }

    @Override
    public String toString() {
        return "ProjectInfo [projectId=" + projectId + ", issueTypeId=" + issueTypeId + "]";
    }

}
