
package file;

import testpackage.Test;

import java.io.*;
import java.util.Base64;
import java.net.URI;
import java.net.http.*;

public class CloudStorage {

    private static String URL;
    private static String API_KEY;

    public static void setURL(String url) {
        URL = url;
    }

    public static void setAPI_KEY(String apikey) {
        API_KEY = apikey;
    }

    public static void save(Serializable object) throws Exception {

        Test.print("testcall");

        // Serialize object
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (ObjectOutputStream out = new ObjectOutputStream(output)) {
            out.writeObject(object);
        }

        // Encode to Base64
        String data = Base64.getEncoder()
                .encodeToString(output.toByteArray());

        String json = "{\"save_data\":\"" + data + "\"}";

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL + "WorldSave"))
                .header("apikey", API_KEY)
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 201 &&
                response.statusCode() != 200) {
            throw new IOException(
                    "Save failed: HTTP " + response.statusCode()
                            + " - " + response.body()
            );
        }
    }
}
