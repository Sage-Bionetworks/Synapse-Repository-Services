package org.sagebionetworks.repo.model.drs;

import org.sagebionetworks.repo.model.file.FileHandleAssociateType;

/**
 * Simple builder for an Access ID
 */
public class AccessIdBuilder {

    String synapseIdWithVersion;
    FileHandleAssociateType associateType;
    String fileHandleId;

    public AccessIdBuilder setSynapseIdWithVersion(String synapseIdWithVersion) {
        this.synapseIdWithVersion = synapseIdWithVersion;
        return this;
    }

    public AccessIdBuilder setAssociateType(FileHandleAssociateType associateType) {
        this.associateType = associateType;
        return this;
    }

    public AccessIdBuilder setFileHandleId(String fileHandleId) {
        this.fileHandleId = fileHandleId;
        return this;
    }

    public AccessId build() {
        return new AccessId(associateType, synapseIdWithVersion, fileHandleId);
    }
}
