import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by zach on 10/19/15.
 */
public class People {
    public static void main(String[] args) {
        ArrayList<Person> people = new ArrayList();

        String fileContent = readFile("people.csv");
        String[] lines = fileContent.split("\n");

        for (String line : lines) {
            if (line == lines[0])
                continue;

            String[] columns = line.split(",");
            Person person = new Person(Integer.valueOf(columns[0]), columns[1], columns[2], columns[3], columns[4], columns[5]);
            people.add(person);
        }






        // write Spark route here
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

                    if (! (counter < people.size())){
                        Spark.halt(403);
                    } else {

                        ArrayList<Person> smallList = new ArrayList(people.subList(counter, counter + 20));
                        HashMap m = new HashMap();
                        m.put("people", smallList);
                        m.put("counter", counter+ 20);
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
                    int idNum = Integer.valueOf(personID)-1;
                    Person person = people.get(idNum);

                    /**this is long and drawn out but works.... for some reason i couldn't make a temp person
                    and draw their info from the single person object
                    so instead i'm searching through the array list each time.**/

                   /* String firstName = people.get(idNum).firstName;
                    String lastName = people.get(idNum).lastName;
                    String email = people.get(idNum).email;
                    String country = people.get(idNum).country;
                    String ip = people.get(idNum).ip;
                    int id = people.get(idNum).id;*/
                     /* m.put("firstname", person.firstName);
                    m.put("lastname", person.lastName);
                    m.put("email", person.email);
                    m.put("country", people.get(idNum).country);
                    m.put("ip", people.get(idNum).ip);
                    m.put("id", people.get(idNum).id);*/



                    HashMap m = new HashMap();

                    m.put("person", person);


                    return new ModelAndView(m, "person.html");
                }),
                new MustacheTemplateEngine()
        );
    }

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
