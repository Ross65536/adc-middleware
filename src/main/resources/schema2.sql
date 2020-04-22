
DROP TABLE Study IF EXISTS;
-- This table is important to persist
CREATE TABLE Study (
    id INT AUTO_INCREMENT PRIMARY KEY,
    study_id VARCHAR(128) UNIQUE NOT NULL,
    uma_id VARCHAR(128) UNIQUE NOT NULL
);

DROP TABLE Repertoire IF EXISTS;
CREATE TABLE Repertoire (
    id INT AUTO_INCREMENT PRIMARY KEY,
    repertoire_id VARCHAR(128) UNIQUE NOT NULL,
    study_id INT NOT NULL,
    FOREIGN KEY (study_id) REFERENCES Study(id) ON DELETE CASCADE
);

DROP TABLE Rearrangement IF EXISTS;
CREATE TABLE Rearrangement (
    id INT AUTO_INCREMENT PRIMARY KEY,
    rearrangement_id VARCHAR(128) UNIQUE NOT NULL,
    repertoire_id INT NOT NULL,
    FOREIGN KEY (repertoire_id) REFERENCES Repertoire(id) ON DELETE CASCADE
);
