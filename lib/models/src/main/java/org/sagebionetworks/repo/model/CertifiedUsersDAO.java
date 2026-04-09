package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.web.NotFoundException;

import java.util.Set;

public interface CertifiedUsersDAO {

    /**
     * Add the given user to the Certified Users table. If the user is already added, this method has no effect.
     *
     * @param userId The ID of the user to add to the Certified Users group.
     * @param isIndividual Whether the user is an individual or a group. Only individual users can be added as certified users.
     */
    void addCertifiedUser(Long userId, boolean isIndividual) throws IllegalArgumentException;

    /**
     * Remove the given user from the Certified Users table.
     *
     * @param userId The ID of the user to be removed from the Certified Users.
     */

    void removeCertifiedUser(Long userId);

    /**
     * Returns true if all the given users are certified users.
     *
     * @param userIds The IDs of the users to check for certification.
     */

    boolean areAllCertifiedUsers(Set<String> userIds);

    /**
     * Returns true if the given users is certified users.
     *
     * @param userId The ID of the user to check for certification.
     */

    boolean isCertifiedUser(String userId);

}
