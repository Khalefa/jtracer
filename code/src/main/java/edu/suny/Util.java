package edu.suny;
import java.util.*;
import java.io.*;
import javax.json.*;
import javax.json.stream.*;


//https://stackoverflow.com/questions/23007567/java-json-pretty-print-javax-json
public class Util{
public static String prettyPrint(JsonStructure json) {
    return jsonFormat(json, JsonGenerator.PRETTY_PRINTING);
}

public static String jsonFormat(JsonStructure json, String... options) {
    StringWriter stringWriter = new StringWriter();
    Map<String, Boolean> config = buildConfig(options);
    JsonWriterFactory writerFactory = Json.createWriterFactory(config);
    JsonWriter jsonWriter = writerFactory.createWriter(stringWriter);

    jsonWriter.write(json);
    jsonWriter.close();

    return stringWriter.toString();
}

private static Map<String, Boolean> buildConfig(String... options) {
    Map<String, Boolean> config = new HashMap<String, Boolean>();

    if (options != null) {
        for (String option : options) {
            config.put(option, true);
        }
    }

    return config;
}
}