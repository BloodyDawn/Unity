CREATE TABLE IF NOT EXISTS `accounts` (
  `id` BIGINT AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL,
  `password` VARCHAR(45) NOT NULL,
  `last_server_id` TINYINT UNSIGNED,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE INDEX(`name`),
  PRIMARY KEY(`id`)
);

CREATE TABLE IF NOT EXISTS `account_otps` (
  `id` BIGINT AUTO_INCREMENT,
  `account_id` BIGINT,
  `name` VARCHAR(32),
  `code` VARCHAR(32), -- TODO: Calculate correct size
  PRIMARY KEY(`id`),
  FOREIGN KEY(`account_id`) REFERENCES `accounts`(`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `account_logins` (
  `id` BIGINT AUTO_INCREMENT,
  `account_id` BIGINT,
  `server_id` TINYINT UNSIGNED,
  `ip` VARCHAR(45),
  `logged_in_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(`id`),
  FOREIGN KEY(`account_id`) REFERENCES `accounts`(`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `account_bans` (
  `id` BIGINT AUTO_INCREMENT,
  `account_id` BIGINT,
  `active` BOOLEAN NOT NULL DEFAULT TRUE,
  `started_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `expires_at` TIMESTAMP NOT NULL,
  `reason` TEXT,
  PRIMARY KEY(`id`),
  FOREIGN KEY(`account_id`) REFERENCES `accounts`(`id`) ON DELETE CASCADE
);
