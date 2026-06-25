-- Migration: Add CCCD URL columns to shops
ALTER TABLE `shops`
  ADD COLUMN `cccd_front_url` VARCHAR(500) DEFAULT NULL,
  ADD COLUMN `cccd_back_url` VARCHAR(500) DEFAULT NULL;
