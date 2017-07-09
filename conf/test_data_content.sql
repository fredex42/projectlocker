# -- !Ups
SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

SET search_path = public, pg_catalog;

BEGIN;

SET CONSTRAINTS ALL DEFERRED;

INSERT INTO "StorageEntry" (id, rootpath, "storageType", "user", password, host, port) VALUES (1, NULL, 'filesystem', 'me', NULL, NULL, NULL);
INSERT INTO "StorageEntry" (id, rootpath, "storageType", "user", password, host, port) VALUES (2, '/backups/projectfiles', 'ftp', 'me', '123456abcde', 'ftp.mysite.com', 21);
INSERT INTO "StorageEntry" (id, rootpath, "storageType", "user", password, host, port) VALUES (3, '/backups/projectfiles', 'ftp', 'me', '123456abcde', 'ftp.mysite.com', 21);

INSERT INTO "FileEntry" (id, filepath, storage, "user", version, ctime, mtime, atime) VALUES (1, '/path/to/a/video.mxf', 2, 'me', 1, '2017-01-17 16:55:00.123', '2017-01-17 16:55:00.123', '2017-01-17 16:55:00.123');
INSERT INTO "FileEntry" (id, filepath, storage, "user", version, ctime, mtime, atime) VALUES (2, '/path/to/a/file.project', 1, 'you', 1, '2016-12-11 12:21:11.021', '2016-12-11 12:21:11.021', '2016-12-11 12:21:11.021');
INSERT INTO "FileEntry" (id, filepath, storage, "user", version, ctime, mtime, atime) VALUES (3, '/path/to/another/file.project', 1, 'you', 1, '2016-12-11 12:21:11.021', '2016-12-11 12:21:11.021', '2016-12-11 12:21:11.021');

SELECT pg_catalog.setval('"FileEntry_id_seq"', 3, true);

INSERT INTO "ProjectType" (id, name, "opensWith", "targetVersion") VALUES (1, 'Premiere 2014 test', 'AdobePremierePro.app', '14.0');
INSERT INTO "ProjectType" (id, name, "opensWith", "targetVersion") VALUES (2, 'Prelude 2014 test', 'AdobePrelude.app', '14.0');
INSERT INTO "ProjectType" (id, name, "opensWith", "targetVersion") VALUES (3, 'Cubase test', 'Cubase.app', '6.0');

INSERT INTO "ProjectFileAssociation" (id, "ProjectEntry", "FileEntry") VALUES (1, 1,2);
SELECT pg_catalog.setval('"ProjectFileAssociation_id_seq"', 2, false);

INSERT INTO "ProjectEntry" (id, "ProjectFileAssociation", "ProjectType", "created","user") VALUES (1, 1, 1, '2016-12-11 12:21:11.021', 'me');
SELECT pg_catalog.setval('"ProjectEntry_id_seq"', 2, false);


INSERT INTO "ProjectTemplate" (id, name, "ProjectType", filepath, storage) VALUES (1, 'Premiere test template 1', 1, '/srv/projectfiles/ProjectTemplatesDev/Premiere/premiere_template_2014.prproj', 1);
INSERT INTO "ProjectTemplate" (id, name, "ProjectType", filepath, storage) VALUES (2, 'Another wonderful test template', 2, '/srv/projectfiles/ProjectTemplatesDev/Premiere/prelude_template_2014.prproj', 1);



SELECT pg_catalog.setval('"ProjectTemplate_id_seq"', 2, true);



SELECT pg_catalog.setval('"ProjectType_id_seq"', 3, true);



SELECT pg_catalog.setval('"StorageEntry_id_seq"', 3, true);

COMMIT;