CREATE TABLE IF NOT EXISTS `residense_functions` (
  `id`  int NOT NULL ,
  `level`  int NOT NULL ,
  `expiration`  bigint NOT NULL ,
  `ownerId`  int NOT NULL ,
  `residenseId`  int NOT NULL ,
  PRIMARY KEY (`id`, `level`, `ownerId`, `residenseId`)
);