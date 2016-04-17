CREATE TABLE IF NOT EXISTS `npc_respawns` (
  `id` int(10) NOT NULL,
  `x` int(10) NOT NULL,
  `y` int(10) NOT NULL,
  `z` int(10) NOT NULL,
  `heading` int(10) DEFAULT NULL,
  `respawnTime` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `currentHp` double unsigned NOT NULL,
  `currentMp` double unsigned NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;