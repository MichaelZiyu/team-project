package jswing;

import javax.swing.*;

import file.CloudStorage;
import file.FileManager;
import game.GameController;
import states.PanelStateController;
import game.World;
import testpackage.Test;

import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

/**
 * initialize and store any panel used in the game
 */
public class PanelManager {

    public static final JPanel startMenuPanel = createStartMenuPanel();
    public static final JPanel settingMenuPanel = createSettingMenuPanel();
    public static final JPanel loadMenuPanel = createLoadMenuPanel();
    public static final JPanel gamePanel = createGamePanel();

    public static final JPanel savePanel = createSavePanel();


    // Serializable object -> String
    public static String convert(Serializable object)
            throws IOException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
        }

        return Base64.getEncoder().encodeToString(bos.toByteArray());
    }

    // String -> Object
    public static Object convert(String str)
            throws IOException, ClassNotFoundException {

        byte[] data = Base64.getDecoder().decode(str);

        try (ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(data))) {
            return in.readObject();
        }
    }

    private static JPanel createSavePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JButton exitButton = new JButton("Exit");

        exitButton.addActionListener(e ->
                PanelStateController.transition("Exit")
        );

        JTextField t1 = new JTextField("url");
        JTextField t2 = new JTextField("apikey");
        JTextField t3 = new JTextField("savename");

        // TEST CLOUD BUTTON
        JButton testButton = new JButton("Test Cloud");

        testButton.addActionListener(e -> {
            System.out.println("\n========== CLOUD DIAGNOSTICS ==========");

            try {
                // TEST 1: URL
                System.out.println("\n[TEST 1] URL validation");

                String url = t1.getText().trim();
                String apiKey = t2.getText().trim();

                if (url.isEmpty()) {
                    throw new Exception("URL is empty");
                }

                if (!url.endsWith("/")) {
                    url += "/";
                }

                URI uri = URI.create(url);

                if (uri.getHost() == null ||
                        (!"https".equalsIgnoreCase(uri.getScheme())
                                && !"http".equalsIgnoreCase(uri.getScheme()))) {
                    throw new Exception("Invalid URL: " + url);
                }

                System.out.println("PASS: " + url);

                // TEST 2: API KEY
                System.out.println("\n[TEST 2] API key");

                if (apiKey.isEmpty()) {
                    throw new Exception("API key is empty");
                }

                System.out.println("PASS: API key provided");

                // TEST 3: DNS
                System.out.println("\n[TEST 3] DNS resolution");

                InetAddress address =
                        InetAddress.getByName(uri.getHost());

                System.out.println(
                        "PASS: IP = " + address.getHostAddress()
                );

                // TEST 4: HTTP CONNECTION
                System.out.println("\n[TEST 4] REST endpoint");

                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url + "saves?select=*"))
                        .timeout(Duration.ofSeconds(15))
                        .header("apikey", apiKey)
                        .header("Authorization", "Bearer " + apiKey)
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

                System.out.println(
                        "HTTP STATUS: " + response.statusCode()
                );

                System.out.println(
                        "SERVER RESPONSE: " + response.body()
                );

                if (response.statusCode() >= 200 &&
                        response.statusCode() < 300) {
                    System.out.println("PASS: Endpoint accessible");
                } else {
                    System.out.println("FAIL: Server rejected request");
                }

            } catch (Exception ex) {
                System.out.println("\nTEST FAILED:");
                ex.printStackTrace();
            }

            System.out.println("\n========== TEST FINISHED ==========\n");
        });

        // SAVE BUTTON
        JButton saveButton = new JButton("Save");

        saveButton.addActionListener(e -> {
            CloudStorage.setURL(
                    "https://frufnphsinqsjabbiwxt.supabase.co/rest/v1/"
            );
            CloudStorage.setAPI_KEY(t2.getText());

            try {
                System.out.println("Starting cloud save...");

                CloudStorage.save(
                        convert(new FileManager(t3.getText()).loadSave())
                );

                System.out.println("Cloud save successful!");

                JOptionPane.showMessageDialog(
                        panel,
                        "Cloud save successful!"
                );

            } catch (Exception ex) {
                System.out.println("Cloud save failed:");
                ex.printStackTrace();

                JOptionPane.showMessageDialog(
                        panel,
                        "Cloud save failed: " + ex.getMessage(),
                        "Save Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        panel.add(exitButton);
        panel.add(t1);
        panel.add(t2);
        panel.add(t3);
        panel.add(testButton);
        panel.add(saveButton);

        return panel;
    }


    private static JPanel createStartMenuPanel() {
        return new StartMenuPanel(
                // Start Game
                () -> {
                    if (FileManager.getSaves().length == 0) {
                        PanelStateController.transition("generateMenu");
                    } else {
                        PanelStateController.transition("loadMenu");
                    }
                },

                // Settings
                () -> PanelStateController.transition("settings"),

                // Exit
                () -> PanelStateController.transition("Exit"),

                () -> PanelStateController.transition("Save")
        );
    }

    private static JPanel createSettingMenuPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JButton exitButton = new JButton("Exit");

        exitButton.addActionListener(e -> {
            PanelStateController.transition("Exit");
        });

        panel.add(exitButton);

        return panel;
    }

    private static JPanel createLoadMenuPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JButton exitButton = new JButton("Exit");
        JComboBox<String> list = new JComboBox<>(FileManager.getSaves());
        list.setMaximumSize(new Dimension(200, 30));
        list.setPreferredSize(new Dimension(200, 30));
        list.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton loadButton = new JButton("Load");

        loadButton.addActionListener(e -> { // should make the game run
            String selected = (String) list.getSelectedItem();
            FileManager file = new FileManager(selected);
            World world = file.loadSave();
            GameController.initializeGame(world);
            PanelStateController.transition("load");
        });

        exitButton.addActionListener(e -> {
            PanelStateController.transition("Exit");
        });

        panel.add(list);
        panel.add(loadButton);
        panel.add(exitButton);

        return panel;
    }

    private static JPanel createGamePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(null);
        return panel;
    }

    private static JPanel createPausePanel() {
        JPanel panel = new JPanel();

        JButton returnButton = new JButton("return the game");

        returnButton.addActionListener(e -> {
            PanelStateController.transition("return");
        });

        panel.add(returnButton);
        return panel;
    }
}
