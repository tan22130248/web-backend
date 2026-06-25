-- Migration V5: Create review_replies table for shop owner replies to reviews

CREATE TABLE IF NOT EXISTS review_replies (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    review_id CHAR(36) NOT NULL,
    shop_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_review_replies_review 
        FOREIGN KEY (review_id) REFERENCES reviews(id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_review_replies_shop 
        FOREIGN KEY (shop_id) REFERENCES shops(id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_review_replies_user 
        FOREIGN KEY (user_id) REFERENCES users(id) 
        ON DELETE CASCADE,
    
    -- Index for better query performance
    INDEX idx_review_replies_review_id (review_id),
    INDEX idx_review_replies_shop_id (shop_id),
    INDEX idx_review_replies_created_at (created_at),
    
    -- Ensure shop can only reply once per review
    UNIQUE KEY uk_review_shop_reply (review_id, shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;