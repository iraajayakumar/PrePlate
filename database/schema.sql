-- =====================================================================
-- PrePlate database schema (MySQL 8.x)
-- Run this whole script in MySQL Workbench, then run seed.sql.
-- Conventions: snake_case, plural table names, VARCHAR for enums
-- (so JPA @Enumerated(EnumType.STRING) maps 1:1), DATETIME(6) in UTC.
-- =====================================================================
CREATE DATABASE IF NOT EXISTS preplate_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE preplate_db;

-- Drop in dependency order so the script can be re-run safely (DESTROYS DATA).
DROP TABLE IF EXISTS consumption_records;
DROP TABLE IF EXISTS dish_forecasts;
DROP TABLE IF EXISTS meal_forecasts;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS dish_preferences;
DROP TABLE IF EXISTS meal_responses;
DROP TABLE IF EXISTS meal_dishes;
DROP TABLE IF EXISTS meals;
DROP TABLE IF EXISTS sustainability_facts;
DROP TABLE IF EXISTS dishes;
DROP TABLE IF EXISTS meal_schedules;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
  id                  BIGINT       NOT NULL AUTO_INCREMENT,
  registration_number VARCHAR(30)  NOT NULL,
  full_name           VARCHAR(100) NOT NULL,
  password_hash       VARCHAR(100) NOT NULL,
  role                VARCHAR(20)  NOT NULL,              -- STUDENT | ADMIN
  department          VARCHAR(100) NULL,
  hostel_name         VARCHAR(100) NULL,
  hostel_room         VARCHAR(20)  NULL,
  created_at          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_registration_number (registration_number),
  KEY idx_users_role (role)
) ENGINE=InnoDB;

CREATE TABLE meal_schedules (
  meal_type           VARCHAR(20) NOT NULL,                -- BREAKFAST | LUNCH | SNACKS | DINNER
  serving_start       TIME        NOT NULL,
  serving_end         TIME        NOT NULL,
  deadline_time       TIME        NOT NULL,                -- response deadline, local time
  deadline_day_offset INT         NOT NULL,                -- -1 = day before the meal, 0 = same day
  PRIMARY KEY (meal_type)
) ENGINE=InnoDB;

CREATE TABLE dishes (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  name        VARCHAR(100)  NOT NULL,
  description VARCHAR(255)  NULL,
  calories    INT           NOT NULL,
  protein_g   DECIMAL(6,1)  NOT NULL,
  carbs_g     DECIMAL(6,1)  NOT NULL,
  fat_g       DECIMAL(6,1)  NOT NULL,
  ingredients TEXT          NULL,                          -- comma separated
  vegetarian  TINYINT(1)    NOT NULL DEFAULT 1,
  image_file  VARCHAR(100)  NULL,                          -- file in frontend/public/assets/dishes/
  PRIMARY KEY (id),
  UNIQUE KEY uk_dishes_name (name)
) ENGINE=InnoDB;

