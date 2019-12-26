USE szz;

DROP TABLE refdiffresult;

CREATE TABLE refdiffresult(
    revision VARCHAR(63),
    project VARCHAR(63),
    summary TEXT,
	refactoringtype VARCHAR(127),
	entityafter TEXT,
	entitybefore TEXT,
	elementtype VARCHAR(127),
	callers BIGINT,
	afterstartline BIGINT,
	afterendline BIGINT,
	afterpathfile TEXT,
	beforestartline BIGINT,
	beforeendline BIGINT,
	beforepathfile TEXT,
	afterstartscope BIGINT,
	aftersimpleName VARCHAR(511),
	aftercontent TEXT,
	afternestinglevel BIGINT,
	revisiontype VARCHAR(63),
	beforestartscope BIGINT,
	beforesimpleName VARCHAR(511),
	beforecontent TEXT,
	beforenestinglevel BIGINT,
	tool VARCHAR(63)
);

DROP TABLE callerrefdiff;

CREATE TABLE callerrefdiff(
    revision VARCHAR(63),
    project VARCHAR(63),
    summary TEXT,
	entityafter TEXT,
	callermethod TEXT,
	callerstartline BIGINT,
	callerendline BIGINT,
	callerpath TEXT,
	refactoringtype VARCHAR(127),
	callerline BIGINT,
	simplename VARCHAR(511),
	nestinglevel BIGINT,
	revisiontype VARCHAR(63),
	type VARCHAR(63),
	tool VARCHAR(63)
);

DROP TABLE szz_refac_revisionprocessed;

CREATE TABLE szz_refac_revisionprocessed(
    project VARCHAR(63),
    revision VARCHAR(63),
    tool VARCHAR(63)
);

DROP TABLE linkedissuegit;

CREATE TABLE linkedissuegit(
    fix_revision VARCHAR(63),
    project VARCHAR(63)
);

DROP TABLE szz_project_lastrevisionprocessed;

CREATE TABLE szz_project_lastrevisionprocessed(
    project VARCHAR(63),
    lastrevisionprocessed VARCHAR(63)
);

DROP TABLE bicraszzold;

CREATE TABLE bicraszzold (
    linenumber INTEGER,
    path TEXT,
    content TEXT,
    revision VARCHAR(63),
	fixrevision VARCHAR(63),
	project VARCHAR(63),
	szz_date TIMESTAMP,
	mergerev BOOLEAN,
	branchrev BOOLEAN,
	changeproperty BOOLEAN,
	missed BOOLEAN,
	furtherback BOOLEAN,
	diffjmessage TEXT,
	diffjlocation TEXT,
	adjustmentindex INTEGER,
	indexposrefac INTEGER,
	indexchangepath INTEGER,
	isrefac BOOLEAN,
	indexfurtherback INTEGER,
	startrevision VARCHAR(63),
	startpath TEXT,
	startlinenumber INTEGER,
	startcontent TEXT);

DROP TABLE bicmaszztest;

CREATE TABLE bicmaszztest(
    linenumber INTEGER,
    path TEXT,
    content TEXT,
    revision VARCHAR(63),
	fixrevision VARCHAR(63),
	project VARCHAR(63),
	szz_date TIMESTAMP,
	mergerev BOOLEAN,
	branchrev BOOLEAN,
	changeproperty BOOLEAN,
	missed BOOLEAN,
	furtherback BOOLEAN,
	diffjmessage TEXT,
	diffjlocation TEXT,
	indexfurtherback INTEGER,
	startrevision VARCHAR(63),
	startpath TEXT,
	startlinenumber INTEGER,
	startcontent TEXT);





