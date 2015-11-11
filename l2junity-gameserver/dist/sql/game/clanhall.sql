CREATE TABLE IF NOT EXISTS `clanhall` (
  `id` int(11) NOT NULL DEFAULT '0',
  `ownerId` int(11) NOT NULL DEFAULT '0',
  `paidUntil` bigint(13) unsigned NOT NULL DEFAULT '0',
  `paid` char(5) DEFAULT 'false' NOT NULL,
  PRIMARY KEY `id` (`id`),
  KEY `ownerId` (`ownerId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;