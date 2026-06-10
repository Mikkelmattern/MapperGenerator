package app;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class Frame implements ActionListener {
    JFrame frame = new JFrame("SpringForm");
    JTextField userField, passwordField, urlField, schemaField, databaseField;

    public void mainFrame() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 UnsupportedLookAndFeelException e) {
            JOptionPane.showMessageDialog(frame, "Fejl: " + e.getMessage());
        }
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
        userField.setText(System.getenv("JDBC_USER") != null ? System.getenv("JDBC_USER"): "");
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

            // 3. Choose output-folder
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = chooser.showOpenDialog(frame);

            if (result == JFileChooser.APPROVE_OPTION) {
                Path outputDir = chooser.getSelectedFile().toPath();

                // 4. Generate files
                classGenerator generator = new classGenerator();
                generator.generate(tables, outputDir);

                // 5. Show message
                JOptionPane.showMessageDialog(frame, "Genereret " + tables.size() + " mappere i " + outputDir);

                // 6. Close Jframe
                frame.dispose();
            }
        } catch (DatabaseException | IOException ex) {
            JOptionPane.showMessageDialog(frame, "Fejl: " + ex.getMessage());
        }

    }
}