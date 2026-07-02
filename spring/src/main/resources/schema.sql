CREATE DATABASE IF NOT EXISTS health_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE health_db;

DROP TABLE IF EXISTS plan_checkin;
DROP TABLE IF EXISTS article_favorite;
DROP TABLE IF EXISTS user_settings;
DROP TABLE IF EXISTS user_plan;
DROP TABLE IF EXISTS heart_record;
DROP TABLE IF EXISTS health_record;
DROP TABLE IF EXISTS diet_record;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  phone VARCHAR(20) UNIQUE,
  password VARCHAR(100) NOT NULL,
  nickname VARCHAR(50),
  gender VARCHAR(10) DEFAULT 'unknown',
  birthday DATE NULL,
  avatar VARCHAR(255),
  role VARCHAR(20) DEFAULT 'USER',
  status TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE diet_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  meal_type VARCHAR(20) NOT NULL,
  food_name VARCHAR(100) NOT NULL,
  weight DECIMAL(8,2) DEFAULT 0,
  calories DECIMAL(8,2) DEFAULT 0,
  record_time DATETIME NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_diet_user_time (user_id, record_time),
  CONSTRAINT fk_diet_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE health_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  height DECIMAL(6,2) NOT NULL,
  weight DECIMAL(6,2) NOT NULL,
  bmi DECIMAL(6,2),
  bmr DECIMAL(8,2),
  record_time DATETIME NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_health_user_time (user_id, record_time),
  CONSTRAINT fk_health_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE heart_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  heart_rate INT NOT NULL,
  measure_type VARCHAR(30) DEFAULT 'manual',
  record_time DATETIME NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_heart_user_time (user_id, record_time),
  CONSTRAINT fk_heart_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(100) NOT NULL,
  category VARCHAR(30),
  target_days INT NOT NULL DEFAULT 7,
  goal_text VARCHAR(255),
  theme VARCHAR(30),
  progress INT DEFAULT 0,
  status VARCHAR(20) DEFAULT 'active',
  start_date DATE NOT NULL,
  end_date DATE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_plan_user_status (user_id, status),
  CONSTRAINT fk_plan_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE plan_checkin (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plan_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  checkin_date DATE NOT NULL,
  remark VARCHAR(255),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_plan_user_date (plan_id, user_id, checkin_date),
  INDEX idx_checkin_user_date (user_id, checkin_date),
  CONSTRAINT fk_checkin_plan FOREIGN KEY (plan_id) REFERENCES user_plan(id) ON DELETE CASCADE,
  CONSTRAINT fk_checkin_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE article_favorite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  article_title VARCHAR(200) NOT NULL,
  article_url VARCHAR(500) NOT NULL,
  source VARCHAR(100),
  summary VARCHAR(500),
  favorite_time DATETIME NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_article_url (user_id, article_url),
  INDEX idx_favorite_user_time (user_id, favorite_time),
  CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_settings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  notify_enabled TINYINT DEFAULT 1,
  step_goal INT DEFAULT 8000,
  privacy_level VARCHAR(20) DEFAULT 'partial',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_settings_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
