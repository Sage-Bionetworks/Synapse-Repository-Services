CREATE TABLE JDOSUBMISSION (
    ID bigint(20) NOT NULL,
    NAME varchar(256) CHARACTER SET latin1 COLLATE latin1_bin,
    EVALUATION_ID bigint(20) NOT NULL,
    USER_ID bigint(20) NOT NULL,
    ENTITY_ID bigint(20) NOT NULL,
    ENTITY_BUNDLE mediumblob,
    VERSION bigint(20) NOT NULL,
    CREATED_ON bigint(20) NOT NULL,
    PRIMARY KEY (ID)
);