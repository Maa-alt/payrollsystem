package com.example.demo1;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;
import java.time.LocalDate;

public class SalaryController {
    // Constants for salary calculation
    private static final double STANDARD_HOURS_PER_MONTH = 160;
    private static final double OVERTIME_RATE_MULTIPLIER = 1.5;
    private static final double INSURANCE_RATE = 0.05;
    private static final double PENSION_RATE = 0.07;

    // FXML Components
    @FXML private TextField txtEmployeeId;
    @FXML private TextField txtBasicSalary;
    @FXML private TextField txtWorkingHours;
    @FXML private TextField txtOvertimeHours;
    @FXML private TextField txtOtherDeductions; // Add this field for other deductions
    @FXML private Label lblHourlyRate;
    @FXML private Label lblRegularPay;
    @FXML private Label lblOvertimePay;
    @FXML private Label lblGrossSalary;
    @FXML private Label lblTax;
    @FXML private Label lblInsurance;
    @FXML private Label lblPension;
    @FXML private Label lblTotalDeductions;
    @FXML private Label lblNetSalary;
    @FXML private ComboBox<String> comboMonth;
    @FXML private Button btnCalculate;

    @FXML private TableView<SalaryRecord> salaryTable;
    @FXML private TableColumn<SalaryRecord, String> colEmployeeId;
    @FXML private TableColumn<SalaryRecord, String> colMonth;
    @FXML private TableColumn<SalaryRecord, Double> colBasic;
    @FXML private TableColumn<SalaryRecord, Double> colWorkingHours;
    @FXML private TableColumn<SalaryRecord, Double> colOvertime;
    @FXML private TableColumn<SalaryRecord, Double> colGross;
    @FXML private TableColumn<SalaryRecord, Double> colTax;
    @FXML private TableColumn<SalaryRecord, Double> colInsurance;
    @FXML private TableColumn<SalaryRecord, Double> colPension;
    @FXML private TableColumn<SalaryRecord, Double> colTotalDeductions;
    @FXML private TableColumn<SalaryRecord, Double> colNet;

    private ObservableList<SalaryRecord> salaryRecords = FXCollections.observableArrayList();
    private String userRole; // Store the user role

    @FXML
    public void initialize() {
        setupMonthComboBox();
        setupTableColumns();
        loadSalaryData();
    }