CREATE TABLE meals (
  id        BIGINT      NOT NULL AUTO_INCREMENT,
  meal_date DATE        NOT NULL,
  meal_type VARCHAR(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_meals_date_type (meal_date, meal_type),
  KEY idx_meals_type_date (meal_type, meal_date)
) ENGINE=InnoDB;

CREATE TABLE meal_dishes (
  meal_id BIGINT NOT NULL,
  dish_id BIGINT NOT NULL,
  PRIMARY KEY (meal_id, dish_id),
  CONSTRAINT fk_meal_dishes_meal FOREIGN KEY (meal_id) REFERENCES meals (id)  ON DELETE CASCADE,
  CONSTRAINT fk_meal_dishes_dish FOREIGN KEY (dish_id) REFERENCES dishes (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE meal_responses (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  user_id      BIGINT      NOT NULL,
  meal_id      BIGINT      NOT NULL,
  status       VARCHAR(20) NOT NULL,                       -- ATTENDING | NOT_ATTENDING | NOT_SURE
  attended     TINYINT(1)  NULL,                           -- NULL = not verified (no verification mechanism yet)
  responded_at DATETIME(6) NOT NULL,
  updated_at   DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_meal_responses_user_meal (user_id, meal_id),
  KEY idx_meal_responses_meal_status (meal_id, status),
  KEY idx_meal_responses_user_responded (user_id, responded_at),
  CONSTRAINT fk_meal_responses_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_meal_responses_meal FOREIGN KEY (meal_id) REFERENCES meals (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE dish_preferences (
  id      BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  meal_id BIGINT NOT NULL,
  dish_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_dish_preferences_user_meal_dish (user_id, meal_id, dish_id),
  KEY idx_dish_preferences_meal_dish (meal_id, dish_id),
  CONSTRAINT fk_dish_preferences_user FOREIGN KEY (user_id) REFERENCES users (id)  ON DELETE CASCADE,
  CONSTRAINT fk_dish_preferences_meal FOREIGN KEY (meal_id) REFERENCES meals (id)  ON DELETE CASCADE,
  CONSTRAINT fk_dish_preferences_dish FOREIGN KEY (dish_id) REFERENCES dishes (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE notifications (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  user_id    BIGINT       NOT NULL,
  type       VARCHAR(30)  NOT NULL,   -- REMINDER | CONFIRMATION | NEXT_MEAL | OPTIONAL_PREFERENCE
  title      VARCHAR(100) NOT NULL,
  message    VARCHAR(255) NOT NULL,
  dedupe_key VARCHAR(100) NULL,       -- makes time-based notifications fire only once per user
  is_read    TINYINT(1)   NOT NULL DEFAULT 0,
  created_at DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_notifications_user_dedupe (user_id, dedupe_key),
  KEY idx_notifications_user_created (user_id, created_at),
  CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE meal_forecasts (
  id                      BIGINT        NOT NULL AUTO_INCREMENT,
  meal_id                 BIGINT        NOT NULL,
  predicted_attendance    INT           NOT NULL,
  lower_bound             INT           NOT NULL,
  upper_bound             INT           NOT NULL,
  confidence              DECIMAL(4,3)  NOT NULL,            -- 0.000 - 1.000
  recommended_preparation INT           NOT NULL,
  source                  VARCHAR(20)   NOT NULL,            -- ML_SERVICE | HEURISTIC
  explanation_json        TEXT          NULL,                -- [{"name":"...","importance":0.4}]
  generated_at            DATETIME(6)   NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_meal_forecasts_meal (meal_id),
  CONSTRAINT fk_meal_forecasts_meal FOREIGN KEY (meal_id) REFERENCES meals (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE dish_forecasts (
  id                      BIGINT      NOT NULL AUTO_INCREMENT,
  meal_id                 BIGINT      NOT NULL,
  dish_id                 BIGINT      NOT NULL,
  student_interest        INT         NOT NULL,
  predicted_demand        INT         NOT NULL,
  recommended_preparation INT         NOT NULL,
  generated_at            DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_dish_forecasts_meal_dish (meal_id, dish_id),
  CONSTRAINT fk_dish_forecasts_meal FOREIGN KEY (meal_id) REFERENCES meals (id)  ON DELETE CASCADE,
  CONSTRAINT fk_dish_forecasts_dish FOREIGN KEY (dish_id) REFERENCES dishes (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE consumption_records (
  id                   BIGINT      NOT NULL AUTO_INCREMENT,
  meal_id              BIGINT      NOT NULL,
  dish_id              BIGINT      NOT NULL,
  recommended_quantity INT         NULL,
  prepared_quantity    INT         NOT NULL,
  consumed_quantity    INT         NOT NULL,
  leftover_quantity    INT         NOT NULL,                -- prepared - consumed
  recorded_at          DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_consumption_records_meal_dish (meal_id, dish_id),
  KEY idx_consumption_records_recorded (recorded_at),
  CONSTRAINT fk_consumption_records_meal FOREIGN KEY (meal_id) REFERENCES meals (id)  ON DELETE CASCADE,
  CONSTRAINT fk_consumption_records_dish FOREIGN KEY (dish_id) REFERENCES dishes (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE sustainability_facts (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  fact_text   VARCHAR(500) NOT NULL,
  source_note VARCHAR(200) NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

SELECT 'schema created' AS status;
