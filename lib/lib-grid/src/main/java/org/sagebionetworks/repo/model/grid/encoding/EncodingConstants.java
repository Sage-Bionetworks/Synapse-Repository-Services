package org.sagebionetworks.repo.model.grid.encoding;


public class EncodingConstants {

    /**
     * Indicates in a variable-length unsigned integer (vu57/b1vu56) if there are more bytes to read.
     */
    public static final int CONTINUATION_BIT = 0x80; // 10000000
    /**
     * Can be used to bitwise-select the bits that represent data in a variable-length unsigned integer
     */
    public static final int DATA_MASK = 0x7F;        // 01111111
}