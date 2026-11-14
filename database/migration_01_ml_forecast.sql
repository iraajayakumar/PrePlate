-- =====================================================================
-- PrePlate migration 01: store forecasts from the ML service
-- Run ONCE in MySQL Workbench on preplate_db (after schema.sql + seed.sql).
-- Existing rows are kept. Quantities become decimals (the ML service
-- returns values like 243.96) and forecasts record how they were made.
-- =====================================================================
USE preplate_db;

-- Meal-level: how was this forecast produced?
ALTER TABLE meal_forecasts
  ADD COLUMN model_version   VARCHAR(60)  NULL AFTER source,
  ADD COLUMN forecast_method VARCHAR(20)  NULL AFTER model_version,   -- ml | fallback | NULL (built-in estimate)
  ADD COLUMN data_source     VARCHAR(20)  NULL AFTER forecast_method, -- synthetic | observed | NULL
  ADD COLUMN fallback_reason VARCHAR(100) NULL AFTER data_source;

-- source now also holds ML_FALLBACK (value length is still <= 20)

-- Dish-level: decimal quantities, unit, method, note and interval
ALTER TABLE dish_forecasts
  MODIFY COLUMN predicted_demand         DECIMAL(10,2) NOT NULL,
  MODIFY COLUMN recommended_preparation  DECIMAL(10,2) NOT NULL,
  ADD COLUMN unit            VARCHAR(20)   NULL AFTER recommended_preparation,
  ADD COLUMN method          VARCHAR(20)   NULL AFTER unit,           -- ml | fallback | NULL (built-in)
  ADD COLUMN note            VARCHAR(255)  NULL AFTER method,         -- e.g. "No dish consumption history..."
  ADD COLUMN interval_lower  DECIMAL(10,2) NULL AFTER note,
  ADD COLUMN interval_upper  DECIMAL(10,2) NULL AFTER interval_lower;

-- Snapshot of the recommendation kept with the actuals
ALTER TABLE consumption_records
  MODIFY COLUMN recommended_quantity DECIMAL(10,2) NULL;

SELECT 'migration_01 applied' AS status;
