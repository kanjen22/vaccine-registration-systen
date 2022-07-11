CREATE TABLE Caregivers (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Patients (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Vaccines (
    Name varchar(255),
    Doses int,
    PRIMARY KEY (Name)
);

CREATE TABLE Availabilities (
    CaregiverName varchar(255) REFERENCES Caregivers(Username),
    PatientName varchar(255) REFERENCES Patients(Username),
    VaccineName varchar(255) REFERENCES Vaccines(Name),
    Time date,
    ID int PRIMARY KEY,
    UNIQUE(CaregiverName, Time),
    CHECK (CaregiverName IS NOT NULL AND Time IS NOT NULL AND ID IS NOT NULL)
);