-- PrePlate seed data: meal schedules, dishes, sustainability facts
-- Run AFTER schema.sql.
USE preplate_db;

INSERT INTO meal_schedules (meal_type, serving_start, serving_end, deadline_time, deadline_day_offset) VALUES
  ('BREAKFAST','07:30:00','09:30:00','18:00:00',-1),
  ('LUNCH','12:30:00','14:30:00','22:00:00',-1),
  ('SNACKS','16:30:00','18:00:00','10:00:00',0),
  ('DINNER','19:30:00','21:30:00','14:00:00',0);

INSERT INTO dishes (name, description, calories, protein_g, carbs_g, fat_g, ingredients, vegetarian, image_file) VALUES
  ('Idli','Steamed rice and urad dal cakes (2 pieces)',120,4,24,0.4,'Rice, Urad dal, Salt',1,'idli.jpg'),
  ('Medu Vada','Crisp fried urad dal doughnut',190,6,20,9,'Urad dal, Pepper, Curry leaves, Oil',1,'medu-vada.jpg'),
  ('Masala Dosa','Crispy rice crepe with potato filling',280,6,40,10,'Rice, Urad dal, Potato, Onion, Mustard',1,'masala-dosa.jpg'),
  ('Ven Pongal','Savoury rice and moong dal porridge',250,7,38,8,'Rice, Moong dal, Ghee, Pepper, Cumin',1,'ven-pongal.jpg'),
  ('Upma','Semolina cooked with vegetables and spices',210,5,33,7,'Semolina, Onion, Mustard, Curry leaves',1,'upma.jpg'),
  ('Poha','Flattened rice with peanuts and spices',180,4,32,4,'Flattened rice, Peanuts, Onion, Turmeric',1,'poha.jpg'),
  ('Sambar','Lentil and vegetable stew',90,4,13,2.5,'Toor dal, Drumstick, Tamarind, Sambar powder',1,'sambar.jpg'),
  ('Coconut Chutney','Fresh coconut and green chilli chutney',70,1,4,6,'Coconut, Green chilli, Roasted gram',1,'coconut-chutney.jpg'),
  ('Boiled Egg','Hard boiled egg',78,6,0.6,5,'Egg',0,'boiled-egg.jpg'),
  ('Poori Masala','Fried wheat bread with potato masala',320,6,42,14,'Wheat flour, Potato, Onion, Oil',1,'poori-masala.jpg'),
  ('Steamed Rice','Plain steamed rice',210,4,45,0.4,'Rice',1,'steamed-rice.jpg'),
  ('Dal Tadka','Yellow lentils tempered with spices',180,9,22,6,'Toor dal, Garlic, Cumin, Ghee',1,'dal-tadka.jpg'),
  ('Paneer Butter Masala','Paneer in rich tomato butter gravy',320,14,12,24,'Paneer, Tomato, Butter, Cream, Spices',1,'paneer-butter-masala.jpg'),
  ('Aloo Gobi','Potato and cauliflower dry curry',150,4,18,7,'Potato, Cauliflower, Turmeric, Cumin',1,'aloo-gobi.jpg'),
  ('Chapati','Whole wheat flatbread',120,3.5,20,3,'Whole wheat flour, Oil',1,'chapati.jpg'),
  ('Curd','Fresh set curd',80,4,6,4,'Milk',1,'curd.jpg'),
  ('Rasam','Peppery tamarind and tomato soup',45,2,7,1,'Tomato, Tamarind, Pepper, Cumin',1,'rasam.jpg'),
  ('Veg Pulao','Fragrant rice with mixed vegetables',260,6,42,8,'Basmati rice, Peas, Carrot, Beans, Whole spices',1,'veg-pulao.jpg'),
  ('Chicken Curry','Home-style chicken curry',280,24,8,16,'Chicken, Onion, Tomato, Spices',0,'chicken-curry.jpg'),
  ('Mixed Veg Curry','Seasonal vegetables in light gravy',130,4,15,6,'Carrot, Beans, Peas, Potato, Spices',1,'mixed-veg-curry.jpg'),
  ('Papad','Roasted lentil wafer',40,2,5,1,'Urad dal, Pepper',1,'papad.jpg'),
  ('Beans Poriyal','Stir-fried beans with coconut',90,3,10,4,'Beans, Coconut, Mustard',1,'beans-poriyal.jpg'),
  ('Samosa','Fried pastry with spiced potato filling',260,5,28,14,'Wheat flour, Potato, Peas, Spices',1,'samosa.jpg'),
  ('Veg Puff','Baked puff pastry with vegetable filling',240,4,26,13,'Puff pastry, Potato, Carrot, Spices',1,'veg-puff.jpg'),
  ('Sundal','Boiled chickpeas tempered with coconut',150,8,22,3,'Chickpeas, Coconut, Mustard, Curry leaves',1,'sundal.jpg'),
  ('Masala Tea','Spiced milk tea',90,2,14,3,'Tea, Milk, Ginger, Cardamom, Sugar',1,'masala-tea.jpg'),
  ('Bhel Puri','Puffed rice chaat with chutneys',180,4,30,5,'Puffed rice, Sev, Onion, Tamarind chutney',1,'bhel-puri.jpg'),
  ('Bread Pakoda','Gram flour battered fried bread',230,5,26,11,'Bread, Gram flour, Oil, Spices',1,'bread-pakoda.jpg'),
  ('Fruit Salad','Seasonal chopped fruit',110,1,26,0.4,'Apple, Banana, Papaya, Grapes',1,'fruit-salad.jpg'),
  ('Veg Fried Rice','Wok tossed rice with vegetables',290,6,48,8,'Rice, Carrot, Beans, Cabbage, Soy sauce',1,'veg-fried-rice.jpg'),
  ('Gobi Manchurian','Crisp cauliflower in tangy sauce',220,5,24,11,'Cauliflower, Corn flour, Soy sauce, Garlic',1,'gobi-manchurian.jpg'),
  ('Dal Fry','Lentils fried with onion and tomato',170,8,20,6,'Mixed dal, Onion, Tomato, Spices',1,'dal-fry.jpg'),
  ('Egg Curry','Boiled eggs in spiced onion gravy',210,13,6,15,'Egg, Onion, Tomato, Spices',0,'egg-curry.jpg'),
  ('Jeera Rice','Cumin flavoured basmati rice',200,4,40,3,'Basmati rice, Cumin, Ghee',1,'jeera-rice.jpg'),
  ('Rajma Masala','Kidney beans in thick gravy',230,10,33,6,'Kidney beans, Onion, Tomato, Spices',1,'rajma-masala.jpg'),
  ('Paneer Bhurji','Scrambled spiced paneer',250,15,7,18,'Paneer, Onion, Tomato, Spices',1,'paneer-bhurji.jpg'),
  ('Kheer','Rice pudding with milk and cardamom',210,5,32,7,'Milk, Rice, Sugar, Cardamom',1,'kheer.jpg');

