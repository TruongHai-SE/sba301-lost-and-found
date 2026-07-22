-- Migration V5: Add stock_images table and is_stock_image flag to posts

CREATE TABLE IF NOT EXISTS stock_images (
    id          BIGSERIAL PRIMARY KEY,
    category    VARCHAR(30) NOT NULL,
    image_url   TEXT NOT NULL,
    label       VARCHAR(100),
    created_at  TIMESTAMP DEFAULT NOW()
);

ALTER TABLE posts ADD COLUMN IF NOT EXISTS is_stock_image BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_stock_images_category ON stock_images(category);

-- Seed stock images from Cloudinary
INSERT INTO stock_images (category, image_url, label) VALUES
    ('DOCS_CARDS',       'https://res.cloudinary.com/dns0wbaea/image/upload/v1784732068/CCCD-CMND.png', 'CCCD / CMND'),
    ('WALLET_BAG',       'https://res.cloudinary.com/dns0wbaea/image/upload/v1784732067/Wallet.png',    'Wallet'),
    ('APPAREL_ACC',      'https://res.cloudinary.com/dns0wbaea/image/upload/v1784732067/Apparel.png',   'Apparel'),
    ('PETS',             'https://res.cloudinary.com/dns0wbaea/image/upload/v1784732067/Animal.png',    'Animal'),
    ('KEYS',             'https://res.cloudinary.com/dns0wbaea/image/upload/v1784732067/Key.png',       'Key'),
    ('WALLET_BAG',       'https://res.cloudinary.com/dns0wbaea/image/upload/v1784732065/Bag.jpg',       'Bag'),
    ('ELECTRONICS',      'https://res.cloudinary.com/dns0wbaea/image/upload/v1784732065/Electronics.jpg', 'Electronics'),
    ('BOOKS_STATIONERY', 'https://res.cloudinary.com/dns0wbaea/image/upload/v1784732065/Books.jpg',     'Books');
