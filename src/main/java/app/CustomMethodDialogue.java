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
    JDialog dialog = new JDialog((Frame) null, "Custom metoder", true);

    private CrudType selectedCrudType = null;
    private List<String> selectedParams = null;
    private TableDefinition currentTable = null;

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
        DefaultListModel<String> columnListContent = new DefaultListModel<>();
        JList<String> columnSelect = new JList<>(columnListContent);
        columnSelect.setVisibleRowCount(5);
        columnSelect.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JTextField methodTextField = new JTextField(20);
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
                    currentTable = table;
                    for (ColumnDefinition column : table.getColumns()) {
                        columnListContent.addElement(column.getColumnName());
                    }
                }
            }
            updateMethodName(methodTextField);
        });

        crudDropDown.addActionListener(event -> {
            selectedCrudType = (CrudType) crudDropDown.getSelectedItem();
            updateMethodName(methodTextField);
        });

        columnSelect.addListSelectionListener(e -> {
            selectedParams = columnSelect.getSelectedValuesList();
            if (!e.getValueIsAdjusting()) {
                updateMethodName(methodTextField);
            }
        });

        // Finds the first table in the table list with the given name for the selected table
        TableDefinition selectedTableDef = tables.stream()
                .filter(t -> t.getTableName().equals(tableDropDown.getSelectedItem()))
                .findFirst().orElse(null);


        // Adds the different JObjects to the rows
        addRow(panel, gbc, "Tabel:", tableDropDown, 0);
        addRow(panel, gbc, "Type:", crudDropDown, 1);
        addRow(panel, gbc, "Parametre:", new JScrollPane(columnSelect), 2);
        addRow(panel, gbc, "Metodenavn:", methodTextField, 3);

        // Adds add button to the row
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(addButton, gbc);

        // Adds the method overview
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel("Tilføjede metoder:"), gbc);
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        panel.add(new JScrollPane(methodOverview), gbc);

        //Add the ok button to the row
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(okButton, gbc);


        addButton.addActionListener(l -> {
            TableDefinition table = tables.stream()
                    .filter(t -> t.getTableName().equals(tableDropDown.getSelectedItem()))
                    .findFirst().orElse(null);
            if (table == null) return;
            List<String> params = columnSelect.getSelectedValuesList();
            List<ColumnDefinition> columnParams = table.getColumns().stream()
                    .filter(t -> params.contains(t.getColumnName())).toList();

            CrudType crudType = (CrudType) crudDropDown.getSelectedItem();
            if (crudType == null) return;
            ReturnType returnType = switch (crudType) {
                case READ ->
                        columnParams.stream().anyMatch(ColumnDefinition::isUnique) ? ReturnType.OBJECT : ReturnType.LIST;
                case UPDATE, DELETE -> ReturnType.VOID;
            };
            String methodName = methodTextField.getText();

            methodOverviewContent.addElement(methodName);

            customMethods.add(new CustomMethod(methodName, table, columnParams, crudType, returnType));
        });

        okButton.addActionListener(action -> {
            dialog.dispose();
        });

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
