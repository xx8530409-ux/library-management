-- 圖書館借閱管理系統：新增「續借」功能所需的資料表結構異動
-- 請在 MySQL 中執行此語句更新 loans 資料表

USE library_db;

ALTER TABLE loans 
ADD COLUMN renew_count INT NOT NULL DEFAULT 0 COMMENT '續借次數 (每筆借閱限續借 1 次)';
