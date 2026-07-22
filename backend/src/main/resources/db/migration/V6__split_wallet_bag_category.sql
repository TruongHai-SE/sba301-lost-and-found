-- Migration V6: Split WALLET_BAG into WALLET and BAG_BACKPACK categories

UPDATE stock_images SET category = 'WALLET' WHERE label = 'Wallet';
UPDATE stock_images SET category = 'BAG_BACKPACK' WHERE label = 'Bag';
UPDATE posts SET category = 'WALLET' WHERE category = 'WALLET_BAG';
