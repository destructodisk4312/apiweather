import com.google.gson.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Main extends JFrame {

    // OpenWeatherMap API key (your latest one)
    private static final String API_KEY = "d405d29696e36cbbf8db613a547b9eca";

    private final JTextField cityField = new JTextField("Sonora,US", 22);
    private final JComboBox<String> unitsBox = new JComboBox<>(new String[]{"metric", "imperial"});
    private final JButton fetchBtn = new JButton("Fetch Weather");
    private final JButton saveBtn = new JButton("Save Report");
    private final JButton exitBtn = new JButton("Exit");
    private final JTextArea output = new JTextArea(16, 60);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Main app = new Main();
            app.setVisible(true);
        });
    }

    public Main() {
        super("Smart Weather GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(12, 12));

        JPanel top = new JPanel(new GridBagLayout());
        top.setBorder(new EmptyBorder(12, 12, 0, 12));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx = 0; gc.gridy = 0;
        top.add(new JLabel("City (e.g., Sonora,US):"), gc);
        gc.gridx = 1; gc.gridy = 0; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1;
        top.add(cityField, gc);

        gc.gridx = 0; gc.gridy = 1; gc.fill = GridBagConstraints.NONE; gc.weightx = 0;
        top.add(new JLabel("Units:"), gc);
        gc.gridx = 1; gc.gridy = 1;
        top.add(unitsBox, gc);

        gc.gridx = 0; gc.gridy = 2; gc.gridwidth = 2;
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(fetchBtn);
        buttons.add(saveBtn);
        buttons.add(exitBtn);
        top.add(buttons, gc);

        add(top, BorderLayout.NORTH);

        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        add(new JScrollPane(output), BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        statusPanel.add(new JLabel("Tip: try \"Twain Harte,US\" or \"San Francisco,US\""));
        add(statusPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);

        fetchBtn.addActionListener(this::onFetchWeather);
        saveBtn.addActionListener(e -> onSaveReport());
        exitBtn.addActionListener(e -> System.exit(0));
    }

    private void onFetchWeather(ActionEvent e) {
        String city = cityField.getText().trim();
        if (city.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a city (e.g., Sonora,US).",
                    "Missing City", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String units = (String) unitsBox.getSelectedItem();

        setBusy(true);
        output.setText("Fetching weather for " + city + " (" + units + ")...\n");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                WeatherResult wr = fetchWeather(city, units);
                return buildReport(wr, units);
            }
            @Override
            protected void done() {
                try {
                    output.setText(get());
                } catch (Exception ex) {
                    output.setText("Error:\n" + ex.getMessage());
                    JOptionPane.showMessageDialog(Main.this,
                            "Failed to fetch weather.\n\n" + ex,
                            "API Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void onSaveReport() {
        String text = output.getText().trim();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nothing to save yet. Fetch weather first.",
                    "No Data", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            String ts = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.now());
            Path out = Path.of("weather_summary_" + ts + ".txt");
            try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(out))) {
                pw.println(text);
            }
            JOptionPane.showMessageDialog(this, "Saved:\n" + out.toAbsolutePath(),
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to save:\n" + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setBusy(boolean busy) {
        fetchBtn.setEnabled(!busy);
        saveBtn.setEnabled(!busy);
        cityField.setEnabled(!busy);
        unitsBox.setEnabled(!busy);
        setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }

    // ---- API call + formatting ----
    private static WeatherResult fetchWeather(String city, String units)
            throws IOException, InterruptedException {

        String url = String.format(
                "https://api.openweathermap.org/data/2.5/weather?q=%s&units=%s&appid=%s",
                uriEncode(city), units, API_KEY);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

        if (res.statusCode() != 200) {
            throw new IOException("HTTP " + res.statusCode() + " -> " + res.body());
        }

        JsonObject root = JsonParser.parseString(res.body()).getAsJsonObject();
        JsonObject main = root.getAsJsonObject("main");
        JsonObject wind = root.getAsJsonObject("wind");
        JsonArray weatherArr = root.getAsJsonArray("weather");

        String description = (weatherArr != null && weatherArr.size() > 0)
                ? weatherArr.get(0).getAsJsonObject().get("description").getAsString()
                : "n/a";

        double temp = main.get("temp").getAsDouble();
        double feels = main.get("feels_like").getAsDouble();
        int humidity = main.get("humidity").getAsInt();
        double windSpeed = wind.has("speed") ? wind.get("speed").getAsDouble() : 0.0;

        String name = root.has("name") ? root.get("name").getAsString() : city;
        long dt = root.has("dt") ? root.get("dt").getAsLong() : System.currentTimeMillis() / 1000;

        return new WeatherResult(name, description, temp, feels, humidity, windSpeed, dt);
    }

    private static String buildReport(WeatherResult r, String units) {
        double delta = Math.round((r.feels - r.temp) * 10) / 10.0;
        String windLabel = (r.wind < 1) ? "calm"
                : (r.wind < 5) ? "light breeze"
                : (r.wind < 10) ? "gentle breeze"
                : (r.wind < 20) ? "windy"
                : "very windy";
        boolean likelyWet = r.description.toLowerCase().contains("rain")
                || r.description.toLowerCase().contains("drizzle")
                || r.description.toLowerCase().contains("thunder");
        String umbrellaHint = likelyWet ? "☔ Consider an umbrella." : "No rain hinted by description.";
        String tempUnit = "metric".equals(units) ? "°C" : "°F";
        String windUnit = "m/s"; // OWM default

        String when = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochSecond(r.epochSeconds));

        return """
               === Smart Weather Brief ===
               Location: %s
               When:     %s (local)
               Units:    %s

               Conditions: %s
               Temp:       %.1f %s (feels like %.1f %s, Δ=%.1f)
               Humidity:   %d%%
               Wind:       %.1f %s (%s)

               Advice: %s
               """.formatted(
                r.city, when, units,
                titleCase(r.description),
                r.temp, tempUnit, r.feels, tempUnit, delta,
                r.humidity,
                r.wind, windUnit, windLabel,
                umbrellaHint
        );
    }

    private static String uriEncode(String s) { return s.replace(" ", "%20"); }
    private static String titleCase(String s) {
        String[] parts = s.split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            out.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }
        return out.toString().trim();
    }

    private record WeatherResult(
            String city, String description, double temp, double feels,
            int humidity, double wind, long epochSeconds) {}
}