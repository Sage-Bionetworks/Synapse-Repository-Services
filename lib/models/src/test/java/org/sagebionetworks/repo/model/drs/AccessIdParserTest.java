package org.sagebionetworks.repo.model.drs;

import org.junit.Test;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AccessIdParserTest {

    @Test
    public void testAllParts() {
        AccessId accessId = AccessIdParser.parseAccessId("FileEntity_syn123.456_123456");
        assertNotNull(accessId);
        assertEquals(FileHandleAssociateType.FileEntity, accessId.getAssociateType());
        assertEquals("syn123.456", accessId.getSynapseIdWithVersion());
        assertEquals("123456", accessId.getFileHandleId());
    }

    @Test
    public void testMixedCase() {
        AccessId accessId = AccessIdParser.parseAccessId("FileEntity_sYn123.456_123456");
        assertNotNull(accessId);
        assertEquals(FileHandleAssociateType.FileEntity, accessId.getAssociateType());
        assertEquals("syn123.456", accessId.getSynapseIdWithVersion());
        assertEquals("123456", accessId.getFileHandleId());
    }

    @Test
    public void testWhiteSpace() {
        AccessId accessId = AccessIdParser.parseAccessId(" \t\nFileEntity_sYn123.456_123456\n\t ");
        assertNotNull(accessId);
        assertEquals(FileHandleAssociateType.FileEntity, accessId.getAssociateType());
        assertEquals("syn123.456", accessId.getSynapseIdWithVersion());
        assertEquals("123456", accessId.getFileHandleId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullString() {
        AccessIdParser.parseAccessId(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmpty() {
        AccessIdParser.parseAccessId("");
    }

    @Test
    public void testIncorrectFileHandleAssociationType() {
        try {
            AccessIdParser.parseAccessId("fileEntity_sny123.1_123");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getCause() instanceof AccessIdParser.ParseException);
            AccessIdParser.ParseException exception = ((AccessIdParser.ParseException) e.getCause());
            assertEquals(10, exception.getErrorIndex());
            assertEquals("No enum constant org.sagebionetworks.repo.model.file.FileHandleAssociateType.fileEntity",
                    exception.getMessage());
        }
    }

    @Test
    public void testSynapseIdWithoutSyn() {
        try {
            AccessIdParser.parseAccessId("FileEntity_123.1_123");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getCause() instanceof AccessIdParser.ParseException);
            AccessIdParser.ParseException exception = ((AccessIdParser.ParseException) e.getCause());
            assertEquals(11, exception.getErrorIndex());
            assertEquals("AccessId must contains syn.", exception.getMessage());
        }
    }

    @Test
    public void testSynapseIdWithSy() {
        try {
            AccessIdParser.parseAccessId("FileEntity_Sy123.1_123");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getCause() instanceof AccessIdParser.ParseException);
            AccessIdParser.ParseException exception = ((AccessIdParser.ParseException) e.getCause());
            assertEquals(13, exception.getErrorIndex());
            assertEquals("AccessId must contains syn.", exception.getMessage());
        }
    }

    @Test
    public void testSynapseIdWithSn() {
        try {
            AccessIdParser.parseAccessId("FileEntity_Sn123.1_123");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getCause() instanceof AccessIdParser.ParseException);
            AccessIdParser.ParseException exception = ((AccessIdParser.ParseException) e.getCause());
            assertEquals(12, exception.getErrorIndex());
            assertEquals("AccessId must contains syn.", exception.getMessage());
        }
    }


    @Test
    public void testSynapseIdWithoutLongId() {
        try {
            AccessIdParser.parseAccessId("FileEntity_Syn.1_123");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getCause() instanceof AccessIdParser.ParseException);
            AccessIdParser.ParseException exception = ((AccessIdParser.ParseException) e.getCause());
            assertEquals(14, exception.getErrorIndex());
            assertEquals("Not a valid synapse Id.", exception.getMessage());
        }
    }

    @Test
    public void testSynapseIdWithoutDotInVersion() {
        try {
            AccessIdParser.parseAccessId("FileEntity_Syn1231_123");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getCause() instanceof AccessIdParser.ParseException);
            AccessIdParser.ParseException exception = ((AccessIdParser.ParseException) e.getCause());
            assertEquals(18, exception.getErrorIndex());
            assertEquals("dot was expected.", exception.getMessage());
        }
    }

    @Test
    public void testSynapseIdWithoutVersion() {
        try {
            AccessIdParser.parseAccessId("FileEntity_Syn123._123");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getCause() instanceof AccessIdParser.ParseException);
            AccessIdParser.ParseException exception = ((AccessIdParser.ParseException) e.getCause());
            assertEquals(18, exception.getErrorIndex());
            assertEquals("Not a valid synapse Version.", exception.getMessage());
        }
    }

    @Test
    public void testSynapseIdWithoutFileHandleId() {
        try {
            AccessIdParser.parseAccessId("FileEntity_Syn123.1_");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getCause() instanceof AccessIdParser.ParseException);
            AccessIdParser.ParseException exception = ((AccessIdParser.ParseException) e.getCause());
            assertEquals(20, exception.getErrorIndex());
            assertEquals("Not a valid file handle Id.", exception.getMessage());
        }
    }

    @Test
    public void testSynapseIdWithoutUnderscore() {
        try {
            AccessIdParser.parseAccessId("FileEntity_Syn123.1");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getCause() instanceof AccessIdParser.ParseException);
            AccessIdParser.ParseException exception = ((AccessIdParser.ParseException) e.getCause());
            assertEquals(19, exception.getErrorIndex());
            assertEquals("Expected character underscore is missing.", exception.getMessage());
        }
    }

    @Test
    public void testSynapseIdWithExtraDelimiter() {
        try {
            AccessIdParser.parseAccessId("FileEntity_Syn123.1_123_");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getCause() instanceof AccessIdParser.ParseException);
            AccessIdParser.ParseException exception = ((AccessIdParser.ParseException) e.getCause());
            assertEquals(23, exception.getErrorIndex());
            assertEquals("must be an end.", exception.getMessage());
        }
    }

}
