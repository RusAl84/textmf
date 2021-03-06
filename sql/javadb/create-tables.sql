create table WORDS
  (WORD varchar(25) NOT NULL,
  PROBABILITY double NOT NULL,
  ABSTRACTION_LEVEL double NOT NULL,
  WORD_TYPE varchar(10) NOT NULL,
  PRIMARY KEY (WORD));

create table WORDS_FROMS
  (WORDFORM varchar(25) NOT NULL,
  ORIGINAL_WORD varchar(25) NOT NULL,
  PROBABILITY double NOT NULL,
  FOREIGN KEY (ORIGINAL_WORD) REFERENCES WORDS (WORD),
  PRIMARY KEY (WORDFORM));

create table CONNECTIONS
  (SUP_ID integer NOT NULL,
  WORD1 varchar(25) NOT NULL,
  WORD2 varchar(25) NOT NULL,
  DISTANCE double NOT NULL,
  PROBABILITY double NOT NULL,
  FOREIGN KEY (WORD1) REFERENCES WORDS (WORD),
  FOREIGN KEY (WORD2) REFERENCES WORDS (WORD),
  PRIMARY KEY (SUP_ID));