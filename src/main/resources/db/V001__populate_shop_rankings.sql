-- Populate shop_rankings with 50 sample shops from CSV list
-- Run this script to auto-populate rankings data

INSERT INTO shop_rankings (id, shop_id, total_points, tier, rank_position, period_month, updated_at)
VALUES 
(UUID(), '009427f1-c388-5f58-bacd-744400e79a62', 92, 'platinum', 1, LAST_DAY(CURDATE()), NOW()),
(UUID(), '00f4df19-0815-5912-b11d-1d2461fadb72', 88, 'gold', 2, LAST_DAY(CURDATE()), NOW()),
(UUID(), '01043527-d9a1-5b4d-8d50-dd64d0eace95', 85, 'gold', 3, LAST_DAY(CURDATE()), NOW()),
(UUID(), '015c8460-6e18-57de-a540-94ba1e4dada3', 78, 'silver', 4, LAST_DAY(CURDATE()), NOW()),
(UUID(), '021dd5f9-77c8-5470-b3c6-f2fb36c4eb27', 82, 'gold', 5, LAST_DAY(CURDATE()), NOW()),
(UUID(), '02469d95-4929-524d-8d71-dfaacbb4656d', 76, 'silver', 6, LAST_DAY(CURDATE()), NOW()),
(UUID(), '02b46bb1-ee2e-52b4-8726-37249705399b', 89, 'gold', 7, LAST_DAY(CURDATE()), NOW()),
(UUID(), '02f15c5b-95a9-5c5b-a285-5bcd82f11969', 71, 'silver', 8, LAST_DAY(CURDATE()), NOW()),
(UUID(), '04e8cf1e-ed70-570a-a3c1-df78e15d9106', 84, 'gold', 9, LAST_DAY(CURDATE()), NOW()),
(UUID(), '04f49da1-b4cf-53ec-8271-31b466145f2d', 79, 'silver', 10, LAST_DAY(CURDATE()), NOW()),
(UUID(), '0568d87e-a86f-52dd-912c-672e420a2b9e', 91, 'platinum', 11, LAST_DAY(CURDATE()), NOW()),
(UUID(), '06a9ee4b-a57c-56f2-8ad1-81e2931885d5', 77, 'silver', 12, LAST_DAY(CURDATE()), NOW()),
(UUID(), '06fb83cc-29e9-5dfb-bb13-fe9dbee821ed', 86, 'gold', 13, LAST_DAY(CURDATE()), NOW()),
(UUID(), '07255fc7-1e78-597c-92a7-6ae2fac75a6d', 73, 'silver', 14, LAST_DAY(CURDATE()), NOW()),
(UUID(), '07302abc-4016-54d1-9dcf-a5046e5db232', 90, 'platinum', 15, LAST_DAY(CURDATE()), NOW()),
(UUID(), '07c06713-4fa3-5ae4-9949-33cbfd379cfd', 80, 'gold', 16, LAST_DAY(CURDATE()), NOW()),
(UUID(), '08b6be28-f2b1-577b-964d-963945c2396d', 72, 'silver', 17, LAST_DAY(CURDATE()), NOW()),
(UUID(), '08ed79e9-0d47-57e6-9fef-0c8f0a514e5b', 87, 'gold', 18, LAST_DAY(CURDATE()), NOW()),
(UUID(), '093f0f4e-866f-5fc7-9093-9ce5f1072296', 81, 'gold', 19, LAST_DAY(CURDATE()), NOW()),
(UUID(), '09900b48-cb6c-59e4-9933-0a1b2e4c4687', 75, 'silver', 20, LAST_DAY(CURDATE()), NOW()),
(UUID(), '09f2c886-89a1-5756-864f-c0286a11c75d', 93, 'platinum', 21, LAST_DAY(CURDATE()), NOW()),
(UUID(), '0a73847a-75b1-50d3-a19f-c61dbce8443a', 68, 'silver', 22, LAST_DAY(CURDATE()), NOW()),
(UUID(), '0b085319-9808-5ae2-af64-d8dbbc638ff6', 83, 'gold', 23, LAST_DAY(CURDATE()), NOW()),
(UUID(), '0b226a9d-8eac-5729-a291-c84189c9fa6d', 74, 'silver', 24, LAST_DAY(CURDATE()), NOW()),
(UUID(), '0b412e56-7e58-5988-a5ea-99dcecd99ec5', 95, 'platinum', 25, LAST_DAY(CURDATE()), NOW()),
(UUID(), '0efbb59c-d704-5c6a-9c59-dcef699de572', 69, 'silver', 26, LAST_DAY(CURDATE()), NOW()),
(UUID(), '0f98b36e-8f2c-5496-bc7f-b1e7832a408c', 88, 'gold', 27, LAST_DAY(CURDATE()), NOW()),
(UUID(), '0f9e15c8-6839-5add-997e-553e56e6b14e', 70, 'silver', 28, LAST_DAY(CURDATE()), NOW()),
(UUID(), '100db529-040c-56cb-9d4d-84aa8542b867', 94, 'platinum', 29, LAST_DAY(CURDATE()), NOW()),
(UUID(), '10b20ca3-86e4-5f12-b7eb-8dbc5bb53125', 52, 'bronze', 30, LAST_DAY(CURDATE()), NOW()),
(UUID(), '10c0a40e-8fd0-5321-a4be-23fd0f77c6d8', 85, 'gold', 31, LAST_DAY(CURDATE()), NOW()),
(UUID(), '11412f4e-2168-5639-aba0-5e1066d28492', 67, 'silver', 32, LAST_DAY(CURDATE()), NOW()),
(UUID(), '12785baa-5baa-520b-8944-e0a05dce1f96', 79, 'silver', 33, LAST_DAY(CURDATE()), NOW()),
(UUID(), '12b7587b-ad17-55ae-86e0-1da0ec50c3fa', 58, 'bronze', 34, LAST_DAY(CURDATE()), NOW()),
(UUID(), '12e063c7-e7f7-5338-8f22-bbba34f3294a', 82, 'gold', 35, LAST_DAY(CURDATE()), NOW()),
(UUID(), '1304e5b3-8c6a-5b69-893f-42da6e0b23fa', 64, 'silver', 36, LAST_DAY(CURDATE()), NOW()),
(UUID(), '1361e1f3-1467-5feb-9b2e-520c8f3495d5', 86, 'gold', 37, LAST_DAY(CURDATE()), NOW()),
(UUID(), '138ddb40-158f-5d0e-82b9-81c417c94874', 55, 'bronze', 38, LAST_DAY(CURDATE()), NOW()),
(UUID(), '1390a4e1-678d-572d-adcf-e4b454d125c7', 77, 'silver', 39, LAST_DAY(CURDATE()), NOW()),
(UUID(), '1474ad97-91d5-5527-bcdb-da07197c623a', 51, 'bronze', 40, LAST_DAY(CURDATE()), NOW()),
(UUID(), '15082491-32bc-5b2b-9b01-5c866a5ba1c5', 88, 'gold', 41, LAST_DAY(CURDATE()), NOW()),
(UUID(), '17771e17-e9ff-58fc-bc29-ddd9ca2af367', 62, 'silver', 42, LAST_DAY(CURDATE()), NOW()),
(UUID(), '18340088-69a1-50b5-a897-a0d9986e4332', 84, 'gold', 43, LAST_DAY(CURDATE()), NOW()),
(UUID(), '184d3842-9723-593f-b2ce-67b7cba12633', 48, 'bronze', 44, LAST_DAY(CURDATE()), NOW()),
(UUID(), '18eca0bf-32f2-524e-a29f-c42385670e58', 91, 'platinum', 45, LAST_DAY(CURDATE()), NOW()),
(UUID(), '197ad3e9-ac5f-50d1-8783-9419008a0e51', 59, 'bronze', 46, LAST_DAY(CURDATE()), NOW()),
(UUID(), '19ff1783-d2d2-5b0f-9ddb-0fa3a9ae63cd', 80, 'gold', 47, LAST_DAY(CURDATE()), NOW()),
(UUID(), '1b5bbb68-566a-52f8-ad22-217adaa65707', 65, 'silver', 48, LAST_DAY(CURDATE()), NOW()),
(UUID(), '1c7af11b-d683-55f2-8871-5988e5acdcec', 81, 'gold', 49, LAST_DAY(CURDATE()), NOW()),
(UUID(), '1cee1346-2733-5d29-97d4-3f99a3714714', 50, 'bronze', 50, LAST_DAY(CURDATE()), NOW());

-- Verify insertion
SELECT COUNT(*) as total_rankings FROM shop_rankings;
SELECT sr.*, s.shop_name FROM shop_rankings sr 
LEFT JOIN shops s ON sr.shop_id = s.id 
ORDER BY sr.rank_position 
LIMIT 10;