INSERT INTO sustainability_facts (fact_text, source_note) VALUES
  ('Every response helps the kitchen prepare closer to actual demand.','PrePlate'),
  ('Roughly one third of the food produced for people worldwide is lost or wasted every year.','FAO estimate - verify before publication'),
  ('Cooking only what is needed saves food, cooking fuel, water and money.','General sustainability fact'),
  ('Growing 1 kg of rice takes roughly 2,500 litres of water, so every leftover plate of rice carries a hidden water cost.','Approximate figure - verify before publication'),
  ('Confirming your meal even 30 minutes early gives the kitchen time to adjust quantities.','PrePlate'),
  ('Food waste in landfills releases methane, a greenhouse gas far stronger than carbon dioxide over the short term.','General sustainability fact'),
  ('Small portions you can always top up are one of the easiest ways to cut plate waste.','General sustainability fact'),
  ('Leftover rice should be cooled and refrigerated quickly, so it stays safe to eat.','General food-safety fact'),
  ('Solar energy can replace part of the fuel used in large-scale cooking.','General sustainability fact - verify SRM-specific claims before publication'),
  ('Seasonal vegetables travel less, cost less and often taste better.','General sustainability fact'),
  ('A plate that is finished is a plate that was planned well. Thank you for planning yours.','PrePlate'),
  ('Dal and rice together give a complete protein, which is why this humble combination is so nutritious.','General nutrition fact');

SELECT 'meal_schedules' AS tbl, COUNT(*) AS rows_inserted FROM meal_schedules UNION ALL SELECT 'dishes', COUNT(*) FROM dishes UNION ALL SELECT 'sustainability_facts', COUNT(*) FROM sustainability_facts;

-- =====================================================================
-- DEMO DATA: users, meals, menus, responses, preferences, consumption history
-- Everything below is safe to re-run (INSERT IGNORE / skips existing rows).
-- Logins: ADMIN001 / admin123   and   RA2111003010001 .. RA2111003010030 / student123
-- =====================================================================

