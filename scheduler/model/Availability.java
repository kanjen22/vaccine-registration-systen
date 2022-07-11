package scheduler.model;

import scheduler.db.ConnectionManager;

import java.sql.*;
import java.util.Random;

public class Availability {
    private final String caregiverName;
    private String patientName;
    private String vaccineName;
    private final Date date;
    private final int ID;

    private Availability(AvailabilityBuilder builder) {
        this.caregiverName = builder.caregiverName;
        this.patientName = null;
        this.vaccineName = null;
        this.date = builder.date;
        this.ID = builder.ID;
    }

    private Availability(AvailabilityGetter getter) {
        this.caregiverName = getter.caregiverName;
        this.patientName = getter.patientName;
        this.vaccineName = getter.vaccineName;
        this.date = getter.date;
        this.ID = getter.ID;
    }

    // Output appointment detail
    public void printAppointmentDetail() {
        if (this.patientName != null) {
            System.out.println("=====================================");
            System.out.printf("Caregiver name: %s\n", getCaregiverName());
            System.out.printf("Appointment ID: %d\n", getID());
            System.out.println("=====================================");
        } else {
            System.out.println("This slot is not booked yet!");
        }
    }

    // Getters
    public String getCaregiverName() {
        return caregiverName;
    }

    public String getPatientName() {
        return caregiverName;
    }

    public String getVaccineName() {
        return caregiverName;
    }

    public Date getDate() {
        return date;
    }

    public int getID() {
        return ID;
    }

    public void saveToDB() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addAvailability = "INSERT INTO Availabilities VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addAvailability);
            statement.setString(1, this.caregiverName);
            statement.setString(2, this.patientName);   // might be null
            statement.setString(3, this.vaccineName);   // might be null
            statement.setDate(4, this.date);
            statement.setInt(5, this.ID);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public void bookAppointment(String patientName, String vaccineName) throws SQLException {
        // update Availability Object fields
        this.patientName = patientName;
        this.vaccineName = vaccineName;

        // SQL query
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String bookAppointment  = "UPDATE Availabilities SET PatientName = ?, VaccineName = ?  " +
                "WHERE CaregiverName = ? AND Time = ?;";
        try {
            PreparedStatement statement = con.prepareStatement(bookAppointment);
            statement.setString(1, patientName);
            statement.setString(2, vaccineName);
            statement.setString(3, this.caregiverName);
            statement.setDate(4, this.date);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public void cancelAppointment(String canceller, int ID) throws SQLException {
        // update Availability Object fields
        this.patientName = null;
        this.vaccineName = null;

        // SQL query
        String cancelAppointment = "";
        if (canceller.equals("Caregiver")) {    // delete the entire tuple
            cancelAppointment  = "DELETE Availabilities WHERE ID = ? ";
        } else if (canceller.equals("Patient")) {
            cancelAppointment  = "UPDATE Availabilities SET PatientName = ?, VaccineName = ?  " +
                    "WHERE ID = ?";
        }
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        try {
            PreparedStatement statement = con.prepareStatement(cancelAppointment);
            if (canceller.equals("Caregiver")) {    // delete the entire tuple
                statement.setInt(1, ID);
            } else if (canceller.equals("Patient")) {   // update tuple
                statement.setString(1, patientName);
                statement.setString(2, vaccineName);
                statement.setInt(3, ID);
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public static class AvailabilityBuilder {
        private final String caregiverName;
        private final Date date;
        private final int ID;

        public AvailabilityBuilder(String caregiverName, Date date) throws SQLException {
            this.caregiverName = caregiverName;
            this.date = date;
            this.ID = generateID();
        }

        public Availability build() {
            return new Availability(this);
        }

        private int generateID() throws SQLException {
            // generate non-duplicate ID
            boolean generate = true;
            Random r = new Random();
            int ID = -1;
            while(generate) {
                ID = r.nextInt(10000) + 1;
                ConnectionManager cm = new ConnectionManager();
                Connection con = cm.createConnection();
                String getAppointment = "SELECT * FROM Availabilities WHERE ID = ?";
                try {
                    PreparedStatement statement = con.prepareStatement(getAppointment);
                    statement.setInt(1, ID);
                    ResultSet resultSet = statement.executeQuery();
                    generate = resultSet.isBeforeFirst();
                } catch (SQLException e) {
                    throw new SQLException();
                } finally {
                    cm.closeConnection();
                }
            }
            return ID;
        }
    }

    public static class AvailabilityGetter {
        private final String caregiverName;
        private String patientName;
        private String vaccineName;
        private final Date date;
        private int ID;

        public AvailabilityGetter(String caregiverName, Date date) {
            this.caregiverName = caregiverName;
            this.date = date;
        }

        public Availability get() throws SQLException {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String getAvailability = "SELECT * FROM Availabilities WHERE caregiverName = ? AND Time = ?";
            try {
                PreparedStatement statement = con.prepareStatement(getAvailability);
                statement.setString(1, this.caregiverName);
                statement.setDate(2, this.date);
                ResultSet resultSet = statement.executeQuery();
                // check if this availability actually exists
                while (resultSet.next()) {
                    this.patientName = resultSet.getString("PatientName");
                    this.vaccineName = resultSet.getString("VaccineName");
                    this.ID = resultSet.getInt("ID");
                    return new Availability(this);
                }
                return null;
            } catch (SQLException e) {
                throw new SQLException();
            } finally {
                cm.closeConnection();
            }
        }
    }
}
