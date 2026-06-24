package app;

import javax.swing.*;
import java.awt.*;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CustomMethodDialogue {


    private final List<TableDefinition> tables;
    private final List<CustomMethod> customMethods = new ArrayList<>();
    JDialog dialog;

    private CrudType selectedCrudType = null;
    private List<String> selectedParams = null;
    private TableDefinition currentTable = null;

    public CustomMethodDialogue(List<TableDefinition> tables) {
        this.tables = tables;
    }

    public List<CustomMethod> show() {

        dialog = new JDialog((Frame) null, "Custom Methods", true);

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
        DefaultListModel<String> columnListContent = new DefaultListModel<>();
        JList<String> columnSelect = new JList<>(columnListContent);
        columnSelect.setVisibleRowCount(5);
        columnSelect.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JTextField methodTextField = new JTextField(20);
        JButton addButton = new JButton("+ Add Method");
        addButton.setEnabled(false);
        DefaultListModel<String> methodOverviewContent = new DefaultListModel<>();
        JList<String> methodOverview = new JList<>(methodOverviewContent);
        methodOverview.setVisibleRowCount(5);
        JButton removeButton = new JButton("Remove");
        removeButton.setForeground(Color.RED);
        removeButton.setEnabled(false);
        JButton okButton = new JButton("Ok");

        // Action listener for when an item is selected in the table dropdown
        tableDropDown.addActionListener(event -> {
            String selectedName = (String) tableDropDown.getSelectedItem();

            for (TableDefinition table : tables) {
                if (table.getTableName().equals(selectedName)) {
                    columnListContent.clear();
                    currentTable = table;
                    for (ColumnDefinition column : table.getColumns()) {
                        columnListContent.addElement(column.getColumnName());
                    }
                }
            }
            updateMethodName(methodTextField);
        });

        // Updates the method name based on crud type
        crudDropDown.addActionListener(event -> {
            selectedCrudType = (CrudType) crudDropDown.getSelectedItem();
            updateMethodName(methodTextField);
        });

        // Sets the chosen params as a list of strings, and updates the method name accordingly
        columnSelect.addListSelectionListener(e -> {
            selectedParams = columnSelect.getSelectedValuesList();
            addButton.setEnabled(!columnSelect.isSelectionEmpty());

            if (!e.getValueIsAdjusting()) {
                updateMethodName(methodTextField);
            }
        });

        // Enables remove button if an element is selected
        methodOverview.addListSelectionListener(event -> {
            removeButton.setEnabled(!(event.getValueIsAdjusting() || methodOverview.isSelectionEmpty()));
        });

        // Adds function to remove button
        removeButton.addActionListener(event -> {
            int selectedIndex = methodOverview.getSelectedIndex();
            if (selectedIndex == -1) return;

            // Finds index of correct method and
            methodOverviewContent.remove(selectedIndex);
            customMethods.remove(selectedIndex);
            removeButton.setEnabled(false);
        });

        // Adds an asterisk to unique columns
        columnSelect.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            ColumnDefinition col = currentTable.getColumns().stream().filter(c -> c.getColumnName()
                    .equals(value)).findFirst().orElseThrow();

            JLabel label = new JLabel(col.isUnique() ? value + " *" : value);
            label.setOpaque(true);

            if (isSelected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            } else {
                label.setBackground(list.getBackground());
                label.setForeground(list.getForeground());
            }
            return label;
        });

        // Adds function to the add button
        addButton.addActionListener(l -> {

            // Finds the table with the name chosen in the table dropdown list
            TableDefinition table = tables.stream()
                    .filter(t -> t.getTableName().equals(tableDropDown.getSelectedItem()))
                    .findFirst().orElse(null);

            if (table == null) return;

            // Finds the columns with the names chosen in the column selection list
            List<String> params = columnSelect.getSelectedValuesList();
            List<ColumnDefinition> columnParams = table.getColumns().stream()
                    .filter(t -> params.contains(t.getColumnName())).toList();

            // Finds the crud type
            CrudType crudType = (CrudType) crudDropDown.getSelectedItem();

            // Hopefully redundant null check
            if (crudType == null) return;

            // Figures out the return type based on whether crud type and whether a column is unique
            ReturnType returnType = switch (crudType) {
                case READ ->
                        columnParams.stream().anyMatch(ColumnDefinition::isUnique) ? ReturnType.OBJECT : ReturnType.LIST;
                case UPDATE, DELETE -> ReturnType.VOID;
            };

            String methodName = methodTextField.getText();

            // Adds the method name to the overview window
            methodOverviewContent.addElement(methodName);

            customMethods.add(new CustomMethod(methodName, table, columnParams, crudType, returnType));
        });


        okButton.addActionListener(action -> {
            dialog.dispose();
        });

        // Finds the first table in the table list with the given name for the selected table
        TableDefinition selectedTableDef = tables.stream()
                .filter(t -> t.getTableName().equals(tableDropDown.getSelectedItem()))
                .findFirst().orElse(null);


        // Adds the different JObjects to the rows
        addRow(panel, gbc, "Table:", tableDropDown, 0);
        addRow(panel, gbc, "Type:", crudDropDown, 1);
        addRow(panel, gbc, "Parameters:", new JScrollPane(columnSelect), 2);
        addRow(panel, gbc, "Method name:", methodTextField, 3);

        // Adds add button to the row
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(addButton, gbc);

        // Adds the method overview
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel("Added methods:"), gbc);
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        panel.add(new JScrollPane(methodOverview), gbc);

        // Add the remove button to the row
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(removeButton, gbc);


        //Add the ok button to the row
        gbc.gridy = 7;
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(okButton, gbc);

        dialog.add(panel);
        tableDropDown.getActionListeners()[0].actionPerformed(null);
        crudDropDown.getActionListeners()[0].actionPerformed(null);
        dialog.setVisible(true);
        updateMethodName(methodTextField);
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

    private void updateMethodName(JTextField methodField) {
        if (currentTable == null || selectedCrudType == null) return;
        CrudType crudType = selectedCrudType;
        List<String> params = selectedParams;
        TableDefinition table = currentTable;
        String tableName = table.getTableName();

        List<ColumnDefinition> columnParams = selectedParams != null
                ? currentTable.getColumns().stream()
                .filter(c -> selectedParams.contains(c.getColumnName()))
                .toList()
                : List.of();

        ReturnType returnType = switch (selectedCrudType) {
            case READ -> columnParams.stream().anyMatch(c -> c.isPrimaryKey() || c.isUnique())
                    ? ReturnType.OBJECT : ReturnType.LIST;
            case UPDATE, DELETE -> ReturnType.VOID;
        };

        String prefix = switch (crudType) {
            case READ -> returnType == ReturnType.LIST ? "getAll" : "get";
            case UPDATE -> "update";
            case DELETE -> "delete";
            case null, default -> "";
        };

        if (tableName != null) {
            prefix += toPascalCase(tableName);
        }

        String suffix = (params != null && !params.isEmpty())
                ? params.stream().map(this::toPascalCase)
                .collect(Collectors.joining("And")) : "";


        methodField.setText(suffix.isEmpty()
                ? prefix
                : String.format("%sBy%s", prefix, suffix));
    }
}