SET @today = DATE(CONVERT_TZ(UTC_TIMESTAMP(), '+00:00', '+05:30'));   -- Asia/Kolkata date

-- 1. Users (ADMIN001/admin123, students RA2111003010001../student123)
INSERT IGNORE INTO users (registration_number, full_name, password_hash, role, department, hostel_name, hostel_room, created_at) VALUES
('ADMIN001','Kitchen Admin','$2a$10$hJ67UsD7fSbWRpP9ZHGxrOtj4PNL3RmaRmZLnVqhK7gqSJel7d.qm','ADMIN',NULL,NULL,NULL,NOW(6)),
('RA2111003010001','Aarav Mehta','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','CSE','Kaveri','101',NOW(6)),
('RA2111003010002','Aadhya Iyer','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','ECE','Ganga','102',NOW(6)),
('RA2111003010003','Aditya Reddy','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','Mechanical','Narmada','103',NOW(6)),
('RA2111003010004','Ananya Sharma','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','Civil','Yamuna','104',NOW(6)),
('RA2111003010005','Arjun Nair','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','Biotech','Kaveri','105',NOW(6)),
('RA2111003010006','Diya Patel','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','CSE','Ganga','106',NOW(6)),
('RA2111003010007','Ishaan Rao','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','ECE','Narmada','107',NOW(6)),
('RA2111003010008','Kavya Menon','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','Mechanical','Yamuna','108',NOW(6)),
('RA2111003010009','Meera Kapoor','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','Civil','Kaveri','109',NOW(6)),
('RA2111003010010','Rohan Das','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','Biotech','Ganga','110',NOW(6)),
('RA2111003010011','Saanvi Kumar','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','CSE','Narmada','111',NOW(6)),
('RA2111003010012','Vikram Singh','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','ECE','Yamuna','112',NOW(6)),
('RA2111003010013','Aarav Iyer','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','Mechanical','Kaveri','113',NOW(6)),
('RA2111003010014','Aadhya Reddy','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','Civil','Ganga','114',NOW(6)),
('RA2111003010015','Aditya Sharma','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','Biotech','Narmada','115',NOW(6)),
('RA2111003010016','Ananya Nair','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','CSE','Yamuna','116',NOW(6)),
('RA2111003010017','Arjun Patel','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','ECE','Kaveri','117',NOW(6)),
('RA2111003010018','Diya Rao','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','Mechanical','Ganga','118',NOW(6)),
('RA2111003010019','Ishaan Menon','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','Civil','Narmada','119',NOW(6)),
('RA2111003010020','Kavya Kapoor','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','Biotech','Yamuna','120',NOW(6)),
('RA2111003010021','Meera Das','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','CSE','Kaveri','121',NOW(6)),
('RA2111003010022','Rohan Kumar','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','ECE','Ganga','122',NOW(6)),
('RA2111003010023','Saanvi Singh','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','Mechanical','Narmada','123',NOW(6)),
('RA2111003010024','Vikram Mehta','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','Civil','Yamuna','124',NOW(6)),
('RA2111003010025','Aarav Reddy','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','Biotech','Kaveri','125',NOW(6)),
('RA2111003010026','Aadhya Sharma','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','CSE','Ganga','126',NOW(6)),
('RA2111003010027','Aditya Nair','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','ECE','Narmada','127',NOW(6)),
('RA2111003010028','Ananya Patel','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','Mechanical','Yamuna','128',NOW(6)),
('RA2111003010029','Arjun Rao','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','Civil','Kaveri','129',NOW(6)),
('RA2111003010030','Diya Menon','$2a$10$N265MhzSTxaFImg0JTJsiOvFuTJOGkPj4/N/vlHBGtJ0gHoFUx7Pm','STUDENT','Biotech','Ganga','130',NOW(6));

-- 2. Meals: every meal type for every day from today-14 to today+3
INSERT IGNORE INTO meals (meal_date, meal_type)
SELECT DATE_ADD(@today, INTERVAL o.d DAY), t.meal_type
FROM (SELECT -14 d UNION ALL SELECT -13 UNION ALL SELECT -12 UNION ALL SELECT -11 UNION ALL SELECT -10 UNION ALL SELECT -9
      UNION ALL SELECT -8 UNION ALL SELECT -7 UNION ALL SELECT -6 UNION ALL SELECT -5 UNION ALL SELECT -4 UNION ALL SELECT -3
      UNION ALL SELECT -2 UNION ALL SELECT -1 UNION ALL SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3) o
