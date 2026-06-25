package app;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import com.formdev.flatlaf.FlatLightLaf;

import javafx.application.Platform;

public class MainWindow implements ActionListener {
    JFrame frame;
    JTextField userField, passwordField, urlField, schemaField, databaseField;

    public void mainFrame() {
        FlatLightLaf.setup();

        frame = new JFrame("Connect to database");
        Platform.startup(() -> {
        });
        Platform.setImplicitExit(false);

        frame.setSize(350, 250);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // margin between fields
        gbc.anchor = GridBagConstraints.WEST;

        // Add label + field pair
        userField = addRow(panel, gbc, "USER:", 0);
        passwordField = addRow(panel, gbc, "PASSWORD:", 1);
        urlField = addRow(panel, gbc, "URL:", 2);
        schemaField = addRow(panel, gbc, "SCHEMA:", 3);
        databaseField = addRow(panel, gbc, "DATABASE:", 4);


        // If user has .env use variables from there
        userField.setText(System.getenv("JDBC_USER") != null ? System.getenv("JDBC_USER") : "");
        passwordField.setText(System.getenv("JDBC_PASSWORD") != null ? System.getenv("JDBC_PASSWORD") : "");
        urlField.setText(System.getenv("JDBC_CONNECTION_STRING") != null ? System.getenv("JDBC_CONNECTION_STRING") : "");
        schemaField.setText("public");
        databaseField.setText(System.getenv("JDBC_DB") != null ? System.getenv("JDBC_DB") : "");


        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(this);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2; // Is two columns wide
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(submitButton, gbc);

        frame.add(panel);
        frame.setVisible(true);
    }

    private JTextField addRow(JPanel panel, GridBagConstraints gbc, String labelText, int row) {
        // Label in left column
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel(labelText), gbc);

        // Text in right column
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        JTextField field = new JTextField(15);
        panel.add(field, gbc);

        return field;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String user = userField.getText();
        String password = passwordField.getText();
        String database = databaseField.getText();
        String url = String.format(urlField.getText(), database);
        String schema = schemaField.getText();

        try {
            // 1. Get connection
            ConnectionPool connectionPool = ConnectionPool.getInstance(user, password, url, schema, database);

            // 2. Read schema
            SchemaReader schemaReader = new SchemaReader();
            List<TableDefinition> tables = schemaReader.readSchema(connectionPool);

            // Create custom methods
            CustomMethodDialogue customMethodDialogue = new CustomMethodDialogue(tables);
            List<CustomMethod> customMethods = customMethodDialogue.show();

            // 3. Choose output-folder
            Path outputDir = PathResolver.chooseDirectory();


            if (outputDir != null) {

                // 4. Create correct root if not found
                Path javaRoot = PathResolver.resolveJavaRoot(outputDir);

                if (!javaRoot.equals(outputDir)) {
                    JOptionPane.showMessageDialog(frame, "Files will be generated in " + javaRoot);
                }

                // 5. Generate files
                ClassGenerator generator = new ClassGenerator();
                generator.generate(tables, javaRoot, customMethods);

                // 6. Show message
                JOptionPane.showMessageDialog(frame, "Generated " + tables.size() + " mappers in " + outputDir);

                // 7. Close JFrame
                frame.dispose();
                System.exit(0);
            }
        } catch (DatabaseException | IOException ex) {
            JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage());
        }

    }
}