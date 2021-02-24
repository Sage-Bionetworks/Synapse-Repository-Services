package org.sagebionetworks.repo.model.files;

import java.time.Instant;
import java.util.Objects;

/**
 * DTO for the file handle association scanner job status
 *
 */
public class FilesScannerStatus {

	private Long id;
	private FilesScannerState state;
	private Instant startedOn;
	private Instant updatedOn;
	private Long jobsCount;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public FilesScannerState getState() {
		return state;
	}

	public void setState(FilesScannerState state) {
		this.state = state;
	}

	public Instant getStartedOn() {
		return startedOn;
	}

	public void setStartedOn(Instant startedOn) {
		this.startedOn = startedOn;
	}

	public Instant getUpdatedOn() {
		return updatedOn;
	}

	public void setUpdatedOn(Instant updatedOn) {
		this.updatedOn = updatedOn;
	}

	public Long getJobsCount() {
		return jobsCount;
	}

	public void setJobsCount(Long jobsCount) {
		this.jobsCount = jobsCount;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, jobsCount, startedOn, state, updatedOn);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		FilesScannerStatus other = (FilesScannerStatus) obj;
		return Objects.equals(id, other.id) && Objects.equals(jobsCount, other.jobsCount) && Objects.equals(startedOn, other.startedOn)
				&& state == other.state && Objects.equals(updatedOn, other.updatedOn);
	}

	@Override
	public String toString() {
		return "FilesScannerStatus [id=" + id + ", state=" + state + ", startedOn=" + startedOn + ", updatedOn=" + updatedOn
				+ ", jobsCount=" + jobsCount + "]";
	}

}
