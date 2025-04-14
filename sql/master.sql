CREATE DATABASE `aib_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */;

-- aib_db.fruits definition

CREATE TABLE `fruits` (
  `fruit_id` int(11) NOT NULL AUTO_INCREMENT,
  `fruit_name` varchar(255) NOT NULL,
  `source_country` varchar(255) NOT NULL,
  PRIMARY KEY (`fruit_id`)
) ;


-- aib_db.shops definition

CREATE TABLE `shops` (
  `shop_id` int(11) NOT NULL AUTO_INCREMENT,
  `shop_name` varchar(255) NOT NULL,
  `city` varchar(255) NOT NULL,
  `country` varchar(255) NOT NULL,
  PRIMARY KEY (`shop_id`)
);


-- aib_db.warehouses definition

CREATE TABLE `warehouses` (
  `warehouse_id` int(11) NOT NULL AUTO_INCREMENT,
  `warehouse_name` varchar(255) NOT NULL,
  `city` varchar(255) NOT NULL,
  `country` varchar(255) NOT NULL,
  `is_source` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`warehouse_id`)
);


-- aib_db.borrowings definition

CREATE TABLE `borrowings` (
  `borrowing_id` int(11) NOT NULL AUTO_INCREMENT,
  `fruit_id` int(11) NOT NULL,
  `borrowing_shop_id` int(11) NOT NULL,
  `receiving_shop_id` int(11) NOT NULL,
  `quantity` int(11) NOT NULL,
  `borrowing_date` date NOT NULL,
  `status` varchar(50) NOT NULL,
  PRIMARY KEY (`borrowing_id`),
  KEY `fruit_id` (`fruit_id`),
  KEY `borrowing_shop_id` (`borrowing_shop_id`),
  KEY `receiving_shop_id` (`receiving_shop_id`),
  CONSTRAINT `borrowings_ibfk_1` FOREIGN KEY (`fruit_id`) REFERENCES `fruits` (`fruit_id`),
  CONSTRAINT `borrowings_ibfk_2` FOREIGN KEY (`borrowing_shop_id`) REFERENCES `shops` (`shop_id`),
  CONSTRAINT `borrowings_ibfk_3` FOREIGN KEY (`receiving_shop_id`) REFERENCES `shops` (`shop_id`)
) ;


-- aib_db.deliveries definition

CREATE TABLE `deliveries` (
  `delivery_id` int(11) NOT NULL AUTO_INCREMENT,
  `fruit_id` int(11) NOT NULL,
  `from_warehouse_id` int(11) NOT NULL,
  `to_warehouse_id` int(11) NOT NULL,
  `quantity` int(11) NOT NULL,
  `delivery_date` date NOT NULL,
  `status` varchar(50) NOT NULL,
  PRIMARY KEY (`delivery_id`),
  KEY `fruit_id` (`fruit_id`),
  KEY `from_warehouse_id` (`from_warehouse_id`),
  KEY `to_warehouse_id` (`to_warehouse_id`),
  CONSTRAINT `deliveries_ibfk_1` FOREIGN KEY (`fruit_id`) REFERENCES `fruits` (`fruit_id`),
  CONSTRAINT `deliveries_ibfk_2` FOREIGN KEY (`from_warehouse_id`) REFERENCES `warehouses` (`warehouse_id`),
  CONSTRAINT `deliveries_ibfk_3` FOREIGN KEY (`to_warehouse_id`) REFERENCES `warehouses` (`warehouse_id`)
) ;

-- aib_db.inventory definition

CREATE TABLE `inventory` (
  `inventory_id` int(11) NOT NULL AUTO_INCREMENT,
  `fruit_id` int(11) NOT NULL,
  `shop_id` int(11) DEFAULT NULL,
  `warehouse_id` int(11) DEFAULT NULL,
  `quantity` int(11) NOT NULL,
  PRIMARY KEY (`inventory_id`),
  KEY `fruit_id` (`fruit_id`),
  KEY `shop_id` (`shop_id`),
  KEY `warehouse_id` (`warehouse_id`),
  CONSTRAINT `inventory_ibfk_1` FOREIGN KEY (`fruit_id`) REFERENCES `fruits` (`fruit_id`),
  CONSTRAINT `inventory_ibfk_2` FOREIGN KEY (`shop_id`) REFERENCES `shops` (`shop_id`),
  CONSTRAINT `inventory_ibfk_3` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses` (`warehouse_id`)
);


-- aib_db.reservations definition

