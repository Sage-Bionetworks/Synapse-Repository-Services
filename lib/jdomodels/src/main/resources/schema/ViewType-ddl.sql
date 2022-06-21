CREATE TABLE IF NOT EXISTS `VIEW_TYPE` (
  `VIEW_ID` BIGINT NOT NULL,
  `VIEW_OBJECT_TYPE` enum('ENTITY','SUBMISSION', 'DATASET', 'DATASET_COLLECTION') NOT NULL,
  `VIEW_TYPE_MASK` BIGINT NOT NULL,
  `ETAG` char(36) NOT NULL,
  PRIMARY KEY (`VIEW_ID`)
)