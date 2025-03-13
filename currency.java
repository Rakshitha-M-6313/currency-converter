package r1;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

public class currency {
    private static final String API_URL = "https://api.freecurrencyapi.com/v1/latest?apikey=fca_live_V9mLlJdS76SD7XcZKH9XpmQ3OhNYeEUPSRIXoKhF";
    private static final String[] currencies = {"USD", "INR", "EUR", "GBP", "JPY"};
    private static Map<String, Double> exchangeRates = new HashMap<>();
    private static boolean darkMode = false;

    public static void main(String[] args) {
        JFrame frame = new JFrame("Currency Converter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        JLabel amountLabel = new JLabel("Amount:");
        amountLabel.setBounds(20, 20, 80, 30);
        JTextField amountTextField = new JTextField("0");
        amountTextField.setBounds(100, 20, 100, 30);

        JLabel fromCurrencyLabel = new JLabel("From Currency:");
        fromCurrencyLabel.setBounds(20, 60, 100, 30);
        JComboBox<String> fromCurrencyComboBox = new JComboBox<>(currencies);
        fromCurrencyComboBox.setBounds(130, 60, 100, 30);

        JLabel toCurrencyLabel = new JLabel("To Currency:");
        toCurrencyLabel.setBounds(20, 100, 100, 30);
        JComboBox<String> toCurrencyComboBox = new JComboBox<>(currencies);
        toCurrencyComboBox.setBounds(130, 100, 100, 30);

        JButton convertButton = new JButton("Convert");
        convertButton.setBounds(240, 60, 100, 30);

        JButton resetButton = new JButton("Reset");
        resetButton.setBounds(240, 100, 100, 30);

        JButton themeButton = new JButton("Toggle Theme");
        themeButton.setBounds(240, 140, 120, 30);

        JButton trendButton = new JButton("Show Trends");
        trendButton.setBounds(240, 180, 120, 30);

        JLabel resultLabel = new JLabel("Result:");
        resultLabel.setBounds(20, 220, 60, 30);
        JLabel resultValueLabel = new JLabel("0.00");
        resultValueLabel.setBounds(80, 220, 100, 30);

        JLabel exchangeRateLabel = new JLabel("Exchange Rate:");
        exchangeRateLabel.setBounds(20, 260, 120, 30);
        JLabel exchangeRateValueLabel = new JLabel("N/A");
        exchangeRateValueLabel.setBounds(140, 260, 100, 30);

        JLabel updateLabel = new JLabel("");
        updateLabel.setBounds(20, 300, 300, 30);

        convertButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    double amount = Double.parseDouble(amountTextField.getText());
                    String fromCurrency = fromCurrencyComboBox.getSelectedItem().toString();
                    String toCurrency = toCurrencyComboBox.getSelectedItem().toString();

                    double conversionRate = getConversionRate(fromCurrency, toCurrency);
                    double result = amount * conversionRate;
                    insertConversionRecord(fromCurrency, toCurrency, amount, result);

                    DecimalFormat df = new DecimalFormat("#0.00");
                    resultValueLabel.setText(df.format(result));
                    exchangeRateValueLabel.setText(String.valueOf(conversionRate));
                } catch (NumberFormatException ex) {
                    resultValueLabel.setText("Invalid input");
                }
            }
        });

        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                amountTextField.setText("0");
                fromCurrencyComboBox.setSelectedIndex(0);
                toCurrencyComboBox.setSelectedIndex(0);
                resultValueLabel.setText("0.00");
                exchangeRateValueLabel.setText("N/A");
                updateLabel.setText("");
            }
        });

        themeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                darkMode = !darkMode;
                updateTheme(frame, darkMode);
            }
        });

        trendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showTrendGraph(null, null);
            }
        });

        frame.setLayout(null);
        frame.add(amountLabel);
        frame.add(amountTextField);
        frame.add(fromCurrencyLabel);
        frame.add(fromCurrencyComboBox);
        frame.add(toCurrencyLabel);
        frame.add(toCurrencyComboBox);
        frame.add(convertButton);
        frame.add(resetButton);
        frame.add(themeButton);
        frame.add(trendButton);
        frame.add(resultLabel);
        frame.add(resultValueLabel);
        frame.add(exchangeRateLabel);
        frame.add(exchangeRateValueLabel);
        frame.add(updateLabel);

        updateTheme(frame, darkMode);
        frame.setVisible(true);

        fetchExchangeRates(updateLabel);
    }

    private static void updateTheme(JFrame frame, boolean darkMode) {
        Color bgColor = darkMode ? Color.DARK_GRAY : Color.LIGHT_GRAY;
        Color fgColor = darkMode ? Color.WHITE : Color.BLACK;

        frame.getContentPane().setBackground(bgColor);
        for (Component component : frame.getContentPane().getComponents()) {
            component.setBackground(bgColor);
            component.setForeground(fgColor);
        }
    }

    private static double getConversionRate(String fromCurrency, String toCurrency) {
        if (exchangeRates.isEmpty()) {
            fetchExchangeRates(null);
        }
        Double fromRate = exchangeRates.get(fromCurrency);
        Double toRate = exchangeRates.get(toCurrency);

        if (fromRate == null || toRate == null) {
            return 1.0;
        }
        return toRate / fromRate;
    }

    private static void fetchExchangeRates(JLabel updateLabel) {
        new Thread(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                    JsonObject rates = json.getAsJsonObject("data");
                    for (String currency : currencies) {
                        exchangeRates.put(currency, rates.get(currency).getAsDouble());
                    }
                    if (updateLabel != null) {
                        updateLabel.setText("Exchange rates updated successfully!");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (updateLabel != null) {
                    updateLabel.setText("Failed to update exchange rates.");
                }
            }
        }).start();
    }

    private static void insertConversionRecord(String fromCurrency, String toCurrency, double amount, double result) {
        String url = "jdbc:mysql://localhost:3306/converter";
        String userName = "root";
        String password = "";

        try (Connection con = DriverManager.getConnection(url, userName, password)) {
            String sql = "INSERT INTO currency_converter (From_currency, To_currency, Amount, Result) VALUES (?, ?, ?, ?)";
            try (PreparedStatement preparedStatement = con.prepareStatement(sql)) {
                preparedStatement.setString(1, fromCurrency);
                preparedStatement.setString(2, toCurrency);
                preparedStatement.setDouble(3, amount);
                preparedStatement.setDouble(4, result);
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void showTrendGraph(String fromCurrency, String toCurrency) {
        JFrame trendFrame = new JFrame(fromCurrency + "/" + toCurrency + " Exchange Rate Trends");
        trendFrame.setSize(800, 600);

        DefaultCategoryDataset dataset = fetchHistoricalData(fromCurrency, toCurrency);
        JFreeChart chart = ChartFactory.createLineChart(
            fromCurrency + "/" + toCurrency + " Exchange Rate Trends",
            "Month",
            "Rate",
            dataset
        );
        ChartPanel chartPanel = new ChartPanel(chart);
        trendFrame.setContentPane(chartPanel);

        trendFrame.setVisible(true);
    }

    private static DefaultCategoryDataset fetchHistoricalData(String fromCurrency, String toCurrency) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        // Replace with actual logic to fetch historical data for fromCurrency/toCurrency pair
        // Example data:
        dataset.addValue(1.0, fromCurrency + "/" + toCurrency, "Jan");
        dataset.addValue(1.1, fromCurrency + "/" + toCurrency, "Feb");
        dataset.addValue(1.2, fromCurrency + "/" + toCurrency, "Mar");
        dataset.addValue(1.3, fromCurrency + "/" + toCurrency, "Apr");
        return dataset;
    }


}