    @FXML
    private void handleSave() {
        try {
            // Validate inputs before saving
            if (!validateInputs()) return;

            // Create a SalaryRecord object with current inputs
            SalaryRecord record = createSalaryRecordFromInputs();

            // Save the SalaryRecord to the database
            saveSalaryRecord(record);

            // Reload the salary data to reflect the new record
            loadSalaryData();

            // Show success message
            showAlert("Success", "Salary record saved successfully!", Alert.AlertType.INFORMATION);

        } catch (Exception e) {
            showAlert("Save Error", "Failed to save record: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private SalaryRecord createSalaryRecordFromInputs() {
        String employeeId = txtEmployeeId.getText().trim();
        String month = comboMonth.getValue();
        double basicSalary = Double.parseDouble(txtBasicSalary.getText());
        double workingHours = Double.parseDouble(txtWorkingHours.getText());
        double overtimeHours = Double.parseDouble(txtOvertimeHours.getText());
        double otherDeductions = txtOtherDeductions.getText().isEmpty() ? 0 :
                Double.parseDouble(txtOtherDeductions.getText());

        CalculationResult result = calculateSalary(basicSalary, workingHours, overtimeHours);

        return new SalaryRecord(
                employeeId,
                "", // Employee name can be handled as needed
                month,
                basicSalary,
                workingHours,
                overtimeHours,
                result.grossSalary,
                result.tax,
                result.insurance,
                result.pension,
                otherDeductions,
                result.totalDeductions,
                result.netSalary
        );
    }

    private void saveSalaryRecord(SalaryRecord record) throws SQLException {
        String sql = "INSERT INTO salaries (employee_id, employee_name, month, basic_salary, working_hours, " +
                "overtime_hours, gross_salary, tax, insurance, pension, other_deductions, " +
                "total_deductions, netsalary) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, record.getEmployeeId());
            pstmt.setString(2, record.getEmployeeName()); // Ensure this is set correctly
            pstmt.setString(3, record.getMonth());
            pstmt.setDouble(4, record.getBasicSalary());
            pstmt.setDouble(5, record.getWorkingHours());
            pstmt.setDouble(6, record.getOvertimeHours());
            pstmt.setDouble(7, record.getGrossSalary());
            pstmt.setDouble(8, record.getTax());
            pstmt.setDouble(9, record.getInsurance());
            pstmt.setDouble(10, record.getPension());
            pstmt.setDouble(11, record.getOtherDeductions());
            pstmt.setDouble(12, record.getTotalDeductions());
            pstmt.setDouble(13, record.getNetSalary());

            pstmt.executeUpdate();
        }
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
        applyRolePermissions();
    }

    private void applyRolePermissions() {
        boolean isAdmin = "Admin".equalsIgnoreCase(userRole);
        // Apply role permissions logic (e.g., enable/disable fields)
        txtEmployeeId.setDisable(!isAdmin);
        txtBasicSalary.setDisable(!isAdmin);
        txtWorkingHours.setDisable(!isAdmin);
        txtOvertimeHours.setDisable(!isAdmin);
        txtOtherDeductions.setDisable(!isAdmin);
        btnCalculate.setDisable(!isAdmin);
    }

    private void setupMonthComboBox() {
        comboMonth.setItems(FXCollections.observableArrayList(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        ));
        comboMonth.getSelectionModel().select(LocalDate.now().getMonthValue() - 1);
    }

    private void setupTableColumns() {
        colEmployeeId.setCellValueFactory(cellData -> cellData.getValue().employeeIdProperty());
        colMonth.setCellValueFactory(cellData -> cellData.getValue().monthProperty());
        colBasic.setCellValueFactory(cellData -> cellData.getValue().basicSalaryProperty().asObject());
        colWorkingHours.setCellValueFactory(cellData -> cellData.getValue().workingHoursProperty().asObject());
        colOvertime.setCellValueFactory(cellData -> cellData.getValue().overtimeHoursProperty().asObject());
        colGross.setCellValueFactory(cellData -> cellData.getValue().grossSalaryProperty().asObject());
        colTax.setCellValueFactory(cellData -> cellData.getValue().taxProperty().asObject());
        colInsurance.setCellValueFactory(cellData -> cellData.getValue().insuranceProperty().asObject());
        colPension.setCellValueFactory(cellData -> cellData.getValue().pensionProperty().asObject());
        colTotalDeductions.setCellValueFactory(cellData -> cellData.getValue().totalDeductionsProperty().asObject());
        colNet.setCellValueFactory(cellData -> cellData.getValue().netSalaryProperty().asObject());
    }

    @FXML
    private void handleCalculate() {
        try {
            if (!validateInputs()) return;

            double basicSalary = Double.parseDouble(txtBasicSalary.getText());
            double workingHours = Double.parseDouble(txtWorkingHours.getText());
            double overtimeHours = Double.parseDouble(txtOvertimeHours.getText());

            CalculationResult result = calculateSalary(basicSalary, workingHours, overtimeHours);
            updateCalculationLabels(result);

        } catch (Exception e) {
            showAlert("Calculation Error", "Error in calculation: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private CalculationResult calculateSalary(double basicSalary, double workingHours, double overtimeHours) {
        double hourlyRate = basicSalary / STANDARD_HOURS_PER_MONTH;
        double regularPay = Math.min(workingHours, STANDARD_HOURS_PER_MONTH) * hourlyRate;
        double overtimePay = overtimeHours * hourlyRate * OVERTIME_RATE_MULTIPLIER;
        double grossSalary = regularPay + overtimePay;
        double tax = calculateTax(grossSalary);
        double insurance = grossSalary * INSURANCE_RATE;
        double pension = grossSalary * PENSION_RATE;
        double totalDeductions = tax + insurance + pension;
        double netSalary = grossSalary - totalDeductions;

        return new CalculationResult(
                hourlyRate, regularPay, overtimePay, grossSalary,
                tax, insurance, pension, totalDeductions, netSalary
        );
    }

    private void updateCalculationLabels(CalculationResult result) {
        lblHourlyRate.setText(String.format("M%.2f/hour", result.hourlyRate));
        lblRegularPay.setText(String.format("M%.2f", result.regularPay));
        lblOvertimePay.setText(String.format("M%.2f", result.overtimePay));
        lblGrossSalary.setText(String.format("M%.2f", result.grossSalary));
        lblTax.setText(String.format("M%.2f", result.tax));
        lblInsurance.setText(String.format("M%.2f", result.insurance));
        lblPension.setText(String.format("M%.2f", result.pension));
        lblTotalDeductions.setText(String.format("M%.2f", result.totalDeductions));
        lblNetSalary.setText(String.format("M%.2f", result.netSalary));
    }

    private double calculateTax(double grossSalary) {
        // Progressive tax calculation
        if (grossSalary <= 5000) return grossSalary * 0.10;
        if (grossSalary <= 10000) return 500 + (grossSalary - 5000) * 0.15;
        if (grossSalary <= 20000) return 1250 + (grossSalary - 10000) * 0.20;
        if (grossSalary <= 50000) return 3250 + (grossSalary - 20000) * 0.25;
        return 10750 + (grossSalary - 50000) * 0.30;
    }

    private boolean validateInputs() {
        try {
            if (txtEmployeeId.getText().trim().isEmpty()) {
                throw new IllegalArgumentException("Employee ID is required");
            }
            if (comboMonth.getValue() == null) {
                throw new IllegalArgumentException("Please select a month");
            }

            validateNumericField(txtBasicSalary, "Basic salary", true);
            validateNumericField(txtWorkingHours, "Working hours", false);
            validateNumericField(txtOvertimeHours, "Overtime hours", false);

            return true;
        } catch (IllegalArgumentException e) {
            showAlert("Invalid Input", e.getMessage(), Alert.AlertType.ERROR);
            return false;
        }
    }

    private void validateNumericField(TextField field, String fieldName, boolean mustBePositive) {
        try {
            double value = Double.parseDouble(field.getText());
            if (mustBePositive && value <= 0) {
                throw new IllegalArgumentException(fieldName + " must be positive");
            }
            if (value < 0) {
                throw new IllegalArgumentException(fieldName + " cannot be negative");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + fieldName + " value");
        }
    }

    private void loadSalaryData() {
        salaryRecords.clear();
        String query = "SELECT * FROM salaries ORDER BY month, employee_id";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                SalaryRecord record = new SalaryRecord(
                        rs.getString("employee_id"),
                        rs.getString("employee_name"),
                        rs.getString("month"),
                        rs.getDouble("basic_salary"),
                        rs.getDouble("working_hours"),
                        rs.getDouble("overtime_hours"),
                        rs.getDouble("gross_salary"),
                        rs.getDouble("tax"),
                        rs.getDouble("insurance"),
                        rs.getDouble("pension"),
                        rs.getDouble("other_deductions"),
                        rs.getDouble("total_deductions"),
                        rs.getDouble("netsalary")
                );
                salaryRecords.add(record);
            }

            salaryTable.setItems(salaryRecords);

        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load salary data: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static class CalculationResult {
        final double hourlyRate;
        final double regularPay;
        final double overtimePay;
        final double grossSalary;
        final double tax;
        final double insurance;
        final double pension;
        final double totalDeductions;
        final double netSalary;

        CalculationResult(double hourlyRate, double regularPay, double overtimePay,
                          double grossSalary, double tax, double insurance,
                          double pension, double totalDeductions, double netSalary) {
            this.hourlyRate = hourlyRate;
            this.regularPay = regularPay;
            this.overtimePay = overtimePay;
            this.grossSalary = grossSalary;
            this.tax = tax;
            this.insurance = insurance;
            this.pension = pension;
            this.totalDeductions = totalDeductions;
            this.netSalary = netSalary;
        }
    }
}