package org.sagebionetworks.repo.model.drs;


import org.sagebionetworks.repo.model.file.FileHandleAssociateType;

import java.util.Objects;

/**
 * Immutable representation of id to fetch file.
 * Use {@linkplain AccessIdBuilder} to create new instances of this
 * class. To parse an AccessId from a string use {@linkplain AccessIdParser}
 */
public class AccessId {
    final FileHandleAssociateType associateType;
    final String synapseIdWithVersion;
    final String fileHandleId;

    /**
     * @param associateType
     * @param synapseIdWithVersion
     * @param fileHandleId
     */
    public AccessId(FileHandleAssociateType associateType, String synapseIdWithVersion, String fileHandleId) {
        if (synapseIdWithVersion == null || associateType == null || fileHandleId == null) {
            throw new IllegalArgumentException("Required field missing.");
        }
        this.associateType = associateType;
        this.synapseIdWithVersion = synapseIdWithVersion;
        this.fileHandleId = fileHandleId;
    }

    /**
     * The synapse id has prefix syn and concatenated with version using delimiter (.)
     *
     * @return
     */
    public String getSynapseIdWithVersion() {
        return synapseIdWithVersion;
    }


    /**
     * Enumeration of all possible objects types that can be associated with a file Handle
     *
     * @return
     */
    public FileHandleAssociateType getAssociateType() {
        return associateType;
    }

    /**
     * The fileHandle id for the file
     *
     * @return
     */
    public String getFileHandleId() {
        return fileHandleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccessId accessId = (AccessId) o;
        return Objects.equals(synapseIdWithVersion, accessId.synapseIdWithVersion)
                && associateType == accessId.associateType
                && Objects.equals(fileHandleId, accessId.fileHandleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(synapseIdWithVersion, associateType, fileHandleId);
    }

    @Override
    public String toString() {
        return associateType.name() + "_" + synapseIdWithVersion + "_" + fileHandleId;
    }

    /**
     * Parse the provided string into AccessId
     *
     * @param toParse
     * @return
     */
    public static AccessId parse(String toParse) {
        return AccessIdParser.parseAccessId(toParse);
    }
}
