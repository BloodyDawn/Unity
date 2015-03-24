CREATE TABLE IF NOT EXISTS `clan_wars` (
  `clan1` varchar(35) NOT NULL DEFAULT '',
  `clan2` varchar(35) NOT NULL DEFAULT '',
  `clan1Kill` int(11) NOT NULL DEFAULT '',
  `clan2Kill` int(11) NOT NULL DEFAULT '',
  `winnerClan` varchar(35) NOT NULL DEFAULT '0',
  `startTime` bigint(13) NOT NULL DEFAULT '',
  `endTime` bigint(13) NOT NULL DEFAULT '',
  `state` tinyint(4) NOT NULL DEFAULT '',
  KEY `clan1` (`clan1`),
  KEY `clan2` (`clan2`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;