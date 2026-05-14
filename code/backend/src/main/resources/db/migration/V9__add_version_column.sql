-- V9: 为 entries 表添加乐观锁 version 列
ALTER TABLE entries ADD COLUMN version INT NOT NULL DEFAULT 0;
