package app;

import javax.swing.*;
import java.awt.*;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;

public class CustomMethodDialogue {


    private final List<TableDefinition> tables;
    private final List<CustomMethod> customMethods = new ArrayList<>();
    JDialog dialog = new JDialog((Frame) null, "Custom metoder", true);

    public CustomMethodDialogue(List<TableDefinition> tables) {
        this.tables = tables;
    }

    public List<CustomMethod> show() {

        // Dialogue setup
        dialog.setSize(400, 450);
        dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dialog.setLocationRelativeTo(null);
        // Panel setup
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // margin between fields
        gbc.anchor = GridBagConstraints.WEST;

        // Map TableDefinition to each of their name
        String[] tablesNames = tables.stream().map(TableDefinition::getTableName).toArray(String[]::new);

        // Creates the dropdown for tables and updates the column list to match
        JComboBox<String> tableDropDown = new JComboBox<>(tablesNames);
        JComboBox<CrudType> crudDropDown = new JComboBox<>(CrudType.values());
        JComboBox<ReturnType> returnDropDown = new JComboBox<>(ReturnType.values());
        DefaultListModel<String> columnListContent = new DefaultListModel<>();
        JList<String> columnSelect = new JList<>(columnListContent);
        columnSelect.setVisibleRowCount(5);
        columnSelect.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JTextField methodTextField = new JTextField(15);
        JButton addButton = new JButton("Tilføj metode");
        DefaultListModel<String> methodOverviewContent = new DefaultListModel<>();
        JList<String> methodOverview = new JList<>(methodOverviewContent);
        methodOverview.setVisibleRowCount(5);
        JButton okButton = new JButton("Ok");

        // Action listener for when an item is selected in the table dropdown
        tableDropDown.addActionListener(event -> {
            String selectedName = (String) tableDropDown.getSelectedItem();

            for (TableDefinition table : tables) {
                if (table.getTableName().equals(selectedName)) {
                    columnListContent.clear();
                    //TODO create method to change methodname correctly based on table, crud and params
                    for (ColumnDefinition column : table.getColumns()) {
                        columnListContent.addElement(column.getColumnName());
                    }
                }
            }
        });

        crudDropDown.addActionListener(event -> {

        });

        // Finds the first table in the table list with the given name for the selected table
        TableDefinition selectedTableDef = tables.stream()
                .filter(t -> t.getTableName().equals(tableDropDown.getSelectedItem()))
                .findFirst().orElse(null);

        // CRUD dropdown
        CrudType selectedCrudType = (CrudType) crudDropDown.getSelectedItem();
        if (selectedTableDef != null && selectedCrudType != null) {
            // TODO rethink how the method name should look
            String s = selectedCrudType.getCrudPrefix() + toPascalCase(selectedTableDef.getTableName());
            methodTextField.setText(s);
        }

        // Adds the different JObjects to the rows
        addRow(panel, gbc, "Tabel:", tableDropDown, 0);
        addRow(panel, gbc, "Type:", crudDropDown, 1);
        addRow(panel, gbc, "Parametre:", new JScrollPane(columnSelect), 2);
        addRow(panel, gbc, "Returnerer:", returnDropDown, 3);
        addRow(panel, gbc, "Metodenavn:", methodTextField, 4);

        // Adds add button to the row
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(addButton, gbc);

        // Adds the method overview
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel("Tilføjede metoder:"), gbc);
        gbc.gridy = 7;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        panel.add(new JScrollPane(methodOverview), gbc);

        //Add the ok button to the row
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(okButton, gbc);


        addButton.addActionListener(l -> {
            TableDefinition table = tables.stream()
                    .filter(t -> t.getTableName().equals(tableDropDown.getSelectedItem()))
                    .findFirst().orElse(null);
            List<String> params = columnSelect.getSelectedValuesList();
            List<ColumnDefinition> columnParams = table != null ? table.getColumns().stream()
                    .filter(t -> params.contains(t.getColumnName())).toList() : null;

            CrudType crudType = (CrudType) crudDropDown.getSelectedItem();
            ReturnType returnType = (ReturnType) returnDropDown.getSelectedItem();
            String methodName = methodTextField.getText();

            methodOverviewContent.addElement(methodName);

            customMethods.add(new CustomMethod(methodName, table, columnParams, crudType, returnType));
        });

        okButton.addActionListener(action -> {
            dialog.dispose();
        });

        dialog.add(panel);
        tableDropDown.getActionListeners()[0].actionPerformed(null);
        dialog.setVisible(true);

        return customMethods;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, String labelText, JComponent component, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel(labelText), gbc);
        gbc.gridx = 1;
        panel.add(component, gbc);
    }

    private String toPascalCase(String tableName) {
        String[] results = tableName.split("_");
        StringBuilder name = new StringBuilder();
        for (String result : results) {
            String upperCased = Character.toUpperCase(result.charAt(0)) + result.substring(1);
            name.append(upperCased);
        }
        return name.toString();
    }
}
