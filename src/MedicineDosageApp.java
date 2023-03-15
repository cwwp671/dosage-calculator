import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class MedicineDosageApp extends JFrame implements ActionListener {

    private final JLabel weightLabel;
    private final JTextField weightTextField;
    private final JLabel medicineLabel;
    private final JComboBox<String> medicineComboBox;
    private final JButton calculateButton;
    private final JButton addMedicineButton;
    private final JButton editMedicineButton;
    private final JButton removeMedicineButton;
    private final JLabel resultLabel;

    private final Connection conn;

    public MedicineDosageApp() {
        super("Medicine Dosage Calculator");
        setSize(400, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize the database
        conn = initializeDatabase();

        // Initialize the GUI components
        weightLabel = new JLabel("Weight (kg):");
        weightTextField = new JTextField();
        medicineLabel = new JLabel("Medicine:");
        medicineComboBox = new JComboBox<>();
        calculateButton = new JButton("Calculate");
        resultLabel = new JLabel();
        addMedicineButton = new JButton("Add Medicine");
        editMedicineButton = new JButton("Edit Medicine");
        removeMedicineButton = new JButton("Remove Medicine");

        // Add the action listener to the calculate button
        calculateButton.addActionListener(this);
        addMedicineButton.addActionListener(this);
        editMedicineButton.addActionListener(this);
        removeMedicineButton.addActionListener(this);

        // Load the medicines from the database
        loadMedicines();

        // Set up the layout
        JPanel contentPane = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 10, 10, 10);

        c.gridx = 0;
        c.gridy = 0;
        contentPane.add(weightLabel, c);

        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        contentPane.add(weightTextField, c);

        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.0;
        c.fill = GridBagConstraints.NONE;
        contentPane.add(medicineLabel, c);

        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        contentPane.add(medicineComboBox, c);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.NONE;
        contentPane.add(calculateButton, c);

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        contentPane.add(resultLabel, c);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        buttonPanel.add(addMedicineButton);
        buttonPanel.add(editMedicineButton);
        buttonPanel.add(removeMedicineButton);

        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        contentPane.add(buttonPanel, c);

        setContentPane(contentPane);
        pack();
        setVisible(true);

    }

    /**
     * Initialize the SQLite database.
     */
    private Connection initializeDatabase() {
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:medicines.db");
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS medicines (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, dosage FLOAT)");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

    /**
     * Load the medicines from the database into the combo box.
     */
    private void loadMedicines() {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT name FROM medicines");
            while (rs.next()) {
                String name = rs.getString("name");
                medicineComboBox.addItem(name);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Calculate the medicine dosage and display it in the result label.
     */
    private void calculateDosage() {
        // Get the weight and selected medicine
        double weight = 0;
        try {
            weight = Double.parseDouble(weightTextField.getText());
        } catch (NumberFormatException e) {
            showErrorDialog("Invalid weight input!");
            return;
        }
        String medicineName = (String) medicineComboBox.getSelectedItem();

        // Retrieve the medicine dosage from the database
        double dosage = 0;
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT dosage FROM medicines WHERE name = ?");
            stmt.setString(1, medicineName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                dosage = rs.getDouble("dosage");
            } else {
                showErrorDialog("Medicine not found in database!");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showErrorDialog("Error retrieving medicine dosage from database!");
            return;
        }

        // Calculate the medicine dosage
        double medicineDosage = dosage * weight;

        // Display the result
        resultLabel.setText("Dosage: " + medicineDosage + " mg");
    }

    /**
     * Show an error dialog with the specified message.
     *
     * @param message the error message to display
     */
    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == calculateButton) {
            calculateDosage();
        }

        if (e.getSource() == addMedicineButton) {
            String name = JOptionPane.showInputDialog(this, "Enter the medicine name:");
            if (name != null && !name.isEmpty()) {
                try {
                    double dosage = Double.parseDouble(JOptionPane.showInputDialog(this, "Enter the medicine dosage (mg/kg):"));
                    PreparedStatement stmt = conn.prepareStatement("INSERT INTO medicines (name, dosage) VALUES (?, ?)");
                    stmt.setString(1, name);
                    stmt.setDouble(2, dosage);
                    stmt.executeUpdate();
                    medicineComboBox.addItem(name);
                    JOptionPane.showMessageDialog(this, "Medicine added successfully.");
                } catch (NumberFormatException | SQLException ex) {
                    ex.printStackTrace();
                    showErrorDialog("Error adding medicine to database!");
                }
            }
        }

        if (e.getSource() == editMedicineButton) {

            ComboBoxModel editMedicineModel = medicineComboBox.getModel();
            int size = editMedicineModel.getSize();
            Object[] element = new Object[size];
            for(int i=0;i<size;i++) {
                element[i] = editMedicineModel.getElementAt(i);
                System.out.println("Element at " + i + " = " + element);
            }

            String name = (String) JOptionPane.showInputDialog(this, "Select a medicine to edit:", "Edit Medicine", JOptionPane.QUESTION_MESSAGE, null, element, medicineComboBox.getSelectedItem());
            if (name != null) {
                try {
                    double dosage = Double.parseDouble(JOptionPane.showInputDialog(this, "Enter the new medicine dosage (mg/kg):"));
                    PreparedStatement stmt = conn.prepareStatement("UPDATE medicines SET dosage = ? WHERE name = ?");
                    stmt.setDouble(1, dosage);
                    stmt.setString(2, name);
                    int rowsAffected = stmt.executeUpdate();
                    if (rowsAffected > 0) {
                        JOptionPane.showMessageDialog(this, "Medicine updated successfully.");
                    } else {
                        showErrorDialog("Medicine not found in database!");
                    }
                } catch (NumberFormatException | SQLException ex) {
                    ex.printStackTrace();
                    showErrorDialog("Error updating medicine in database!");
                }
            }
        } else if (e.getSource() == removeMedicineButton) {

            ComboBoxModel removeMedicineModel = medicineComboBox.getModel();
            int size = removeMedicineModel.getSize();
            Object[] element = new Object[size];
            for(int i=0;i<size;i++) {
                element[i] = removeMedicineModel.getElementAt(i);
                System.out.println("Element at " + i + " = " + element);
            }

            String name = (String) JOptionPane.showInputDialog(this, "Select a medicine to remove:", "Remove Medicine", JOptionPane.QUESTION_MESSAGE, null, element, medicineComboBox.getSelectedItem());
            if (name != null) {
                try {
                PreparedStatement stmt = conn.prepareStatement("DELETE FROM medicines WHERE name = ?");
                stmt.setString(1, name);
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    medicineComboBox.removeItem(name);
                    JOptionPane.showMessageDialog(this, "Medicine removed successfully.");
                } else {
                    showErrorDialog("Medicine not found in database!");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                showErrorDialog("Error removing medicine from database!");
                }
             }
            }
    }

    public static void main(String[] args) {
        new MedicineDosageApp();
    }
}


