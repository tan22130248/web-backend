-- =====================================================================
-- One-time repair: align FK column type + collation with parent tables
-- =====================================================================
-- Root cause:
--   The legacy tables (users, orders, shops) use char(36) /
--   utf8mb4_unicode_ci. Hibernate (ddl-auto=update) auto-created
--   seller_registrations, order_status_history and shop_settings with
--   MySQL 8's default varchar(255) / utf8mb4_0900_ai_ci for their FK
--   columns. MySQL then refuses both the foreign key (incompatible
--   type) and any join (illegal mix of collations).
--
--   ddl-auto=update never ALTERs existing columns, so the entity
--   annotations only affect *fresh* creation. This script repairs the
--   columns that already exist in the database.
--
-- How to run (adjust db name / credentials to match your .env DB_URL):
--   mysql -u root -p fashion_new < docs/fix-fk-collation.sql
-- =====================================================================

-- order_status_history.order_id  ->  orders.id
ALTER TABLE order_status_history
    MODIFY COLUMN order_id CHAR(36)
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;

-- seller_registrations.user_id  ->  users.id
ALTER TABLE seller_registrations
    MODIFY COLUMN user_id CHAR(36)
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;

-- shop_settings.shop_id  ->  shops.id
ALTER TABLE shop_settings
    MODIFY COLUMN shop_id CHAR(36)
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;

-- After the columns match, Hibernate will be able to add the foreign
-- keys on the next startup. If you want them now, uncomment below
-- (ignore "Duplicate key" if a constraint already exists):
--
-- ALTER TABLE order_status_history
--     ADD CONSTRAINT fk_osh_order FOREIGN KEY (order_id) REFERENCES orders (id);
-- ALTER TABLE seller_registrations
--     ADD CONSTRAINT fk_sr_user FOREIGN KEY (user_id) REFERENCES users (id);
-- ALTER TABLE shop_settings
--     ADD CONSTRAINT fk_ss_shop FOREIGN KEY (shop_id) REFERENCES shops (id);
