CREATE DATABASE `aib_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */;

-- aib_db.fruits definition

CREATE TABLE `fruits` (
  `fruit_id` int(11) NOT NULL AUTO_INCREMENT,
  `fruit_name` varchar(255) NOT NULL,
  `source_country` varchar(255) NOT NULL,
  PRIMARY KEY (`fruit_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


-- aib_db.shops definition

CREATE TABLE `shops` (
  `shop_id` int(11) NOT NULL AUTO_INCREMENT,
  `shop_name` varchar(255) NOT NULL,
  `city` varchar(255) NOT NULL,
  `country` varchar(255) NOT NULL,
  PRIMARY KEY (`shop_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


-- aib_db.warehouses definition

CREATE TABLE `warehouses` (
  `warehouse_id` int(11) NOT NULL AUTO_INCREMENT,
  `warehouse_name` varchar(255) NOT NULL,
  `city` varchar(255) NOT NULL,
  `country` varchar(255) NOT NULL,
  `is_source` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`warehouse_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


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
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


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
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


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
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;