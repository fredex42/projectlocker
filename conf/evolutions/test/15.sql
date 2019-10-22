# --!Ups
ALTER TABLE "StorageEntry" ADD COLUMN S_DEVICE VARCHAR(128) NULL;
ALTER TABLE "StorageEntry" ADD COLUMN B_VERSIONS BOOLEAN DEFAULT FALSE;
ALTER TABLE "StorageEntry" ADD COLUMN S_NICKNAME VARCHAR(128) NULL;
DROP INDEX IX_PATH_STORAGE;
CREATE UNIQUE INDEX IX_PATHVERS_STORAGE ON "FileEntry" (S_FILEPATH, K_STORAGE_ID, I_VERSION);

# --!Downs
ALTER TABLE "StorageEntry" DROP COLUMN S_DEVICE;
ALTER TABLE "StorageEntry" DROP COLUMN B_VERSIONS;
ALTER TABLE "StorageEntry" DROP COLUMN S_NICKNAME;
DROP INDEX IX_PATHVERS_STORAGE;
CREATE UNIQUE INDEX IX_PATH_STORAGE ON "FileEntry" (S_FILEPATH, K_STORAGE_ID);