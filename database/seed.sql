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
