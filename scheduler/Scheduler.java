package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.model.Availability;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

public class Scheduler {
    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            // display current login status
            System.out.print("(Current login status: ");
            if (currentCaregiver != null && currentPatient != null) {
                System.out.print("THERES IS AN ERROR WITH LOGIN SYSTEM");
            } else if (currentCaregiver == null && currentPatient == null) {
                System.out.print("user not logged-in yet");
            } else if (currentCaregiver != null) {
                System.out.printf("Caregiver as %s", currentCaregiver.getUsername());
            } else {
                System.out.printf("Patient as %s", currentPatient.getUsername());
            }
            System.out.print(")\n");

            // display menu options
            System.out.println("*** Please enter one of the following commands ***");
            System.out.println("-- NOTE: date is given in YYYY-MM-DD format --");
            System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1) (OK)
            System.out.println("> create_caregiver <username> <password>");
            System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1) (OK)
            System.out.println("> login_caregiver <username> <password>");
            System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2) (OK)
            System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
            System.out.println("> upload_availability <date>");
            System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
            System.out.println("> add_doses <vaccine> <number>");
            System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
            System.out.println("> logout");  // TODO: implement logout (Part 2) (OK)
            System.out.println("> quit");
            System.out.println();
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            switch (operation) {
                case "create_patient" -> createPatient(tokens);
                case "create_caregiver" -> createCaregiver(tokens);
                case "login_patient" -> loginPatient(tokens);
                case "login_caregiver" -> loginCaregiver(tokens);
                case "search_caregiver_schedule" -> searchCaregiverSchedule(tokens);
                case "reserve" -> reserve(tokens);
                case "upload_availability" -> uploadAvailability(tokens);
                case "cancel" -> cancel(tokens);
                case "add_doses" -> addDoses(tokens);
                case "show_appointments" -> showAppointments(tokens);
                case "logout" -> logout(tokens);
                case "quit" -> {
                    System.out.println("Bye!");
                    return;
                }
                default -> System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // TODO: Part 1
        // create_caregiver <username> <password>
        // check 1: if someone else is still logged-in, ask user to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("Please logout first before creating a new account!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 3: check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentPatient.saveToDB();
            System.out.println(" *** Account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed!");
            e.printStackTrace();
        }
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: if someone else is still logged-in, ask user to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("Please logout first before creating a new account!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 3: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println(" *** Account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static String getAvailableCaregiver(Date d) {
        String caregiverName = "";

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String checkBookable = "SELECT TOP 1 CaregiverName FROM Availabilities WHERE Time = ? AND PatientName IS NULL";
        try {
            PreparedStatement statement = con.prepareStatement(checkBookable);
            statement.setDate(1, d);
            ResultSet resultSet = statement.executeQuery();
            while(resultSet.next()) {
                caregiverName = resultSet.getString("CaregiverName");
            }
            return caregiverName;
        } catch (SQLException e) {
            System.out.println("Error occurred when checking availability for this date");
            e.printStackTrace();
            return "SQL failed";
        } finally {
            cm.closeConnection();
        }
    }

    private static void loginPatient(String[] tokens) {
        // TODO: Part 1
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("Already logged-in!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) { // means login unsuccessful
            System.out.println("Please try again!");
        } else {
            System.out.println("Patient logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("Already logged-in!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Caregiver logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        // TODO: Part 2
        // search_caregiver_schedule <date>
        // check 1: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        // check 2: if user hasn't logged-in, they need to log in first
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first to check caregiver schedule!");
            return;
        }
        String date = tokens[1];
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectCaregiver = "SELECT CaregiverName FROM Availabilities WHERE Time = ? AND PatientName IS NULL";
        String selectVaccine = "SELECT Name, Doses FROM Vaccines";
        try {
            // Display available caregiver
            Date d = Date.valueOf(date);
            PreparedStatement statement_1 = con.prepareStatement(selectCaregiver);
            statement_1.setDate(1, d);
            ResultSet resultSet_1 = statement_1.executeQuery();
            System.out.println("=====================================");
            System.out.println("   Caregiver available on this day ");
            System.out.println("=====================================");
            while(resultSet_1.next()) {
                System.out.printf("%20s\n", resultSet_1.getString("CaregiverName"));
            }
            // Display available caregiver
            PreparedStatement statement_2 = con.prepareStatement(selectVaccine);
            ResultSet resultSet_2 = statement_2.executeQuery();
            System.out.println("=====================================");
            System.out.println("  Vaccine-Type        Doses-Left");
            System.out.println("=====================================");
            while(resultSet_2.next()) {
                System.out.printf("  %10s  %15d\n",
                        resultSet_2.getString("Name"), resultSet_2.getInt("Doses"));
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when checking caregiver schedule");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) {
        // TODO: Part 2
        // reserve <date> <vaccine>
        // check 1: if user is logged in as patient
        if (currentPatient == null) {
            System.out.println("Please login as a patient first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String date  = tokens[1];
        String vaccineName = tokens[2];
        // check 3: if caregiver is available on given date
        try {
            Date d = Date.valueOf(date);
            String AvailableCaregiver = getAvailableCaregiver(d);
            if (AvailableCaregiver.equals("")) {
                System.out.println("Provided date has no caregivers available!");
            } else {
                Vaccine currentVaccine = new Vaccine.VaccineGetter(vaccineName).get();
                // check 4: if vaccine is available
                if (currentVaccine == null || currentVaccine.getAvailableDoses() == 0) {
                    System.out.printf("%s is not available\n", vaccineName);
                    return;
                }
                // decrease vaccine by 1 & check vaccine available
                currentVaccine.decreaseAvailableDoses(1);
                // schedule appointment
                Availability currentAppointment = new Availability.AvailabilityGetter(AvailableCaregiver, d).get();
                currentAppointment.bookAppointment(currentPatient.getUsername(), vaccineName);
                // Output assigned caregiver and appointment ID
                currentAppointment.printAppointmentDetail();
                System.out.println("Reservation successful!");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Reservation failed!");
            e.printStackTrace();
        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // TODO: Extra credit
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first to cancel appointments!");
            return;
        }
        // check 2: the length for tokens need to be exactly 1
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String ID = tokens[1];
        String getAppoint = "";
        String user = "";
        if (currentCaregiver != null) {
            getAppoint = "SELECT * FROM Availabilities WHERE CaregiverName = ? AND ID = ?";
            user = "Caregiver";
        } else if (currentPatient != null) {
            getAppoint = "SELECT * FROM Availabilities WHERE PatientName = ? AND ID = ?";
            user = "Patient";
        }
        try {
            // check 3: whether appointment exist
            PreparedStatement statement = con.prepareStatement(getAppoint);
            if (currentCaregiver != null) {
                statement.setString(1, currentCaregiver.getUsername());
            } else if (currentPatient != null) {
                statement.setString(1, currentPatient.getUsername());
            }
            statement.setInt(2, Integer.parseInt(ID));
            ResultSet resultSet = statement.executeQuery();
            if (!resultSet.isBeforeFirst()) {
                System.out.println("invalid Appointment ID!");
                return;
            }
            while (resultSet.next()) {
                // update Availabilities table
                Date d = resultSet.getDate("Time");
                String caregiverName = resultSet.getString("CaregiverName");
                Availability currentAppointment = new Availability.AvailabilityGetter(caregiverName, d).get();
                currentAppointment.cancelAppointment(user, Integer.parseInt(ID));
                // update Vaccines table
                String vaccineName = resultSet.getString("VaccineName");
                Vaccine currentVaccine = new Vaccine.VaccineGetter(vaccineName).get();
                currentVaccine.increaseAvailableDoses(1);
                System.out.println("Cancellation successful!");
            }
        } catch (SQLException e) {
            System.out.println("Error occurred when cancelling user appointments");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        try {
            String vaccineName = tokens[1];
            int doses = Integer.parseInt(tokens[2]);
            Vaccine vaccine = new Vaccine.VaccineGetter(vaccineName).get();
            // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
            // table
            if (vaccine == null) {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } else {
                // if the vaccine is not null, meaning that the vaccine already exists in our table
                vaccine.increaseAvailableDoses(doses);
            }
            System.out.println("Doses updated!");
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number of doses!");
            return;
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
    }

    private static void showAppointments(String[] tokens) {
        // TODO: Part 2
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first to check appointments!");
            return;
        }
        // check 2: the length for tokens need to be exactly 1
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String getAppoint = "";
        String otherUser = "";
        if (currentCaregiver != null) {
            getAppoint = "SELECT * FROM Availabilities WHERE CaregiverName = ? AND PatientName IS NOT NULL";
            otherUser = "Patient";
        } else if (currentPatient != null) {
            getAppoint = "SELECT * FROM Availabilities WHERE PatientName = ?";
            otherUser = "Caregiver";
        }
        try {
            PreparedStatement statement = con.prepareStatement(getAppoint);
            if (currentCaregiver != null) {
                statement.setString(1, currentCaregiver.getUsername());
            } else if (currentPatient != null) {
                statement.setString(1, currentPatient.getUsername());
            }
            ResultSet resultSet = statement.executeQuery();
            System.out.println("===============================================================");
            System.out.println("  Appointment-ID     Vaccine-Type        Date        " + otherUser);
            System.out.println("===============================================================");
            while(resultSet.next()) {
                System.out.printf("%10s%20s%18s%12s\n",
                        resultSet.getInt("ID") + "",
                        resultSet.getString("VaccineName"),
                        resultSet.getDate("Time") + "",
                        resultSet.getString(otherUser + "Name"));
            }
        } catch (SQLException e) {
            System.out.println("Error occurred when checking user appointments");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void logout(String[] tokens) {
        // TODO: Part 2
        // logout
        // check 1: check if there is even a user currently logged-in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("You are not logged-in yet!");
            return;
        }
        // check 2: the length for tokens need to be exactly 1
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }
        // logout
        currentCaregiver = null;
        currentPatient = null;
    }
}
