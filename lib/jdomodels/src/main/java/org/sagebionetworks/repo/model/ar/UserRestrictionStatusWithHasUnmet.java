package org.sagebionetworks.repo.model.ar;

import org.sagebionetworks.repo.model.dbo.entity.UserEntityPermissionsState;

import java.util.Objects;

/**
 * The user's access restriction status and met access requirement for a single subject. This object
 * includes information on all access restrictions on a subject and
 * has unmet access status for the subject.
 *
 */
public class UserRestrictionStatusWithHasUnmet {
    private boolean hasUnmet = false;
    private UsersRestrictionStatus usersRestrictionStatus;

    /**
     * The user met access requirement if either has exemption or has approvals on all the AR's
     * user is exempted if it is data contributor and has exemption eligibility on AR
     * @param userEntityPermissionsState
     * @param usersRestrictionStatus
     */
    public UserRestrictionStatusWithHasUnmet(UserEntityPermissionsState userEntityPermissionsState,
                                             UsersRestrictionStatus usersRestrictionStatus) {
        boolean isUserDataContributor = userEntityPermissionsState.hasUpdate() && userEntityPermissionsState.hasDelete();
        usersRestrictionStatus.getAccessRestrictions().forEach(restriction -> {
            boolean isExemptionEligible = restriction.isExemptionEligible();
            boolean isUserExempted = isUserDataContributor && isExemptionEligible;
            if (hasUnmet) {
                return;
            }
            this.hasUnmet = !isUserExempted && restriction.isUnmet();
        });

        this.usersRestrictionStatus = usersRestrictionStatus;
    }

    /**
     * @return the hasUnmet True if the user has unmet access restrictions for this
     *         subject.
     */
    public boolean hasUnmet() {
        return hasUnmet;
    }

    /**
     * @return usersRestrictionStatus
     */
    public UsersRestrictionStatus getUsersRestrictionStatus() {
        return usersRestrictionStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserRestrictionStatusWithHasUnmet other = (UserRestrictionStatusWithHasUnmet) o;
        return hasUnmet == other.hasUnmet && Objects.equals(usersRestrictionStatus, other.usersRestrictionStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hasUnmet, usersRestrictionStatus);
    }

    @Override
    public String toString() {
        return "UserRestrictionStatusWithHasUnmet [hasUnmet=" + hasUnmet +
                ", usersRestrictionStatus=" + usersRestrictionStatus + "]";
    }
}
