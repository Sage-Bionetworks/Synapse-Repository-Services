package org.sagebionetworks.repo.model.drs;


import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.util.ValidateArgument;

import java.util.Objects;

/**
 * Immutable representation of id to fetch file.
 * Use {@linkplain AccessIdBuilder} to create new instances of this
 * class.
 */
public class AccessId {
    private final static String DELIMITER = "_";
    private final static String SYN = "syn";
    private final FileHandleAssociateType associateType;
    private final IdAndVersion synapseIdWithVersion;
    private final String fileHandleId;

    /**
     * @param accessIdBuilder
     */
    private AccessId(final AccessIdBuilder accessIdBuilder) {
        this.associateType = accessIdBuilder.associateType;
        this.synapseIdWithVersion = accessIdBuilder.synapseIdWithVersion;
        this.fileHandleId = accessIdBuilder.fileHandleId;
    }

    /**
     * The synapse id with version
     *
     * @return
     */
    public IdAndVersion getSynapseIdWithVersion() {
        return this.synapseIdWithVersion;
    }


    /**
     * Enumeration of all possible objects types that can be associated with a file Handle
     *
     * @return
     */
    public FileHandleAssociateType getAssociateType() {
        return this.associateType;
    }

    /**
     * The fileHandle id for the file
     *
     * @return
     */
    public String getFileHandleId() {
        return this.fileHandleId;
    }

    /**
     * Parse the provided string into AccessId
     *
     * @param toDecode
     * @return AccessId
     */
    public static AccessId decode(final String toDecode) throws IllegalArgumentException {
        try {
            if (StringUtils.isEmpty(toDecode)) {
                throw new IllegalArgumentException("AccessId must not be null or empty.");
            }

            final String[] array = toDecode.trim().split(DELIMITER);
            if (array.length != 3) {
                throw new IllegalArgumentException("Invalid accessId");
            }

            final AccessIdBuilder builder = new AccessIdBuilder();
            builder.setAssociateType(getFileHandleAssociateType(array[0]));
            builder.setSynapseIdWithVersion(getIdAndVersion(array[1]));
            builder.setFileHandleId(getFileHandleID(array[2]));
            return builder.build();
        } catch (final Exception exception) {
            throw new IllegalArgumentException(exception.getMessage());
        }
    }

    private static FileHandleAssociateType getFileHandleAssociateType(final String associationType) {
        try {
            return FileHandleAssociateType.valueOf(associationType);
        } catch (final Exception exception) {
            throw new IllegalArgumentException("AccessId must contain a valid file handle association type.");
        }
    }

    private static IdAndVersion getIdAndVersion(final String synIdWithVersion) {
        if (!synIdWithVersion.startsWith("syn")) {
            throw new IllegalArgumentException("AccessId must contain syn prefix with id and version.eg FileEntity_syn123.1_12345");
        }
        return IdAndVersion.parse(synIdWithVersion);
    }

    private static String getFileHandleID(final String fileHandleId) {
        try {
            return String.valueOf(Long.parseLong(fileHandleId));
        } catch (final Exception exception) {
            throw new IllegalArgumentException("AccessId must contain valid file handle id.");
        }
    }

    public static String encode(final AccessId accessId) {
        return accessId.associateType.name() + DELIMITER +
                SYN + accessId.synapseIdWithVersion.getId() + "." + accessId.getSynapseIdWithVersion().getVersion().get() +
                DELIMITER + accessId.fileHandleId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AccessId accessId = (AccessId) o;
        return this.associateType == accessId.associateType && Objects.equals(this.synapseIdWithVersion, accessId.synapseIdWithVersion)
                && Objects.equals(this.fileHandleId, accessId.fileHandleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.associateType, this.synapseIdWithVersion, this.fileHandleId);
    }

    public static class AccessIdBuilder {
        FileHandleAssociateType associateType;
        IdAndVersion synapseIdWithVersion;
        String fileHandleId;

        public AccessIdBuilder setSynapseIdWithVersion(final IdAndVersion synapseIdWithVersion) {
            this.synapseIdWithVersion = synapseIdWithVersion;
            return this;
        }

        public AccessIdBuilder setAssociateType(final FileHandleAssociateType associateType) {
            this.associateType = associateType;
            return this;
        }

        public AccessIdBuilder setFileHandleId(final String fileHandleId) {
            this.fileHandleId = fileHandleId;
            return this;
        }

        public AccessId build() {
            ValidateArgument.required(this.associateType, "fileHandleAssociationType");
            ValidateArgument.required(this.synapseIdWithVersion, "synapseIdWithVersion");
            ValidateArgument.required(this.fileHandleId, "fileHandleId");
            validateSynapseIdWithVersion(this.synapseIdWithVersion);
            return new AccessId(this);
        }

        private void validateSynapseIdWithVersion(final IdAndVersion synapseIdWithVersion) {
            if (!synapseIdWithVersion.getVersion().isPresent()) {
                throw new IllegalArgumentException("Synapse id should include version. e.g syn123.1");
            }
        }
    }
}