CREATE TABLE `reservations` (
  `reservation_id` int(11) NOT NULL AUTO_INCREMENT,
  `fruit_id` int(11) NOT NULL,
  `shop_id` int(11) NOT NULL,
  `quantity` int(11) NOT NULL,
  `reservation_date` date NOT NULL,
  `status` varchar(50) NOT NULL,
  PRIMARY KEY (`reservation_id`),
  KEY `fruit_id` (`fruit_id`),
  KEY `shop_id` (`shop_id`),
  CONSTRAINT `reservations_ibfk_1` FOREIGN KEY (`fruit_id`) REFERENCES `fruits` (`fruit_id`),
  CONSTRAINT `reservations_ibfk_2` FOREIGN KEY (`shop_id`) REFERENCES `shops` (`shop_id`)
) ;

-- aib_db.users definition

CREATE TABLE `users` (
  `user_id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` varchar(50) NOT NULL,
  `shop_id` int(11) DEFAULT NULL,
  `warehouse_id` int(11) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `hash_version` varchar(255) DEFAULT NULL,
  `userEmail` varchar(100) NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`),
  KEY `shop_id` (`shop_id`),
  KEY `warehouse_id` (`warehouse_id`),
  CONSTRAINT `users_ibfk_1` FOREIGN KEY (`shop_id`) REFERENCES `shops` (`shop_id`),
  CONSTRAINT `users_ibfk_2` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses` (`warehouse_id`)
) ;
INSERT INTO aib_db.shops (shop_name,city,country) VALUES
	 ('Bakery Shop A','Tokyo','Japan'),
	 ('Bakery Shop B','Osaka','Japan'),
	 ('Bakery Shop C','New York','USA'),
	 ('Bakery Shop D','Los Angeles','USA'),
	 ('Bakery Shop E','Hong Kong','Hong Kong');

INSERT INTO aib_db.fruits (fruit_name,source_country) VALUES
	 ('Apple','USA'),
	 ('Banana','Ecuador'),
	 ('Strawberry','Japan'),
	 ('Orange','Spain');

INSERT INTO aib_db.warehouses (warehouse_name,city,country,is_source) VALUES
	 ('Warehouse 1','Tokyo','Japan',1),
	 ('Warehouse 2','New York','USA',1),
	 ('Warehouse 3','Hong Kong','Hong Kong',1),
	 ('Warehouse 4','Osaka','Japan',0),
	 ('Warehouse 5','Los Angeles','USA',0);   

INSERT INTO aib_db.borrowings (fruit_id,borrowing_shop_id,receiving_shop_id,quantity,borrowing_date,status) VALUES
	 (1,1,2,5,'2025-03-25','Returned'),
	 (2,2,1,3,'2025-03-26','Approved');

INSERT INTO aib_db.deliveries (fruit_id,from_warehouse_id,to_warehouse_id,quantity,delivery_date,status) VALUES
	 (1,1,4,200,'2025-03-28','Shipped'),
	 (2,2,5,150,'2025-03-29','Delivered');

INSERT INTO aib_db.inventory (fruit_id,shop_id,warehouse_id,quantity) VALUES
	 (1,1,NULL,50),
	 (2,1,NULL,30),
	 (1,2,NULL,40),
	 (3,1,NULL,20),
	 (1,NULL,1,1000),
	 (2,NULL,1,800),
	 (1,NULL,2,1200),
	 (4,3,NULL,60);

INSERT INTO aib_db.reservations (fruit_id,shop_id,quantity,reservation_date,status) VALUES
	 (1,1,10,'2025-04-01','Submitted'),
	 (2,2,5,'2025-04-01','Approved'),
	 (3,5,2,'2025-04-05','Submitted');

INSERT INTO aib_db.users (username,password,`role`,shop_id,warehouse_id,created_at,updated_at,hash_version,userEmail) VALUES
	 ('shop_user1','shop_user1pwd','Bakery shop staff',1,NULL,'2025-03-12 22:34:54','2025-03-24 23:11:08','1','shop_user1@gmail.com'),
	 ('shop_user2','shop_user2pwd','Bakery shop staff',2,NULL,'2025-03-12 22:34:54','2025-03-24 23:11:08','1','shop_user2@gmail.com'),
	 ('warehouse_user1','warehouse_user1pwd','Warehouse Staff',NULL,1,'2025-03-12 22:34:54','2025-03-24 23:11:08','1','warehouse_user1@gmail.com'),
	 ('warehouse_user2','warehouse_user2pwd','Warehouse Staff',NULL,2,'2025-03-12 22:34:54','2025-03-24 23:11:08','1','warehouse_user2@gmail.com'),
	 ('manager1','manager1pwd','Senior Management',NULL,NULL,'2025-03-12 22:34:54','2025-03-24 23:11:08','1','manager1@gmail.com'),
	 ('shop_user3','shop_user3pwd','Bakery shop staff',5,NULL,'2025-03-12 22:34:54','2025-03-24 23:11:08','1','shop_user3@gmail.com');
