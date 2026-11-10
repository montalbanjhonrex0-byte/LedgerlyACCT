import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.text.DecimalFormat;

public class LedgerlyAccounting extends JFrame {
    static final Color NAVY = new Color(23, 63, 60);
    static final Color GREEN = new Color(23, 102, 91);
    static final Color CREAM = new Color(247, 244, 233);
    static final Color INK = new Color(32, 59, 58);
    static final Color MUTED = new Color(107, 127, 126);
    static final Color LINE = new Color(215, 224, 216);
    static final Color GOLD = new Color(201, 163, 78);

    final LinkedHashMap<String, CurrencyInfo> countries = new LinkedHashMap<>();
    final File dataDir = new File(System.getProperty("user.home"), "LedgerlyData");
    final File usersFile = new File(dataDir, "users.dat");
    Map<String, User> users = new LinkedHashMap<>();
    User current;
    TransactionBook activeBook;
    String activeTab = "Dashboard";
    final DecimalFormat fmt = new DecimalFormat("#,##0.00");

    // ===== START MERGED CODE (NEW ADDITION): fields for the Journal back-end and the Columnar worksheet =====
    final JournalService journalService = new JournalService();   // Journal back-end (Tasks 2, 3, 4)
    JournalApiServer journalApi;                                  // Journal back-end routes (Task 1)
    int journalApiPort = -1;
    String journalSyncedFor = "";                                 // which user's saved entries are already loaded
    final Map<String, LedgerlyColumnar> columnarSheets = new HashMap<>();
    // ===== END MERGED CODE (NEW ADDITION) =====

