-- Migration V4: Create comment_replies table for shop owner replies

CREATE TABLE IF NOT EXISTS comment_replies (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    comment_id CHAR(36) NOT NULL,
    shop_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_comment_replies_comment 
        FOREIGN KEY (comment_id) REFERENCES product_comments(id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_comment_replies_shop 
        FOREIGN KEY (shop_id) REFERENCES shops(id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_comment_replies_user 
        FOREIGN KEY (user_id) REFERENCES users(id) 
        ON DELETE CASCADE,
    
    -- Index for better query performance
    INDEX idx_comment_replies_comment_id (comment_id),
    INDEX idx_comment_replies_shop_id (shop_id),
    INDEX idx_comment_replies_created_at (created_at),
    
    -- Ensure shop can only reply once per comment
    UNIQUE KEY uk_comment_shop_reply (comment_id, shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;