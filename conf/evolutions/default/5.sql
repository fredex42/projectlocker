# -- !Ups

CREATE TABLE "PlutoWorkingGroup" (
  id INTEGER NOT NULL PRIMARY KEY,
  S_HIDE CHARACTER VARYING NULL,
  S_NAME CHARACTER VARYING NOT NULL,
  U_UUID CHARACTER VARYING NOT NULL UNIQUE
);

CREATE SEQUENCE "PlutoWorkingGroup_id_seq"
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;
ALTER SEQUENCE "PlutoWorkingGroup_id_seq" OWNED BY "PlutoWorkingGroup".id;

ALTER TABLE public."PlutoWorkingGroup" OWNER TO projectlocker;
ALTER TABLE "PlutoWorkingGroup_id_seq" OWNER TO projectlocker;

ALTER TABLE ONLY "PlutoWorkingGroup" ALTER COLUMN id SET DEFAULT nextval('"PlutoWorkingGroup_id_seq"'::regclass);

CREATE TABLE "PlutoCommission" (
  id INTEGER NOT NULL PRIMARY KEY,
  I_COLLECTION_ID INTEGER NOT NULL,
  S_SITE_ID CHARACTER VARYING NOT NULL,
  T_CREATED TIMESTAMP WITH TIME ZONE NOT NULL,
  T_UPDATED TIMESTAMP WITH TIME ZONE NOT NULL,
  S_TITLE CHARACTER VARYING NOT NULL,
  S_STATUS CHARACTER VARYING NOT NULL,
  S_DESCRIPTION CHARACTER VARYING NULL,
  K_WORKING_GROUP INTEGER NOT NULL,
  UNIQUE (S_SITE_ID, I_COLLECTION_ID)
);

CREATE INDEX IX_COLLECTION_ID ON "PlutoCommission" (I_COLLECTION_ID);
CREATE INDEX IX_STATUS ON "PlutoCommission" (S_DESCRIPTION);

ALTER TABLE "PlutoCommission" ADD CONSTRAINT "fk_workinggroup" FOREIGN KEY (K_WORKING_GROUP) REFERENCES "PlutoWorkingGroup"(id) DEFERRABLE INITIALLY DEFERRED;

CREATE SEQUENCE "PlutoCommission_id_seq"
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;
ALTER SEQUENCE "PlutoCommission_id_seq" OWNED BY "PlutoCommission".id;

ALTER TABLE public."PlutoCommission" OWNER TO projectlocker;
ALTER TABLE "PlutoCommission_id_seq" OWNER TO projectlocker;

ALTER TABLE ONLY "PlutoCommission" ALTER COLUMN id SET DEFAULT nextval('"PlutoCommission_id_seq"'::regclass);

ALTER TABLE "ProjectEntry" ADD COLUMN K_WORKING_GROUP INTEGER NULL;
ALTER TABLE "ProjectEntry" ADD COLUMN K_COMMISSION INTEGER NULL;
ALTER TABLE "ProjectEntry" ADD CONSTRAINT FK_WORKING_GROUP FOREIGN KEY (K_WORKING_GROUP) REFERENCES "PlutoWorkingGroup"(id) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE "ProjectEntry" ADD CONSTRAINT FK_COMMISSION FOREIGN KEY (K_COMMISSION) REFERENCES "PlutoCommission"(id) DEFERRABLE INITIALLY DEFERRED;

# -- !Downs
ALTER TABLE "ProjectEntry" DROP CONSTRAINT FK_WORKING_GROUP;
ALTER TABLE "ProjectEntry" DROP CONSTRAINT FK_COMMISSION;
ALTER TABLE "ProjectEntry" DROP COLUMN K_WORKING_GROUP;
ALTER TABLE "ProjectEntry" DROP COLUMN K_COMMISSION;
DROP TABLE "PlutoCommission" CASCADE ;
DROP TABLE "PlutoWorkingGroup" CASCADE ;