CROSS JOIN (SELECT 'BREAKFAST' meal_type UNION ALL SELECT 'LUNCH' UNION ALL SELECT 'SNACKS' UNION ALL SELECT 'DINNER') t;

-- 3. Menus (same rotation as the application's seeder)
DROP TEMPORARY TABLE IF EXISTS menu_map;
CREATE TEMPORARY TABLE menu_map (meal_type VARCHAR(20), menu_idx INT, dish_name VARCHAR(100));
INSERT INTO menu_map VALUES
('BREAKFAST',0,'Idli'),('BREAKFAST',0,'Medu Vada'),('BREAKFAST',0,'Sambar'),('BREAKFAST',0,'Coconut Chutney'),('BREAKFAST',1,'Masala Dosa'),('BREAKFAST',1,'Sambar'),('BREAKFAST',1,'Coconut Chutney'),('BREAKFAST',2,'Ven Pongal'),('BREAKFAST',2,'Medu Vada'),('BREAKFAST',2,'Sambar'),('BREAKFAST',2,'Coconut Chutney'),('BREAKFAST',3,'Upma'),('BREAKFAST',3,'Coconut Chutney'),('BREAKFAST',3,'Boiled Egg'),('BREAKFAST',4,'Poha'),('BREAKFAST',4,'Boiled Egg'),('BREAKFAST',4,'Masala Tea'),('LUNCH',0,'Steamed Rice'),('LUNCH',0,'Dal Tadka'),('LUNCH',0,'Paneer Butter Masala'),('LUNCH',0,'Aloo Gobi'),('LUNCH',0,'Chapati'),('LUNCH',0,'Curd'),('LUNCH',1,'Veg Pulao'),('LUNCH',1,'Dal Fry'),('LUNCH',1,'Mixed Veg Curry'),('LUNCH',1,'Papad'),('LUNCH',1,'Curd'),('LUNCH',2,'Jeera Rice'),('LUNCH',2,'Rajma Masala'),('LUNCH',2,'Beans Poriyal'),('LUNCH',2,'Chapati'),('LUNCH',2,'Curd'),('LUNCH',3,'Steamed Rice'),('LUNCH',3,'Sambar'),('LUNCH',3,'Rasam'),('LUNCH',3,'Egg Curry'),('LUNCH',3,'Beans Poriyal'),('LUNCH',3,'Papad'),('SNACKS',0,'Samosa'),('SNACKS',0,'Masala Tea'),('SNACKS',1,'Veg Puff'),('SNACKS',1,'Masala Tea'),('SNACKS',2,'Sundal'),('SNACKS',2,'Masala Tea'),('SNACKS',3,'Bhel Puri'),('SNACKS',3,'Masala Tea'),('SNACKS',4,'Bread Pakoda'),('SNACKS',4,'Masala Tea'),('DINNER',0,'Chapati'),('DINNER',0,'Paneer Bhurji'),('DINNER',0,'Dal Tadka'),('DINNER',0,'Curd'),('DINNER',1,'Veg Fried Rice'),('DINNER',1,'Gobi Manchurian'),('DINNER',2,'Steamed Rice'),('DINNER',2,'Chicken Curry'),('DINNER',2,'Dal Fry'),('DINNER',2,'Papad'),('DINNER',3,'Chapati'),('DINNER',3,'Rajma Masala'),('DINNER',3,'Jeera Rice'),('DINNER',3,'Fruit Salad'),('DINNER',4,'Poori Masala'),('DINNER',4,'Aloo Gobi'),('DINNER',4,'Kheer');
INSERT IGNORE INTO meal_dishes (meal_id, dish_id)
SELECT m.id, d.id
FROM meals m
JOIN menu_map mm ON mm.meal_type = m.meal_type
 AND mm.menu_idx = MOD(DAYOFYEAR(m.meal_date), CASE m.meal_type WHEN 'BREAKFAST' THEN 5 WHEN 'LUNCH' THEN 4 WHEN 'SNACKS' THEN 5 WHEN 'DINNER' THEN 5 END)
JOIN dishes d ON d.name = mm.dish_name
WHERE m.meal_date BETWEEN DATE_SUB(@today, INTERVAL 14 DAY) AND DATE_ADD(@today, INTERVAL 3 DAY)
  AND NOT EXISTS (SELECT 1 FROM meal_dishes x WHERE x.meal_id = m.id);
DROP TEMPORARY TABLE menu_map;

-- 4. Student responses (about 85% of students respond; the demo student RA2111003010001 leaves future meals open)
INSERT IGNORE INTO meal_responses (user_id, meal_id, status, attended, responded_at, updated_at)
SELECT u.id, m.id,
  CASE WHEN h.r < 600 THEN 'ATTENDING' WHEN h.r < 750 THEN 'NOT_ATTENDING' ELSE 'NOT_SURE' END,
  CASE WHEN m.meal_date < @today AND h.r < 600 THEN IF(MOD(CRC32(CONCAT(u.id,'a',m.id)), 100) < 90, 1, 0) ELSE NULL END,
  NOW(6), NOW(6)
FROM users u
JOIN meals m ON m.meal_date BETWEEN DATE_SUB(@today, INTERVAL 14 DAY) AND DATE_ADD(@today, INTERVAL 3 DAY)
JOIN (SELECT 0 AS r) z
JOIN (SELECT u2.id AS uid, m2.id AS mid, MOD(CRC32(CONCAT(u2.id,'-',m2.id)), 1000) AS r FROM users u2 CROSS JOIN meals m2) h
  ON h.uid = u.id AND h.mid = m.id
WHERE u.role = 'STUDENT' AND h.r < 850
  AND NOT (m.meal_date >= @today AND u.registration_number = 'RA2111003010001');

-- 5. Dish preferences for students who attended past meals
INSERT IGNORE INTO dish_preferences (user_id, meal_id, dish_id)
SELECT r.user_id, r.meal_id, md.dish_id
FROM meal_responses r
JOIN meals m ON m.id = r.meal_id AND m.meal_date < @today
JOIN meal_dishes md ON md.meal_id = r.meal_id
WHERE r.status = 'ATTENDING'
  AND MOD(CRC32(CONCAT(r.user_id,'p',r.meal_id)), 100) < 70
  AND MOD(CRC32(CONCAT(r.user_id,'-',r.meal_id,'-',md.dish_id)), 100) < 55;

-- 6. Consumption history for past meals (servings): prepared vs consumed per dish
INSERT IGNORE INTO consumption_records (meal_id, dish_id, recommended_quantity, prepared_quantity, consumed_quantity, leftover_quantity, recorded_at)
SELECT q.meal_id, q.dish_id, q.rec, q.prep, q.cons, q.prep - q.cons, NOW(6)
FROM (
  SELECT t.meal_id, t.dish_id, t.rec,
         ROUND(t.rec * (1 + MOD(CRC32(CONCAT(t.meal_id,'x',t.dish_id)), 50) / 1000)) AS prep,
         LEAST(ROUND(t.rec * (1 + MOD(CRC32(CONCAT(t.meal_id,'x',t.dish_id)), 50) / 1000)),
               ROUND(t.dem * (0.80 + MOD(CRC32(CONCAT(t.dish_id,'y',t.meal_id)), 200) / 1000))) AS cons
  FROM (
    SELECT md.meal_id, md.dish_id,
           ROUND(a.att * (0.55 + 0.45 * COALESCE(i.cnt,0) / GREATEST(a.att,1))) AS dem,
           ROUND(a.att * (0.55 + 0.45 * COALESCE(i.cnt,0) / GREATEST(a.att,1)) * 1.05) AS rec
    FROM meal_dishes md
    JOIN meals m ON m.id = md.meal_id AND m.meal_date < @today
    JOIN (SELECT meal_id, COUNT(*) att FROM meal_responses WHERE status = 'ATTENDING' GROUP BY meal_id) a ON a.meal_id = md.meal_id
    LEFT JOIN (SELECT meal_id, dish_id, COUNT(*) cnt FROM dish_preferences GROUP BY meal_id, dish_id) i
           ON i.meal_id = md.meal_id AND i.dish_id = md.dish_id
  ) t
) q
WHERE q.rec > 0;

SELECT (SELECT COUNT(*) FROM users) AS users, (SELECT COUNT(*) FROM meals) AS meals,
       (SELECT COUNT(*) FROM meal_dishes) AS meal_dishes, (SELECT COUNT(*) FROM meal_responses) AS responses,
       (SELECT COUNT(*) FROM dish_preferences) AS preferences, (SELECT COUNT(*) FROM consumption_records) AS consumption;
