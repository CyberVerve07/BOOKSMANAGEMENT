import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;

 class CreativeBookStore extends JFrame {

    private JTable bookTable;
    private DefaultTableModel tableModel;

    private JTextField tfTitle, tfAuthor, tfPrice, tfQuantity;
    private JButton btnAdd, btnUpdate, btnDelete;

    public CreativeBookStore() {
        setTitle("Creative Book Store Management");
        setSize(750, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Left panel
        JPanel leftPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(160, 180, 200, 100));
                g2.fillOval(30, 30, 60, 60);
                g2.fillOval(80, 150, 90, 90);
                g2.setColor(new Color(120, 140, 170, 150));
                g2.setStroke(new BasicStroke(3f));
                g2.drawLine(20, 250, 120, 350);
                g2.drawLine(50, 300, 150, 370);
            }
        };

        leftPanel.setPreferredSize(new Dimension(180, 0));
        leftPanel.setBackground(new Color(245, 245, 250));
        add(leftPanel, BorderLayout.WEST);

        // Table
        String[] cols = {"Title", "Author", "Price", "Quantity"};
        tableModel = new DefaultTableModel(cols, 0);
        bookTable = new JTable(tableModel);
        bookTable.setRowHeight(28);
        bookTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
        bookTable.setSelectionBackground(new Color(220, 240, 255));
        JScrollPane scrollPane = new JScrollPane(bookTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(scrollPane, BorderLayout.CENTER);

        // Right panel
        JPanel rightPanel = new JPanel();
        rightPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(new Color(250, 250, 255));

        tfTitle = createInputField("Title:", rightPanel);
        tfAuthor = createInputField("Author:", rightPanel);
        tfPrice = createInputField("Price:", rightPanel);
        tfQuantity = createInputField("Quantity:", rightPanel);

        JPanel buttonsPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        buttonsPanel.setBackground(new Color(250, 250, 255));
        btnAdd = createButton("Add Book", new Color(100, 149, 237));
        btnUpdate = createButton("Update Book", new Color(72, 209, 204));
        btnDelete = createButton("Delete Book", new Color(255, 99, 71));

        buttonsPanel.add(btnAdd);
        buttonsPanel.add(btnUpdate);
        buttonsPanel.add(btnDelete);

        rightPanel.add(Box.createVerticalStrut(20));
        rightPanel.add(buttonsPanel);

        add(rightPanel, BorderLayout.EAST);

        // Listeners
        btnAdd.addActionListener(e -> addBook());
        btnUpdate.addActionListener(e -> updateBook());
        btnDelete.addActionListener(e -> deleteBook());

        bookTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && bookTable.getSelectedRow() != -1) {
                loadBookToFields();
            }
        });
    }

    private JTextField createInputField(String label, JPanel parent) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        JTextField tf = new JTextField(15);
        tf.setFont(new Font("SansSerif", Font.PLAIN, 14));
        panel.add(lbl, BorderLayout.NORTH);
        panel.add(tf, BorderLayout.CENTER);
        panel.setMaximumSize(new Dimension(250, 60));
        parent.add(panel);
        parent.add(Box.createVerticalStrut(10));
        return tf;
    }

    private JButton createButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(color.darker()); }
            public void mouseExited(MouseEvent e) { btn.setBackground(color); }
        });
        return btn;
    }

    private void addBook() {
        if (!validateInput()) return;
        tableModel.addRow(new Object[]{
                tfTitle.getText(),
                tfAuthor.getText(),
                Double.parseDouble(tfPrice.getText()),
                Integer.parseInt(tfQuantity.getText())
        });
        clearInputs();
        JOptionPane.showMessageDialog(this, "Book added successfully!");
    }

    private void updateBook() {
        int row = bookTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a book to update.");
            return;
        }
        if (!validateInput()) return;

        tableModel.setValueAt(tfTitle.getText(), row, 0);
        tableModel.setValueAt(tfAuthor.getText(), row, 1);
        tableModel.setValueAt(Double.parseDouble(tfPrice.getText()), row, 2);
        tableModel.setValueAt(Integer.parseInt(tfQuantity.getText()), row, 3);
        JOptionPane.showMessageDialog(this, "Book updated successfully!");
    }

    private void deleteBook() {
        int row = bookTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a book to delete.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this book?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            tableModel.removeRow(row);
            clearInputs();
        }
    }

    private void loadBookToFields() {
        int row = bookTable.getSelectedRow();
        tfTitle.setText(tableModel.getValueAt(row, 0).toString());
        tfAuthor.setText(tableModel.getValueAt(row, 1).toString());
        tfPrice.setText(tableModel.getValueAt(row, 2).toString());
        tfQuantity.setText(tableModel.getValueAt(row, 3).toString());
    }

    private boolean validateInput() {
        if (tfTitle.getText().trim().isEmpty() ||
                tfAuthor.getText().trim().isEmpty() ||
                tfPrice.getText().trim().isEmpty() ||
                tfQuantity.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields.");
            return false;
        }
        try {
            Double.parseDouble(tfPrice.getText());
            Integer.parseInt(tfQuantity.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Price must be a number and Quantity an integer.");
            return false;
        }
        return true;
    }

    private void clearInputs() {
        tfTitle.setText("");
        tfAuthor.setText("");
        tfPrice.setText("");
        tfQuantity.setText("");
        bookTable.clearSelection();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new CreativeBookStore().setVisible(true);
        });
    }
}
