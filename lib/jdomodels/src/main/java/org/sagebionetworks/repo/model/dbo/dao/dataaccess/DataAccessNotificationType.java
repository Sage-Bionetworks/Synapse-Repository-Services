package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.time.Period;

import org.sagebionetworks.repo.model.ApprovalState;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Type of notifications sent for access approvals.
 */
public enum DataAccessNotificationType {
	/**
	 * Notification sent after revocation
	 */
	REVOCATION(ApprovalState.REVOKED), 
	/**
	 * Notification sent 2 months before the expiration date
	 */
	FIRST_RENEWAL_REMINDER(ApprovalState.APPROVED, Period.ofMonths(2)), 
	/**
	 * Notification sent 1 month before the expiration date
	 */
	SECOND_RENEWAL_REMINDER(ApprovalState.APPROVED, Period.ofMonths(1));
	
	private ApprovalState expectedState;
	private Period reminderPeriod;
	
	private DataAccessNotificationType(ApprovalState expectedState) {
		this(expectedState, null);
	}
	
	private DataAccessNotificationType(ApprovalState expectedState, Period reminderPeriod) {
		ValidateArgument.required(expectedState, "expectedState");
		this.expectedState = expectedState;
		this.reminderPeriod = reminderPeriod;
	}
	
	public boolean isReminder() {
		return reminderPeriod != null;
	}
	
	public Period getReminderPeriod() {
		return reminderPeriod;
	}
	
	public ApprovalState getExpectedState() {
		return expectedState;
	}
}