    static class CurrencyInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        String code, symbol, name;
        CurrencyInfo(String code, String symbol, String name) {
            this.code = code; this.symbol = symbol; this.name = name;
        }
    }
    static class User implements Serializable {
        private static final long serialVersionUID = 1L;
        String name, email, hash, country, code, symbol, currencyName;
        User(String n, String e, String h, String c, CurrencyInfo ci) {
            name=n; email=e; hash=h; country=c; code=ci.code; symbol=ci.symbol; currencyName=ci.name;
        }
    }
    static class Entry implements Serializable {
        private static final long serialVersionUID = 1L;
        String date, account, description, type, entrySide;
        String entryId;
        double amount;
        Entry(String d, String a, String desc, String t, String side, double amt) {
            date=d; account=a; description=desc; type=t; entrySide=side; amount=amt;
        }
    }
    static class TransactionBook implements Serializable {
        private static final long serialVersionUID = 1L;
        String id, name, createdAt, updatedAt;
        ArrayList<Entry> entries = new ArrayList<>();
        TransactionBook(String name) {
            this.id = UUID.randomUUID().toString();
            this.name = name;
            this.createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"));
            this.updatedAt = createdAt;
        }
        void touch() {
            updatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"));
        }
    }

    public LedgerlyAccounting() {
        super("Ledgerly | Accounting Workspace");
        countries.put("Philippines", new CurrencyInfo("PHP", "₱", "Philippine Peso"));
        countries.put("United States", new CurrencyInfo("USD", "$", "US Dollar"));
        countries.put("Canada", new CurrencyInfo("CAD", "$", "Canadian Dollar"));
        countries.put("United Kingdom", new CurrencyInfo("GBP", "£", "British Pound"));
        countries.put("Australia", new CurrencyInfo("AUD", "$", "Australian Dollar"));
        countries.put("Singapore", new CurrencyInfo("SGD", "$", "Singapore Dollar"));
        countries.put("India", new CurrencyInfo("INR", "₹", "Indian Rupee"));
        countries.put("Japan", new CurrencyInfo("JPY", "¥", "Japanese Yen"));
        countries.put("Indonesia", new CurrencyInfo("IDR", "Rp", "Indonesian Rupiah"));
        countries.put("Malaysia", new CurrencyInfo("MYR", "RM", "Malaysian Ringgit"));
        countries.put("United Arab Emirates", new CurrencyInfo("AED", "د.إ", "UAE Dirham"));
        if (!dataDir.exists()) dataDir.mkdirs();
        loadUsers();
        // ===== START MERGED CODE (NEW ADDITION) =====
        startJournalBackend();
        // ===== END MERGED CODE (NEW ADDITION) =====
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1180, 760);
        setMinimumSize(new Dimension(920, 620));
        setLocationRelativeTo(null);
        showLogin();
    }

    JPanel whitePanel(int pad) {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(pad, pad, pad, pad));
        return p;
    }
    JLabel label(String text, int size, boolean bold, Color fg) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(size >= 18 ? "Georgia" : "Arial", bold ? Font.BOLD : Font.PLAIN, size));
        l.setForeground(fg);
        return l;
    }
    JButton button(String text, Runnable action, boolean primary) {
        JButton b = new JButton(text) {
            private boolean hover;
            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent e) { hover = true; repaint(); }
                    public void mouseExited(java.awt.event.MouseEvent e) { hover = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = primary ? (hover ? new Color(17, 83, 74) : GREEN)
                                     : (hover ? new Color(239, 246, 241) : Color.WHITE);
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 13, 13);
                if (!primary) {
                    g2.setColor(LINE);
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 13, 13);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Arial", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setForeground(primary ? Color.WHITE : GREEN);
        b.setBorder(new EmptyBorder(10, 16, 10, 16));
        b.addActionListener(e -> action.run());
        return b;
    }
    JTextField field() {
        JTextField f = new JTextField();
        f.setFont(new Font("Arial", Font.PLAIN, 13));
        f.setPreferredSize(new Dimension(250, 38));
        f.setBorder(new CompoundBorder(new LineBorder(LINE), new EmptyBorder(8, 9, 8, 9)));
        return f;
    }
    JPasswordField passwordField() {
        JPasswordField f = new JPasswordField();
        f.setFont(new Font("Arial", Font.PLAIN, 13));
        f.setPreferredSize(new Dimension(250, 38));
        f.setBorder(new CompoundBorder(new LineBorder(LINE), new EmptyBorder(8, 9, 8, 9)));
        return f;
    }
    JPanel formField(String title, JComponent input) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(Color.WHITE);
        p.add(label(title, 12, true, INK), BorderLayout.NORTH);
        p.add(input, BorderLayout.CENTER);
        p.setBorder(new EmptyBorder(6, 0, 5, 0));
        return p;
    }
    void screen(Component c) {
        JPanel transition = new JPanel(new BorderLayout()) {
            float alpha = 0f;
            {
                setOpaque(false);
                javax.swing.Timer timer = new javax.swing.Timer(16, null);
                timer.addActionListener(e -> {
                    alpha = Math.min(1f, alpha + 0.12f);
                    repaint();
                    if (alpha >= 1f) timer.stop();
                });
                timer.start();
            }
            @Override protected void paintChildren(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
                super.paintChildren(g2);
                g2.dispose();
            }
        };
        transition.add(c, BorderLayout.CENTER);
        setContentPane(transition);
        revalidate();
        repaint();
    }
    String hash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : bytes) out.append(String.format("%02x", b));
            return out.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    void error(String s) {
        JOptionPane.showMessageDialog(this, s, "Please check", JOptionPane.ERROR_MESSAGE);
    }
    @SuppressWarnings("unchecked")
    void loadUsers() {
        if (!usersFile.exists()) return;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(usersFile))) {
            Object obj = in.readObject();
            if (obj instanceof Map) users = (Map<String, User>) obj;
        } catch (Exception ex) {
            users = new LinkedHashMap<>();
        }
    }
    void saveUsers() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(usersFile))) {
            out.writeObject(users);
        } catch (Exception ex) { error("Unable to save account information: " + ex.getMessage()); }
    }
    File userFolder() {
        File f = new File(dataDir, hash(current.email).substring(0, 16));
        if (!f.exists()) f.mkdirs();
        return f;
    }
    File bookFile(String id) { return new File(userFolder(), id + ".dat"); }
    void saveBook(TransactionBook book) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(bookFile(book.id)))) {
            out.writeObject(book);
        } catch (Exception ex) { error("Unable to save transaction: " + ex.getMessage()); }
    }
    TransactionBook readBook(File f) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
            return (TransactionBook) in.readObject();
        } catch (Exception ex) { return null; }
    }
    java.util.List<TransactionBook> getBooks() {
        ArrayList<TransactionBook> result = new ArrayList<>();
        File[] files = userFolder().listFiles((dir, name) -> name.endsWith(".dat"));
        if (files != null) {
            for (File f : files) {
                TransactionBook b = readBook(f);
                if (b != null) result.add(b);
            }
        }
        result.sort((a,b) -> b.updatedAt.compareTo(a.updatedAt));
        return result;
    }

    void showLogin() {
        JPanel outer = new JPanel(new GridLayout(1, 2));
        outer.setBackground(CREAM);

        JPanel brand = new JPanel(new GridBagLayout());
        brand.setBackground(NAVY);
        brand.setBorder(new EmptyBorder(36, 44, 36, 44));

        JPanel brandContent = new JPanel();
        brandContent.setOpaque(false);
        brandContent.setLayout(new BoxLayout(brandContent, BoxLayout.Y_AXIS));

        JLabel logo = label("◆  Ledgerly", 36, true, Color.WHITE);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel tagline = label("Professional Accounting Workspace", 15, false, new Color(210, 230, 220));
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel desc = label("Manage journals, ledgers, and financial statements effortlessly with precision and clarity.", 13, false, new Color(185, 210, 198));
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);

        brandContent.add(logo);
        brandContent.add(Box.createVerticalStrut(10));
        brandContent.add(tagline);
        brandContent.add(Box.createVerticalStrut(24));
        brandContent.add(desc);

        GridBagConstraints brandGbc = new GridBagConstraints();
        brandGbc.gridx = 0; brandGbc.gridy = 0; brandGbc.weightx = 1; brandGbc.weighty = 1;
        brandGbc.anchor = GridBagConstraints.WEST;
        brand.add(brandContent, brandGbc);

        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(CREAM);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(new LineBorder(new Color(220, 226, 218), 1), new EmptyBorder(30, 36, 30, 36)));
        card.setPreferredSize(new Dimension(440, 430));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;

        JLabel welcome = label("Welcome Back", 26, true, INK);
        welcome.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridy = 0; c.insets = new Insets(0, 0, 6, 0);
        card.add(welcome, c);

        JLabel subtitle = label("Log in to access your financial workbooks.", 12, false, MUTED);
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridy = 1; c.insets = new Insets(0, 0, 24, 0);
        card.add(subtitle, c);

        JTextField email = field();
        JPasswordField pass = passwordField();

        c.gridy = 2; c.insets = new Insets(0, 0, 4, 0);
        card.add(label("Email address", 11, true, INK), c);
        c.gridy = 3; c.insets = new Insets(0, 0, 14, 0);
        card.add(email, c);

        c.gridy = 4; c.insets = new Insets(0, 0, 4, 0);
        card.add(label("Password", 11, true, INK), c);

        JPanel passwordRow = new JPanel(new BorderLayout(8, 0));
        passwordRow.setBackground(Color.WHITE);
        passwordRow.add(pass, BorderLayout.CENTER);

        JToggleButton showPassword = new JToggleButton("Show");
        showPassword.setFont(new Font("Arial", Font.BOLD, 11));
        showPassword.setForeground(GREEN);
        showPassword.setBackground(Color.WHITE);
        showPassword.setFocusPainted(false);
        showPassword.setCursor(new Cursor(Cursor.HAND_CURSOR));
        showPassword.setBorder(new CompoundBorder(new LineBorder(LINE), new EmptyBorder(8, 11, 8, 11)));
        final char defaultEcho = pass.getEchoChar();
        showPassword.addActionListener(e -> {
            pass.setEchoChar(showPassword.isSelected() ? (char) 0 : defaultEcho);
            showPassword.setText(showPassword.isSelected() ? "Hide" : "Show");
        });
        passwordRow.add(showPassword, BorderLayout.EAST);

        c.gridy = 5; c.insets = new Insets(0, 0, 20, 0);
        card.add(passwordRow, c);

        Runnable doLogin = () -> {
            String em = email.getText().trim().toLowerCase(Locale.ROOT);
            User u = users.get(em);
            if (u != null && u.hash.equals(hash(new String(pass.getPassword())))) {
                current = u;
                activeBook = null;
                activeTab = "Dashboard";
                showMain();
            } else {
                error("Email or password is incorrect. Sign up first if needed.");
            }
        };

        JButton login = button("Log in", doLogin, true);
        login.setPreferredSize(new Dimension(330, 42));
        c.gridy = 6; c.insets = new Insets(0, 0, 12, 0);
        card.add(login, c);

        JPanel createRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        createRow.setBackground(Color.WHITE);
        createRow.add(label("New to Ledgerly?", 11, false, MUTED));
        JButton create = button("Create account", this::showSignup, false);
        create.setFont(new Font("Arial", Font.BOLD, 11));
        createRow.add(create);
        c.gridy = 7; c.insets = new Insets(0, 0, 0, 0);
        card.add(createRow, c);

        GridBagConstraints rightGbc = new GridBagConstraints();
        rightGbc.gridx = 0; rightGbc.gridy = 0; rightGbc.weightx = 1; rightGbc.weighty = 1;
        rightGbc.anchor = GridBagConstraints.CENTER;
        right.add(card, rightGbc);

        outer.add(brand);
        outer.add(right);
        screen(outer);

        email.addActionListener(e -> pass.requestFocusInWindow());
        pass.addActionListener(e -> doLogin.run());
        SwingUtilities.invokeLater(() -> email.requestFocusInWindow());
    }

    void showSignup() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(CREAM);

        JPanel card = whitePanel(24);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new CompoundBorder(new LineBorder(LINE), new EmptyBorder(24, 32, 24, 32)));
        card.setPreferredSize(new Dimension(480, 580));

        JLabel title = label("Create Your Account", 22, true, INK);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(4));

        JLabel subtitle = label("Set up your profile and preferred currency.", 12, false, MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(14));

        JTextField name = field(), email = field();
        JPasswordField pass = passwordField(), confirm = passwordField();

        card.add(formField("Full name", name));
        card.add(formField("Email address", email));
        card.add(formField("Password (minimum 8 characters)", pass));
        card.add(formField("Confirm password", confirm));

        JComboBox<String> country = new JComboBox<>(countries.keySet().toArray(new String[0]));
        country.setSelectedItem("Philippines");
        country.setBackground(Color.WHITE);
        country.setPreferredSize(new Dimension(250, 38));
        card.add(formField("Country", country));

        JLabel hint = label("", 11, true, GREEN);
        Runnable updateHint = () -> {
            CurrencyInfo ci = countries.get((String)country.getSelectedItem());
            hint.setText("Default currency: " + ci.code + " (" + ci.symbol + ") — " + ci.name);
        };
        country.addActionListener(e -> updateHint.run());
        updateHint.run();
        card.add(hint);
        card.add(Box.createVerticalStrut(14));

        JButton signup = button("Create account", () -> {
            String n=name.getText().trim(), em=email.getText().trim().toLowerCase(Locale.ROOT);
            String pw=new String(pass.getPassword()), cpw=new String(confirm.getPassword());
            String c=(String)country.getSelectedItem();
            if (n.isEmpty()) { error("Enter your full name."); return; }
            if (!em.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) { error("Enter a valid email address."); return; }
            if (pw.length()<8) { error("Password must contain at least 8 characters."); return; }
            if (!pw.equals(cpw)) { error("Passwords do not match."); return; }
            if (users.containsKey(em)) { error("This email is already registered."); return; }
            users.put(em, new User(n, em, hash(pw), c, countries.get(c)));
            saveUsers();
            JOptionPane.showMessageDialog(this, "Account created successfully!");
            showLogin();
        }, true);
        signup.setAlignmentX(Component.CENTER_ALIGNMENT);
        signup.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        card.add(signup);
        card.add(Box.createVerticalStrut(8));

        JButton backBtn = button("Back to log in", this::showLogin, false);
        backBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        backBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        card.add(backBtn);

        outer.add(card);
        screen(outer);
    }

    JPanel card() {
        JPanel p = new JPanel(new BorderLayout(0, 14));
        p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(new LineBorder(new Color(229, 235, 228), 1), new EmptyBorder(18, 18, 18, 18)));
        return p;
    }
    JTable table(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setRowHeight(34);
        t.setFont(new Font("Arial", Font.PLAIN, 12));
        t.setForeground(INK);
        t.setBackground(Color.WHITE);
        t.setSelectionBackground(new Color(225, 240, 232));
        t.setSelectionForeground(INK);
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(new Color(237, 241, 235));
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(true);
        t.getTableHeader().setFont(new Font("Arial", Font.BOLD, 11));
        t.getTableHeader().setForeground(MUTED);
        t.getTableHeader().setBackground(new Color(245, 248, 243));
        t.getTableHeader().setPreferredSize(new Dimension(0, 38));
        t.getTableHeader().setBorder(new MatteBorder(0, 0, 1, 0, LINE));
        return t;
    }
    DefaultTableModel model(String... cols) {
        return new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
    }
    void showMain() {
        if (current == null) { showLogin(); return; }
        // ===== START MERGED CODE (NEW ADDITION) =====
        syncJournalBackend();                                                        // load this user's saved entries once
        // ===== END MERGED CODE (NEW ADDITION) =====

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(CREAM);

        JPanel sidebar = new JPanel();
        sidebar.setBackground(NAVY);
        sidebar.setPreferredSize(new Dimension(226, 0));
        sidebar.setBorder(new EmptyBorder(24, 16, 18, 16));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        JLabel brand = label("◆  Ledgerly", 24, true, Color.WHITE);
        brand.setBorder(new EmptyBorder(2, 8, 4, 4));
        sidebar.add(brand);
        JLabel brandSub = label("ACCOUNTING WORKSPACE", 9, true, new Color(178, 205, 192));
        brandSub.setBorder(new EmptyBorder(4, 9, 30, 0));
        sidebar.add(brandSub);

        sidebar.add(sidebarLabel("WORKSPACE"));
        sidebar.add(Box.createVerticalStrut(8));
        for (String tab : new String[]{"Dashboard", "Journal", "Ledger", "12-Column Worksheet"}) {
            sidebar.add(sidebarButton(tab, activeTab.equals(tab), () -> {
                activeTab = tab;
                showMain();
            }));
            sidebar.add(Box.createVerticalStrut(7));
        }

        sidebar.add(Box.createVerticalGlue());

        sidebar.add(sidebarDivider());
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(sidebarUtilityButton("Settings", () -> JOptionPane.showMessageDialog(this, "Settings panel is available under your account profile.")));
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(sidebarUtilityButton("What's New", () -> JOptionPane.showMessageDialog(this, "Ledgerly v2.1 — Enhanced accounting workspace and 12-column columnar sheets.")));
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(sidebarUtilityButton("Help & Support", () -> JOptionPane.showMessageDialog(this, "For assistance, contact support or review journal entry guidelines.")));
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(sidebarUtilityButton("Log out", () -> {
            current = null;
            activeBook = null;
            activeTab = "Dashboard";
            showLogin();
        }));

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(CREAM);

        JPanel topbar = new JPanel(new BorderLayout());
        topbar.setBackground(Color.WHITE);
        topbar.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, LINE), new EmptyBorder(13, 25, 13, 25)));
        
        JPanel pageHeading = new JPanel();
        pageHeading.setOpaque(false);
        pageHeading.setLayout(new BoxLayout(pageHeading, BoxLayout.Y_AXIS));
        pageHeading.add(label("Good day, " + current.name.split(" ")[0], 12, true, MUTED));
        pageHeading.add(Box.createVerticalStrut(3));
        pageHeading.add(label(activeTab, 21, true, INK));
        topbar.add(pageHeading, BorderLayout.WEST);

        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 6));
        topActions.setOpaque(false);
        
        JTextField searchField = new JTextField("Search workbooks...");
        searchField.setFont(new Font("Arial", Font.PLAIN, 12));
        searchField.setPreferredSize(new Dimension(200, 32));
        searchField.setForeground(MUTED);
        searchField.setBorder(new CompoundBorder(new LineBorder(LINE), new EmptyBorder(4, 8, 4, 8)));
        topActions.add(searchField);

        topActions.add(label(current.code + "  " + current.symbol, 11, true, MUTED));
        topActions.add(button("Account  ▾", this::accountMenu, false));
        topbar.add(topActions, BorderLayout.EAST);
        content.add(topbar, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(CREAM);
        body.setBorder(new EmptyBorder(22, 24, 24, 24));

        if (activeTab.equals("Dashboard")) body.add(dashboard(), BorderLayout.CENTER);
        else if (activeTab.equals("Journal")) body.add(journal(), BorderLayout.CENTER);
        else if (activeTab.equals("Ledger")) body.add(ledger(), BorderLayout.CENTER);
        else body.add(columnar12(), BorderLayout.CENTER);

        content.add(body, BorderLayout.CENTER);
        root.add(sidebar, BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        screen(root);
    }

    JLabel sidebarLabel(String text) {
        JLabel l = label(text, 9, true, new Color(154, 184, 169));
        l.setBorder(new EmptyBorder(0, 9, 0, 0));
        return l;
    }

    Component sidebarDivider() {
        JSeparator s = new JSeparator();
        s.setForeground(new Color(36, 88, 83));
        s.setBackground(new Color(36, 88, 83));
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return s;
    }

    JButton sidebarButton(String text, boolean selected, Runnable action) {
        JButton b = new JButton(text);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFont(new Font("Arial", Font.BOLD, 12));
        b.setForeground(selected ? Color.WHITE : new Color(202, 221, 211));
        b.setBackground(selected ? GREEN : NAVY);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(12, 13, 12, 10));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!selected) b.setBackground(new Color(31, 78, 70));
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (!selected) b.setBackground(NAVY);
            }
        });
        b.addActionListener(e -> action.run());
        return b;
    }

    JButton sidebarUtilityButton(String text, Runnable action) {
        JButton b = new JButton(text);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFont(new Font("Arial", Font.PLAIN, 11));
        b.setForeground(new Color(185, 208, 196));
        b.setBackground(NAVY);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(6, 10, 6, 10));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { b.setForeground(Color.WHITE); }
            public void mouseExited(java.awt.event.MouseEvent e) { b.setForeground(new Color(185, 208, 196)); }
        });
        b.addActionListener(e -> action.run());
        return b;
    }

    void accountMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem c = new JMenuItem("Country: " + current.country); c.setEnabled(false); menu.add(c);
        JMenuItem cur = new JMenuItem("Currency: " + current.code + " (" + current.symbol + ")"); cur.setEnabled(false); menu.add(cur);
        menu.show(getContentPane(), Math.max(10,getWidth()-220), 50);
    }

    void exportActiveBookToCSV() {
        if (activeBook == null) {
            error("Please open or select a workbook to export.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Workbook to CSV");
        fileChooser.setSelectedFile(new File(activeBook.name.replaceAll("\\s+", "_") + "_export.csv"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                pw.println("Date,Account Title,Type,Side,Amount,Description");
                for (Entry e : activeBook.entries) {
                    pw.printf("\"%s\",\"%s\",\"%s\",\"%s\",%.2f,\"%s\"\n",
                            e.date,
                            e.account,
                            e.type,
                            e.entrySide,
                            e.amount,
                            e.description != null ? e.description : ""
                    );
                }
                JOptionPane.showMessageDialog(this, "Workbook successfully exported to:\n" + file.getAbsolutePath());
            } catch (Exception ex) {
                error("Failed to export records: " + ex.getMessage());
            }
        }
    }

    JPanel dashboard() {
        JPanel wrap = new JPanel(new BorderLayout(0, 16));
        wrap.setBackground(CREAM);

        java.util.List<TransactionBook> books = getBooks();

        JPanel actionHeader = new JPanel(new BorderLayout());
        actionHeader.setBackground(Color.WHITE);
        actionHeader.setBorder(new CompoundBorder(new LineBorder(new Color(229, 235, 228)), new EmptyBorder(16, 20, 16, 20)));
        
        JLabel dashTitle = label("Dashboard Workbooks", 16, true, INK);
        actionHeader.add(dashTitle, BorderLayout.WEST);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActions.setOpaque(false);
        
        JButton exportReportsBtn = button("Export Reports ›", this::exportActiveBookToCSV, false);
        JButton addNewBtn = button("+ Create Workbook", this::newTransaction, true);

        rightActions.add(exportReportsBtn);
        rightActions.add(addNewBtn);
        actionHeader.add(rightActions, BorderLayout.EAST);
        wrap.add(actionHeader, BorderLayout.NORTH);

        JPanel tableCard = card();
        JPanel tableHead = new JPanel(new BorderLayout());
        tableHead.setBackground(Color.WHITE);
        tableHead.add(label("All Workbooks & Financial Logs", 14, true, INK), BorderLayout.WEST);
        tableHead.add(button("Refresh", this::showMain, false), BorderLayout.EAST);
        tableCard.add(tableHead, BorderLayout.NORTH);

        DefaultTableModel m = model("WORKBOOK NAME", "CREATED AT", "LAST UPDATED", "ENTRIES", "STATUS");
        for (TransactionBook b : books) {
            m.addRow(new Object[]{b.name, b.createdAt, b.updatedAt, b.entries.size(), "Active"});
        }
        if (books.isEmpty()) m.addRow(new Object[]{"No workbooks created yet", "—", "—", "0", "Get Started"});

        JTable t = table(m);
        
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        t.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = t.rowAtPoint(e.getPoint());
                if (row >= 0 && row < books.size() && e.getClickCount() == 2) openBook(books.get(row));
            }
        });
        tableCard.add(new JScrollPane(t), BorderLayout.CENTER);

        // NEW: pressing the Delete key on a selected row also deletes it
        t.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent e) {
                int row = t.getSelectedRow();
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DELETE && row >= 0 && row < books.size())
                    deleteBook(books.get(row));
            }
        });

        // ===== NEW: bottom bar with hint text (left) and Delete button (right) =====
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(Color.WHITE);
        bottomBar.setBorder(new EmptyBorder(8, 0, 0, 0));
        bottomBar.add(label("Double-click any workbook row to open and edit journal entries.", 11, false, MUTED), BorderLayout.WEST);

        JButton deleteBtn = dangerButton("Delete Selected Workbook", () -> {
            int row = t.getSelectedRow();
            if (books.isEmpty() || row < 0 || row >= books.size()) {
                error("Please click a workbook row first, then press Delete.");
                return;
            }
            deleteBook(books.get(row));
        });
        bottomBar.add(deleteBtn, BorderLayout.EAST);
        tableCard.add(bottomBar, BorderLayout.SOUTH);
        // ===== END NEW =====

        wrap.add(tableCard, BorderLayout.CENTER);
        return wrap;
    }

    // ===== NEW: red filled button (easy to see) =====
    JButton dangerButton(String text, Runnable action) {
        final Color RED = new Color(190, 40, 40), RED_DARK = new Color(150, 28, 28);
        JButton b = new JButton(text) {
            private boolean hover;
            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent e) { hover = true; repaint(); }
                    public void mouseExited(java.awt.event.MouseEvent e) { hover = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hover ? RED_DARK : RED);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 13, 13);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Arial", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setForeground(Color.WHITE);
        b.setBorder(new EmptyBorder(10, 16, 10, 16));
        b.addActionListener(e -> action.run());
        return b;
    }

    // ===== NEW: delete one workbook (asks first, then removes the file) =====
    void deleteBook(TransactionBook book) {
        int ok = JOptionPane.showConfirmDialog(this,
                "Delete \"" + book.name + "\"?\nThis cannot be undone.",
                "Delete Workbook", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        File f = bookFile(book.id);
        if (f.exists() && !f.delete()) {
            error("Could not delete this workbook.");
            return;
        }
        if (activeBook != null && activeBook.id.equals(book.id)) activeBook = null;
        activeTab = "Dashboard";   // go back to the list after deleting
        showMain();
    }

    // ===== END NEW =====

    void newTransaction() {
        JTextField name=field();
        name.setText("Transaction " + (getBooks().size()+1));
        JPanel form=new JPanel(new BorderLayout(0,8));
        form.add(label("Transaction / workbook name",12,true,INK),BorderLayout.NORTH);
        form.add(name,BorderLayout.CENTER);
        if(JOptionPane.showConfirmDialog(this,form,"New Transaction",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        String n=name.getText().trim();
        if(n.isEmpty()){error("Enter a transaction name.");return;}
        activeBook=new TransactionBook(n);
        saveBook(activeBook);
        activeTab="Journal";
        showMain();
    }
    void openBook(TransactionBook book) {
        activeBook=readBook(bookFile(book.id));
        if(activeBook==null){error("Could not open this transaction.");return;}
        activeTab="Journal";
        showMain();
    }

    String niceDate(String d) {
        try { return LocalDate.parse(d).format(DateTimeFormatter.ofPattern("MMM d, yyyy")); }
        catch (Exception ex) { return d; }
    }

    /** Groups the saved lines into journal entries (one debit line + one credit line, sharing a date and a description). */
    java.util.List<java.util.List<Entry>> journalGroups(TransactionBook book) {
        java.util.List<java.util.List<Entry>> groups = new ArrayList<>();
        if (book == null) return groups;
        java.util.List<Entry> list = book.entries;
        int i = 0;
        while (i < list.size()) {
            Entry first = list.get(i);
            int j = i + 1;
            if (first.entryId != null) {
                while (j < list.size() && first.entryId.equals(list.get(j).entryId)) j++;
            } else if (j < list.size()) {
                // older saved entries have no ID: pair a debit and a credit that sit together with the same date and description
                Entry next = list.get(j);
                if (next.entryId == null
                        && Objects.equals(first.date, next.date)
                        && Objects.equals(first.description, next.description)
                        && !first.entrySide.equalsIgnoreCase(next.entrySide)) j++;
            }
            groups.add(new ArrayList<>(list.subList(i, j)));
            i = j;
        }
        return groups;
    }

    JPanel journal() {
        JPanel p=card();
        JPanel top=new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        String bookName=activeBook==null?"No transaction selected":activeBook.name;
        top.add(label("Workbook: "+bookName,12,true,INK),BorderLayout.WEST);

        final java.util.List<java.util.List<Entry>> groups=journalGroups(activeBook);
        final ArrayList<Integer> rowGroup=new ArrayList<>();   // table row -> index in groups (-1 = no entry)

        JPanel journalButtons=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));
        journalButtons.setOpaque(false);
        top.add(journalButtons,BorderLayout.EAST);
        p.add(top,BorderLayout.NORTH);

        DefaultTableModel m=model("DATE","PARTICULARS","DEBIT","CREDIT");

        for(int g=0;g<groups.size();g++) {
            java.util.List<Entry> group=groups.get(g);
            ArrayList<Entry> debits=new ArrayList<>(), credits=new ArrayList<>();
            for(Entry e:group) {
                if(e.entrySide.equalsIgnoreCase("Debit")) debits.add(e); else credits.add(e);
            }
            // ONE date for the whole entry (on its first line only)
            boolean dateShown=false;
            for(Entry e:debits) {
                m.addRow(new Object[]{dateShown?"":niceDate(e.date), e.account, current.symbol+fmt.format(e.amount), ""});
                rowGroup.add(g); dateShown=true;
            }
            for(Entry e:credits) {
                m.addRow(new Object[]{dateShown?"":niceDate(e.date), "      "+e.account, "", current.symbol+fmt.format(e.amount)});
                rowGroup.add(g); dateShown=true;
            }
            // ONE description for the whole entry, below the credit line
            String desc=group.get(0).description;
            if(desc!=null && !desc.isEmpty()) {
                m.addRow(new Object[]{"", "# "+desc, "", ""});
                rowGroup.add(g);
            }
        }
        if(m.getRowCount()==0) { m.addRow(new Object[]{"","No entries yet","",""}); rowGroup.add(-1); }

        JTable t=table(m);
        t.setRowSorter(null);   // keep the rows of an entry together (sorting would scramble them)
        t.getColumnModel().getColumn(0).setMaxWidth(130);
        t.getColumnModel().getColumn(2).setMaxWidth(160);
        t.getColumnModel().getColumn(3).setMaxWidth(160);

        t.getColumnModel().getColumn(1).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table,Object value,boolean sel,boolean focus,int row,int col) {
                JLabel l=(JLabel)super.getTableCellRendererComponent(table,value,sel,focus,row,col);
                boolean note=value!=null && value.toString().trim().startsWith("#");
                l.setFont(new Font("Arial", note?Font.ITALIC:Font.PLAIN, 12));
                l.setForeground(note?MUTED:INK);
                return l;
            }
        });

        // clicking any line of an entry highlights the whole entry
        final boolean[] busy={false};
        t.getSelectionModel().addListSelectionListener(ev -> {
            if(busy[0] || ev.getValueIsAdjusting()) return;
            int row=t.getSelectedRow();
            if(row<0 || row>=rowGroup.size() || rowGroup.get(row)<0) return;
            int g=rowGroup.get(row), from=row, to=row;
            while(from>0 && rowGroup.get(from-1)==g) from--;
            while(to<rowGroup.size()-1 && rowGroup.get(to+1)==g) to++;
            busy[0]=true;
            t.setRowSelectionInterval(from,to);
            busy[0]=false;
        });

        // delete ONE journal entry (the one that is selected)
        Runnable deleteSelected=() -> {
            int row=t.getSelectedRow();
            if(activeBook==null || row<0 || row>=rowGroup.size() || rowGroup.get(row)<0) {
                error("Click an entry in the journal first, then press Delete Entry.");
                return;
            }
            deleteJournalEntry(groups.get(rowGroup.get(row)));
        };
        journalButtons.add(dangerButton("Delete Entry",deleteSelected));
        journalButtons.add(button("+ Add Entry",this::addEntry,true));
        t.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent e) {
                if(e.getKeyCode()==java.awt.event.KeyEvent.VK_DELETE) deleteSelected.run();
            }
        });

        p.add(new JScrollPane(t),BorderLayout.CENTER);

        JPanel hintBar=new JPanel(new BorderLayout());
        hintBar.setBackground(Color.WHITE);
        hintBar.setBorder(new EmptyBorder(8,0,0,0));
        hintBar.add(label("Click an entry, then press Delete Entry to remove it.",11,false,MUTED),BorderLayout.WEST);
        p.add(hintBar,BorderLayout.SOUTH);
        return p;
    }

    /** Deletes one journal entry (all of its lines) from the open workbook. */
    void deleteJournalEntry(java.util.List<Entry> group) {
        if(activeBook==null || group==null || group.isEmpty()) return;
        StringBuilder names=new StringBuilder();
        for(Entry e:group) { if(names.length()>0) names.append(" / "); names.append(e.account); }
        int ok=JOptionPane.showConfirmDialog(this,
                "Delete this journal entry?\n"+niceDate(group.get(0).date)+"  -  "+names+"\nThis cannot be undone.",
                "Delete Entry",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
        if(ok!=JOptionPane.YES_OPTION) return;
        String id=group.get(0).entryId;
        activeBook.entries.removeIf(e -> group.contains(e));
        // ===== START MERGED CODE (NEW ADDITION): keep the Journal back-end in step =====
        if(id!=null) journalService.delete(current.email,id);
        // ===== END MERGED CODE (NEW ADDITION) =====
        activeBook.touch(); saveBook(activeBook); showMain();
    }

    /** A small non-clickable tag (DEBIT / CREDIT) shown under an account title. */
    JLabel sideTag(String text) {
        JLabel l=new JLabel(text,SwingConstants.CENTER);
        l.setFont(new Font("Arial",Font.BOLD,13));
        l.setForeground(GREEN);
        l.setOpaque(true);
        l.setBackground(CREAM);
        l.setBorder(BorderFactory.createLineBorder(GREEN,2));
        l.setPreferredSize(new Dimension(100,38));
        return l;
    }

    /** Two form fields side by side (left = Debit side, right = Credit side). */
    JPanel pairRow(JComponent left,JComponent right) {
        JPanel row=new JPanel(new GridLayout(1,2,16,0));
        row.setBackground(Color.WHITE);
        row.add(left); row.add(right);
        return row;
    }

    void addEntry() {
        if(activeBook==null){newTransaction();if(activeBook==null)return;}
        String[] types={"Assets","Liabilities","Equity","Revenue","Expenses"};

        JTextField date=field(); date.setText(LocalDate.now().toString());
        JTextField explain=field();
        JTextField accOne=field(), accTwo=field();
        JTextField amountField=field(); amountField.setText("0.00");
        JComboBox<String> typeOne=new JComboBox<>(types);
        JComboBox<String> typeTwo=new JComboBox<>(types);

        // Layout: Date / Description, then the two account titles side by side
        // (left = DEBIT, right = CREDIT) with the DEBIT / CREDIT tag and account type under each title.
        JPanel form=new JPanel();
        form.setLayout(new BoxLayout(form,BoxLayout.Y_AXIS));
        form.setBackground(Color.WHITE);
        JPanel tags=pairRow(sideTag("DEBIT"),sideTag("CREDIT"));
        tags.setBorder(new EmptyBorder(2,0,8,0));
        JComponent[] rows={
            formField("Date (YYYY-MM-DD)",date),
            formField("Description",explain),
            pairRow(formField("Account Title",accOne),formField("Account Title",accTwo)),
            tags,
            pairRow(formField("Account Type",typeOne),formField("Account Type",typeTwo)),
            formField("Amount ("+current.code+"):",amountField)
        };
        for(JComponent r:rows) {
            r.setAlignmentX(Component.LEFT_ALIGNMENT);
            r.setMaximumSize(new Dimension(Integer.MAX_VALUE,r.getPreferredSize().height));
            form.add(r);
        }
        form.setPreferredSize(new Dimension(620,form.getPreferredSize().height+6));

        if(JOptionPane.showConfirmDialog(this,form,"Add Journal Entry",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;

        double amt;
        try { amt=Double.parseDouble(amountField.getText().trim().replace(",","")); if(amt<=0)throw new NumberFormatException(); }
        catch(NumberFormatException ex){error("Enter a valid amount greater than zero.");return;}
        double debitVal=amt, creditVal=amt;

        String dt=date.getText().trim(), dAcc=accOne.getText().trim(), cAcc=accTwo.getText().trim();
        try { LocalDate.parse(dt); }
        catch(Exception ex){error("Date must look like 2026-01-01.");return;}
        if(explain.getText().trim().isEmpty()){error("Enter a description.");return;}
        if(dAcc.isEmpty()||cAcc.isEmpty()){error("Enter both account titles.");return;}
        if(dAcc.equalsIgnoreCase(cAcc)){error("The two account titles must be different.");return;}

        String note=explain.getText().trim();
        String id=UUID.randomUUID().toString();

        Entry d=new Entry(dt,dAcc,note,(String)typeOne.getSelectedItem(),"Debit",debitVal);
        Entry c=new Entry(dt,cAcc,note,(String)typeTwo.getSelectedItem(),"Credit",creditVal);
        d.entryId=id; c.entryId=id;

        // ===== START MERGED CODE (NEW ADDITION): double-entry validation + JE-### numbering from the Journal back-end =====
        syncJournalBackend();
        JournalEntry draftEntry = new JournalEntry(dt, note)
                .addLine(dAcc, (String)typeOne.getSelectedItem(), "Debit", debitVal)
                .addLine(cAcc, (String)typeTwo.getSelectedItem(), "Credit", creditVal);
        JournalService.Result posted = journalService.create(current.email, draftEntry);
        if (!posted.ok()) { error(String.join("\n", posted.errors)); return; }
        d.entryId = posted.entry.id; c.entryId = posted.entry.id;
        // ===== END MERGED CODE (NEW ADDITION) =====

        activeBook.entries.add(d);
        activeBook.entries.add(c);
        activeBook.touch(); saveBook(activeBook); showMain();
    }

    JPanel ledger() {
        JPanel containerPanel=new JPanel();
        containerPanel.setLayout(new BoxLayout(containerPanel,BoxLayout.Y_AXIS));
        containerPanel.setBackground(Color.WHITE);
        Map<String,java.util.List<Entry>> accountMap=new LinkedHashMap<>();
        if(activeBook!=null) for(Entry e:activeBook.entries) accountMap.computeIfAbsent(e.account,k->new ArrayList<>()).add(e);
        if(accountMap.isEmpty()) {
            JPanel empty=card(); empty.add(label("No ledger accounts or entries found yet.",13,false,MUTED),BorderLayout.CENTER); containerPanel.add(empty);
        } else for(Map.Entry<String,java.util.List<Entry>> group:accountMap.entrySet()) {
            String accountName=group.getKey();
            java.util.List<Entry> accEntries=group.getValue();
            String category=accEntries.isEmpty()?"":accEntries.get(0).type;
            JPanel accCard=new JPanel(new BorderLayout(0,8));
            accCard.setBackground(Color.WHITE);
            accCard.setBorder(new CompoundBorder(new LineBorder(LINE),new EmptyBorder(12,12,12,12)));
            accCard.add(label("Account: "+accountName+" ("+category+")",13,true,INK),BorderLayout.NORTH);
            DefaultTableModel m=model("Date","Description","Debit","Credit");
            double totalDebit=0,totalCredit=0;
            for(Entry e:accEntries) {
                if(e.entrySide.equalsIgnoreCase("Debit"))totalDebit+=e.amount;else totalCredit+=e.amount;
                m.addRow(new Object[]{e.date,e.description,
                    e.entrySide.equalsIgnoreCase("Debit")?current.symbol+fmt.format(e.amount):"—",
                    e.entrySide.equalsIgnoreCase("Credit")?current.symbol+fmt.format(e.amount):"—"});
            }
            m.addRow(new Object[]{"","TOTAL",current.symbol+fmt.format(totalDebit),current.symbol+fmt.format(totalCredit)});
            double net=totalDebit-totalCredit;
            m.addRow(new Object[]{"","BALANCE ("+(net>=0?"Debit":"Credit")+")",
                net>=0?current.symbol+fmt.format(Math.abs(net)):"—",
                net<0?current.symbol+fmt.format(Math.abs(net)):"—"});
            JScrollPane scroll=new JScrollPane(table(m)); scroll.setPreferredSize(new Dimension(850,150));
            accCard.add(scroll,BorderLayout.CENTER);
            containerPanel.add(accCard); containerPanel.add(Box.createVerticalStrut(12));
        }
        JScrollPane mainScroll=new JScrollPane(containerPanel); mainScroll.setBorder(null);
        JPanel p=card();
        p.add(label("Ledger Accounts (Totals and Balance Layout)",14,true,INK),BorderLayout.NORTH);
        p.add(mainScroll,BorderLayout.CENTER);
        return p;
    }

    JPanel columnar12() {
        return columnar12Interactive();   // read-only 12-column sheet built from the journal
    }
    // ===== START MERGED CODE (NEW ADDITION): Journal back-end (Tasks 1 to 4) =====
    /** Starts the journal routes (/api/journal, /api/accounts) on this computer only. */
    void startJournalBackend() {
        journalApi = new JournalApiServer(journalService, em -> users.containsKey(em));
        try {
            journalApiPort = journalApi.start(8765);
        } catch (Exception busy) {
            try { journalApiPort = journalApi.start(0); }            // port 8765 is used, pick any free port
            catch (Exception failed) {
                journalApiPort = -1;
                System.err.println("Journal API could not start: " + failed.getMessage());
                return;
            }
        }
        System.out.println("Journal API running at http://localhost:" + journalApiPort + "/api/journal");
    }

    /** Loads the logged-in user's saved journal entries into the back-end (once per user), so JE numbers continue. */
    void syncJournalBackend() {
        if (current == null) return;
        String email = current.email.trim().toLowerCase(Locale.ROOT);
        if (email.equals(journalSyncedFor)) return;
        java.util.List<TransactionBook> books = getBooks();
        for (int b = books.size() - 1; b >= 0; b--) {                 // oldest workbook first
            java.util.List<Entry> list = books.get(b).entries;
            int i = 0;
            while (i < list.size()) {
                Entry first = list.get(i);
                int j = i + 1;
                if (first.entryId != null) {
                    while (j < list.size() && first.entryId.equals(list.get(j).entryId)) j++;
                    JournalEntry saved = new JournalEntry(first.entryId, first.date, first.description);
                    for (int k = i; k < j; k++) {
                        Entry e = list.get(k);
                        saved.addLine(e.account, e.type, e.entrySide, e.amount);
                    }
                    journalService.importSaved(email, saved);
                }
                i = j;
            }
        }
        journalSyncedFor = email;
    }
    // ===== END MERGED CODE (NEW ADDITION) =====

    // ===== START MERGED CODE (NEW ADDITION): Interactive 12-Column Worksheet (LedgerlyColumnar) =====
    /** The 12-column worksheet. It is READ-ONLY: it is rebuilt from the open workbook's journal entries every time it is shown. */
    JPanel columnar12Interactive() {
        String key = current.email + "|" + (activeBook == null ? "none" : activeBook.id);
        LedgerlyColumnar sheet = columnarSheets.get(key);
        if (sheet == null) {
            sheet = new LedgerlyColumnar();
            sheet.setReadOnly(true);
            columnarSheets.put(key, sheet);
        }
        // always show what the journal says right now (adds / deletes in the Journal are picked up here)
        sheet.model.clear();
        loadBookIntoColumnar(sheet);
        // Swing removes the column titles when the sheet leaves the screen (for example when you change tabs).
        // Put them back every time the worksheet is shown.
        sheet.scroll.setColumnHeaderView(sheet.header);

        JPanel p = card();
        JPanel head = new JPanel(new BorderLayout());
        head.setBackground(Color.WHITE);
        head.add(label("12-Column Worksheet \u00B7 Read-only (built from your Journal entries)", 13, true, INK), BorderLayout.WEST);
        head.setBorder(new EmptyBorder(0, 0, 4, 0));

        p.add(head, BorderLayout.NORTH);
        p.add(sheet.getContentPane(), BorderLayout.CENTER);
        return p;
    }

    /** Fills the worksheet's Account Name, Type and Trial Balance columns from the open workbook's journal entries. */
    void loadBookIntoColumnar(LedgerlyColumnar sheet) {
        if (activeBook == null) return;
        Map<String, double[]> totals = new LinkedHashMap<>();
        Map<String, String> kinds = new HashMap<>();
        for (Entry e : activeBook.entries) {
            double[] v = totals.computeIfAbsent(e.account, k -> new double[2]);
            if (e.entrySide.equalsIgnoreCase("Debit")) v[0] += e.amount; else v[1] += e.amount;
            kinds.put(e.account, e.type);
        }
        int r = 0;
        for (Map.Entry<String, double[]> t : totals.entrySet()) {
            String name = t.getKey();
            if (name == null || name.trim().isEmpty()) continue;
            double net = t.getValue()[0] - t.getValue()[1];
            sheet.model.setValueAt(name, r, 1);
            sheet.model.setValueAt(worksheetTypeFor(kinds.get(name), name), r, 2);
            if (net > 0.004) sheet.model.setValueAt(String.format(Locale.ROOT, "%.2f", net), r, WorksheetBackend.FIRST);
            else if (net < -0.004) sheet.model.setValueAt(String.format(Locale.ROOT, "%.2f", -net), r, WorksheetBackend.FIRST + 1);
            r++;
        }
    }

    /** Journal category (Assets, Liabilities ...) to the worksheet's account type. */
    WorksheetBackend.AccountType worksheetTypeFor(String category, String accountName) {
        String c = category == null ? "" : category.trim().toLowerCase(Locale.ROOT);
        switch (c) {
            case "assets": return WorksheetBackend.AccountType.ASSET;
            case "liabilities": return WorksheetBackend.AccountType.LIABILITY;
            case "revenue": return WorksheetBackend.AccountType.REVENUE;
            case "expenses": return WorksheetBackend.AccountType.EXPENSE;
            case "equity":
                return accountName != null && accountName.toLowerCase(Locale.ROOT).contains("draw")
                        ? WorksheetBackend.AccountType.DRAWINGS : WorksheetBackend.AccountType.CAPITAL;
            default: return WorksheetBackend.AccountType.NONE;
        }
    }
    // ===== END MERGED CODE (NEW ADDITION) =====

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LedgerlyAccounting().setVisible(true));
    }
}