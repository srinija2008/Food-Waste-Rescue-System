-- Run this script in MySQL Workbench or the MySQL command line.
CREATE DATABASE IF NOT EXISTS food_rescue_db;
USE food_rescue_db;

CREATE TABLE IF NOT EXISTS donations (
    id INT PRIMARY KEY AUTO_INCREMENT,
    donor_name VARCHAR(100) NOT NULL,
    food_name VARCHAR(120) NOT NULL,
    quantity_kg DECIMAL(10,2) NOT NULL,
    location VARCHAR(200) NOT NULL,
    deadline DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    claimed_by VARCHAR(100) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DROP PROCEDURE IF EXISTS donation_status_summary;
DELIMITER //
CREATE PROCEDURE donation_status_summary()
BEGIN
    SELECT status, COUNT(*) AS total
    FROM donations
    GROUP BY status
    ORDER BY status;
END //
DELIMITER ;
