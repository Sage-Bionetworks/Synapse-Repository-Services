package org.sagebionetworks.repo.model.drs;


import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.util.ValidateArgument;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Immutable representation of id to fetch file.
 * Use {@linkplain Builder} to create new instances of this
 * class.
 */
public class AccessId {
    private final static String DELIMITER = "_";
    private final FileHandleAssociateType associateType;
    private final IdAndVersion synapseIdWithVersion;
    private final String fileHandleId;

    /**
     * @param associateType
     * @param synapseIdWithVersion
     * @param fileHandleId
     */
    private AccessId(final FileHandleAssociateType associateType, final IdAndVersion synapseIdWithVersion, final String fileHandleId) {
        this.associateType = associateType;
        this.synapseIdWithVersion = synapseIdWithVersion;
        this.fileHandleId = fileHandleId;
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
            final Builder builder = new Builder();
            final String[] array = toDecode.trim().split(DELIMITER);
            if (array.length == 1) {
                builder.setFileHandleId(getFileHandleID(array[0]));
            } else if (array.length == 3) {
                builder.setAssociateType(getFileHandleAssociateType(array[0]));
                builder.setSynapseIdWithVersion(IdAndVersion.parse(array[1]));
                builder.setFileHandleId(getFileHandleID(array[2]));
            } else {
                throw new IllegalArgumentException("Invalid accessId");
            }

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

    private static String getFileHandleID(final String fileHandleId) {
        try {
            return String.valueOf(Long.parseLong(fileHandleId));
        } catch (final Exception exception) {
            throw new IllegalArgumentException("AccessId must contain valid file handle id.");
        }
    }

    public String encode() {
        if (associateType == null && synapseIdWithVersion == null) {
            return fileHandleId;
        }

        final StringJoiner joiner = new StringJoiner(DELIMITER);
        joiner.add(associateType.name());
        joiner.add(synapseIdWithVersion.toString());
        joiner.add(fileHandleId);
        return joiner.toString();
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

    public static class Builder {
        FileHandleAssociateType associateType;
        IdAndVersion synapseIdWithVersion;
        String fileHandleId;

        public Builder setSynapseIdWithVersion(final IdAndVersion synapseIdWithVersion) {
            this.synapseIdWithVersion = synapseIdWithVersion;
            return this;
        }

        public Builder setAssociateType(final FileHandleAssociateType associateType) {
            this.associateType = associateType;
            return this;
        }

        public Builder setFileHandleId(final String fileHandleId) {
            this.fileHandleId = fileHandleId;
            return this;
        }

        public AccessId build() {
            ValidateArgument.required(fileHandleId, "fileHandleId");
            if (synapseIdWithVersion != null) {
                validateSynapseIdWithVersion(synapseIdWithVersion);
            }
            return new AccessId(associateType, synapseIdWithVersion, fileHandleId);
        }

        private void validateSynapseIdWithVersion(final IdAndVersion synapseIdWithVersion) {
            if (!synapseIdWithVersion.getVersion().isPresent()) {
                throw new IllegalArgumentException("Synapse id should include version. e.g syn123.1");
            }
        }
    }
}
