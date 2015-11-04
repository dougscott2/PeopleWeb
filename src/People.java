import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.io.File;
import java.io.FileReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by zach on 10/19/15.
 */
public class People {
    static final int NEXTSTEP = 20;

    public static void createTables (Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("DROP TABLE IF EXISTS people");
         stmt.execute("CREATE TABLE IF NOT EXISTS people (id IDENTITY, first_name VARCHAR, last_name VARCHAR, email VARCHAR, " +
                "country VARCHAR, ip VARCHAR)");
    }

    public static void insertPerson(Connection conn, String firstName, String lastName, String email, String country, String ip) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO people VALUES (NULL, ?,?,?,?,?)");
        stmt.setString(1, firstName );
        stmt.setString(2, lastName);
        stmt.setString(3, email);
        stmt.setString(4, country);
        stmt.setString(5, ip);
        stmt.execute();
    }

    public static Person selectPerson(Connection conn, int id) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM people WHERE id = ?");
        stmt.setInt( 1 , id);
        Person person = new Person();
        ResultSet results = stmt.executeQuery();
        if (results.next()){
            person.firstName = results.getString("first_name");
            person.lastName = results.getString("last_name");
            person.email = results.getString("email");
            person.country = results.getString("country");
            person.ip = results.getString("ip");
            person.id = results.getInt("id");
        }
        return person;
    }
    public static Person selectPerson(Connection conn) throws SQLException {
        return selectPerson(conn, 0);
    }

    public static void populateDatabase(Connection conn) throws SQLException {
        Statement stmtDrop = conn.createStatement();
        stmtDrop.execute("DROP TABLE IF EXISTS people");
        createTables(conn);
        ArrayList<Person> people = new ArrayList();
        String fileContent = readFile("people.csv");
        String[] lines = fileContent.split("\n");

        for (String line : lines) {
            if (line == lines[0])
                continue;

            String[] columns = line.split(",");
            Person person = new Person(Integer.valueOf(columns[0]), columns[1], columns[2], columns[3], columns[4], columns[5]);
            people.add(person);
            insertPerson(conn, columns[1], columns[2], columns[3], columns[4], columns[5]);
        }
    }
    public static ArrayList<Person> selectPeople (Connection conn, int offset) throws SQLException {
        ArrayList<Person> people = new ArrayList<>();
        String query = String.format("SELECT * FROM people LIMIT ? OFFSET ?"); //don't need string format yet but prepping for future
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, NEXTSTEP);
        stmt.setInt(2, offset);
        ResultSet results = stmt.executeQuery();

        while (results.next()){
            Person person = new Person();
            person.firstName = results.getString("first_name");
            person.lastName = results.getString("last_name");
            person.email = results.getString("email");
            person.country = results.getString("country");
            person.ip = results.getString("ip");
            person.id = results.getInt("id");

            people.add(person);
        }
        return people;
    }
    public static ArrayList<Person> selectPeople (Connection conn) throws SQLException {
        return selectPeople(conn, 0);
    }
    public static int countPeople (Connection conn) throws SQLException {
         Statement stmt = conn.createStatement();
        ResultSet results = stmt.executeQuery("SELECT COUNT (*) AS counter FROM people");
        int count =0;
        if (results.next()){
            count = results.getInt("counter");
        }
        return count;
    }






    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static void main(String[] args) throws SQLException {

        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        createTables(conn);
        populateDatabase(conn);


       /* ArrayList<Person> people = new ArrayList();

        String fileContent = readFile("people.csv");
        String[] lines = fileContent.split("\n");

        for (String line : lines) {
            if (line == lines[0])
                continue;

            String[] columns = line.split(",");
            Person person = new Person(Integer.valueOf(columns[0]), columns[1], columns[2], columns[3], columns[4], columns[5]);
            people.add(person);
        }*/

         Spark.get(
                "/",
                ((request, response) -> {
                    String offset = request.queryParams("offset");

                  int counter;
                    if (offset == null){
                        counter = 0;
                    } else {
                        counter = Integer.valueOf(offset);
                    }
                    if (!(counter < countPeople(conn))){
                        Spark.halt(403);
                    } else {

                        HashMap m = new HashMap();

                        m.put("counter", counter + NEXTSTEP);
                        m.put("othercounter", counter - NEXTSTEP);
                        m.put("people", selectPeople(conn, counter));
                        m.put("person", selectPeople(conn));


                        boolean showPrevious = counter > 0;
                        m.put("showPrevious", showPrevious);
                        boolean showNext = counter + NEXTSTEP < countPeople(conn);
                        m.put("showNext", showNext);
                        return new ModelAndView(m, "people.html");
                    }
                    return new ModelAndView(new HashMap(), "people.html");
                }),
                new MustacheTemplateEngine()
        );
        Spark.get(
                "/person",
                ((request, response) -> {
                    String personID = request.queryParams("id");
                    HashMap m = new HashMap();
                    try {
                        int idNum = Integer.valueOf(personID);
                        Person person =  selectPerson(conn, idNum);
                        m.put("person", person);
                    } catch (Exception e) {

                    }
                    return new ModelAndView(m, "person.html");
                }),
                new MustacheTemplateEngine()
        );
    }
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    static String readFile(String fileName) {
        File f = new File(fileName);
        try {
            FileReader fr = new FileReader(f);
            int fileSize = (int) f.length();
            char[] fileContent = new char[fileSize];
            fr.read(fileContent);
            return new String(fileContent);
        } catch (Exception e) {
            return null;
        }
    }
}
