import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class LedgerlyColumnar extends JFrame {

    static final int[] SEC_LIGHT = {0x1A8056, 0xD08A1E, 0x2A9D8F, 0x7CA82B, 0xB5651D, 0x3D7EA6};
    static final int[] SEC_DARK = {0x4CC79A, 0xE8B25A, 0x5CCFC0, 0xB2DB5A, 0xE29A5C, 0x7DB8E0};

    static final int[] W = {44, 260, 120};
    static final int AMT_W = 102, ROW_H = 36, TITLE_H = 40, SUB_H = 28;

    static boolean dark = false;
    static Color col(int light, int darkHex) { return new Color(dark ? darkHex : light); }
    static Color bg()     { return col(0xF6F9F7, 0x0F1714); }
    static Color card()   { return col(0xFFFFFF, 0x17221D); }
    static Color ink()    { return col(0x1F2A25, 0xE6EFEA); }
    static Color mute()   { return col(0x6B7770, 0x93A69C); }
    static Color line()   { return col(0xDDE5E0, 0x2A3A33); }
    static Color head()   { return col(0x145C40, 0x0B3626); }
    static Color sub()    { return col(0xE8F5EE, 0x1E3128); }
    static Color auto()   { return col(0xF1F5F3, 0x1B2822); }
    static Color footBg() { return col(0xE8F5EE, 0x1E3128); }
    static Color accent() { return col(0x1A8056, 0x4CC79A); }
    static Color bad()    { return col(0xD32F2F, 0xFF8A80); }
    static Color sec(int s) { return new Color(dark ? SEC_DARK[s % 6] : SEC_LIGHT[s % 6]); }

    class GroupHeader extends JPanel {
        GroupHeader() { setPreferredSize(new Dimension(sum(), TITLE_H + SUB_H)); }
        int sum() { int s = 0; for (int w : colW) s += w; return s; }
        void center(Graphics2D g, String[] lines, int x, int y, int w, int h) {
            FontMetrics m = g.getFontMetrics();
            int lh = m.getHeight(), top = y + (h - lh * lines.length) / 2 + m.getAscent();
            for (int i = 0; i < lines.length; i++)
                g.drawString(lines[i], x + (w - m.stringWidth(lines[i])) / 2, top + i * lh);
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(card()); g.fillRect(0, 0, getWidth(), getHeight());
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            int x = 0, total = TITLE_H + SUB_H;
            String[][] fixed = {{"#"}, {"ACCOUNT NAME"}, {"TYPE"}};
            for (int i = 0; i < WorksheetBackend.FIRST; i++) {
                int w = colW[i];
                g.setColor(head()); g.fillRect(x, 0, w, total);
                g.setColor(Color.WHITE); center(g, fixed[i], x, 0, w, total);
                g.setColor(line()); g.fillRect(x + w - 1, 0, 1, total);
                x += w;
            }
            for (int s = 0; s < model.sections.size() && WorksheetBackend.FIRST + 2 * s + 1 < colW.length; s++) {
                int w0 = colW[WorksheetBackend.FIRST + 2 * s], w1 = colW[WorksheetBackend.FIRST + 2 * s + 1], sw = w0 + w1;
                g.setColor(head()); g.fillRect(x, 0, sw, TITLE_H);
                g.setColor(sec(s)); g.fillRect(x, TITLE_H - 3, sw, 3);
                g.setColor(Color.WHITE); center(g, model.sections.get(s), x, 0, sw, TITLE_H - 3);
                int[] cx = {x, x + w0}, cw = {w0, w1};
                for (int j = 0; j < 2; j++) {
                    g.setColor(sub()); g.fillRect(cx[j], TITLE_H, cw[j], SUB_H);
                    g.setColor(ink()); center(g, new String[]{j == 0 ? "Dr" : "Cr"}, cx[j], TITLE_H, cw[j], SUB_H);
                    g.setColor(line()); g.fillRect(cx[j] + cw[j] - 1, TITLE_H, 1, SUB_H);
                }
                g.setColor(sec(s)); g.fillRect(x + sw - 2, 0, 2, total);
                x += sw;
            }
            g.setColor(line()); g.fillRect(0, total - 1, getWidth(), 1);
        }
    }

    final WorksheetBackend.WorksheetModel model = new WorksheetBackend.WorksheetModel();
    final JLabel status = new JLabel("Enter amounts");
    final JLabel profit = new JLabel("");
    final JLabel title = new JLabel("Ledgerly  \u00B7  Columnar");
    final JLabel hint = new JLabel("Tab / Shift+Tab = across  \u00B7  Enter / \u2193 = down (new row at the end)  \u00B7  \u2191 = up");
    final JComboBox<WorksheetBackend.AccountType> typeBox = new JComboBox<>(WorksheetBackend.AccountType.values());
    int[] colW = baseWidths(WorksheetBackend.FIRST + 12);
    JTable table;
    JScrollPane scroll;
    JPanel top;
    GroupHeader header;
    boolean readOnly = false;
    JButton clearBtn, finalizeBtn;

    static int[] baseWidths(int n) {
        int[] w = new int[n];
        for (int c = 0; c < n; c++) w[c] = c < WorksheetBackend.FIRST ? W[c] : AMT_W;
        return w;
    }

    JTable buildTable() {
        JTable t = new JTable(model) {
            @Override protected void configureEnclosingScrollPane() { }

            @Override public boolean isCellEditable(int r, int c) { return !readOnly && super.isCellEditable(r, c); }

            @Override public void changeSelection(int r, int c, boolean toggle, boolean extend) {
                int rows = model.rows;
                if (readOnly && r >= rows) r = rows - 1;
                else if (r == rows && getSelectedRow() == rows - 1 && !model.finalized) { model.addRow(); }
                else if (r >= rows) r = rows - 1;
                int k = c - WorksheetBackend.FIRST;
                if (k >= WorksheetBackend.INPUT) {
                    if (c == getSelectedColumn() + 1) { c = 1; r = Math.min(r + 1, model.rows - 1); }
                    else c = WorksheetBackend.FIRST + WorksheetBackend.INPUT - 1;
                }
                if (c < 1) c = 1;
                super.changeSelection(r, c, toggle, extend);
            }
        };
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        t.setRowHeight(ROW_H);
        t.setShowGrid(false);
        t.setTableHeader(null);
        t.setCellSelectionEnabled(true);
        t.setSurrendersFocusOnKeystroke(true);

        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(tb, v, sel, foc, r, c);
                int rows = model.rows;
                boolean footer = r >= rows, num = c >= WorksheetBackend.FIRST;
                int k = c - WorksheetBackend.FIRST;
                boolean auto = !footer && k >= WorksheetBackend.INPUT;
                if (num && v instanceof Double) setText(Math.abs((Double) v) < 0.005 ? "" : WorksheetBackend.MONEY.format((Double) v));
                setHorizontalAlignment(num ? RIGHT : (c == 0 ? CENTER : LEFT));
                setFont(num ? new Font(Font.MONOSPACED, footer ? Font.BOLD : Font.PLAIN, 13)
                            : new Font(Font.SANS_SERIF, (footer || c == 0) ? Font.BOLD : Font.PLAIN, footer ? 11 : 13));
                setOpaque(true);
                if (footer && c == 1) setFont(getFont().deriveFont(
                        java.util.Collections.singletonMap(java.awt.font.TextAttribute.TRACKING, 0.08f)));
                if (sel && !footer) setBackground(blend(card(), accent(), 0.25f));
                else setBackground(footer ? footBg() : auto ? auto() : card());
                setForeground(auto || c == 0 ? mute() : ink());
                if (!footer && c == 2 && v instanceof WorksheetBackend.AccountType && v != WorksheetBackend.AccountType.NONE)
                    setForeground(sec(((WorksheetBackend.AccountType) v).section()));
                Color vr = (num && k % 2 == 1) ? sec(k / 2) : line();
                int rw = (num && k % 2 == 1) ? 2 : (c == WorksheetBackend.FIRST - 1 ? 2 : 1);
                boolean gt = r == rows + 3;
                Border b = gt ? new MatteBorder(2, 0, 3, 0, ink()) : new MatteBorder(0, 0, 1, 0, line());
                setBorder(new CompoundBorder(new CompoundBorder(b, new MatteBorder(0, 0, 0, rw, vr)),
                        new EmptyBorder(0, (footer && c == 1) ? 10 : 8, 0, 8)));
                return this;
            }
        });

        JTextField tf = new JTextField();
        t.setDefaultEditor(Object.class, new DefaultCellEditor(tf) {
            @Override public Component getTableCellEditorComponent(JTable tb, Object v, boolean sel, int r, int c) {
                tf.setHorizontalAlignment(c >= WorksheetBackend.FIRST ? JTextField.RIGHT : JTextField.LEFT);
                tf.setBackground(card()); tf.setForeground(ink()); tf.setCaretColor(ink());
                tf.setBorder(new CompoundBorder(new LineBorder(accent(), 2), new EmptyBorder(0, 6, 0, 6)));
                String s = String.valueOf(v);
                if (v instanceof Double) s = ((Double) v) == 0 ? "" : String.valueOf(v);
                if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
                tf.setText(s);
                tf.selectAll();
                return tf;
            }
        });

        typeBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
        typeBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        t.setDefaultEditor(WorksheetBackend.AccountType.class, new DefaultCellEditor(typeBox));

        t.getModel().addTableModelListener(e -> refreshStatus());
        return t;
    }

    static Color blend(Color a, Color b, float t) {
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    void refreshStatus() {
        double np = model.netProfit();
        profit.setText(model.empty() ? "" : (np >= 0 ? "Net Profit: " : "Net Loss: ") + WorksheetBackend.MONEY.format(Math.abs(np)));
        profit.setForeground(np >= 0 ? accent() : bad());

        if (model.finalized) { status.setText("Finalized \u00B7 Locked"); status.setForeground(accent()); }
        else if (model.empty()) { status.setText("Enter amounts"); status.setForeground(mute()); }
        else if (model.balanced()) { status.setText("\u2713 Balanced"); status.setForeground(accent()); }
        else { status.setText("Not balanced \u00B7 check Dr = Cr"); status.setForeground(bad()); }
    }

    void fit() {
        int amt = model.amt(), n = WorksheetBackend.FIRST + amt;
        int vw = scroll.getViewport().getWidth();
        int fixed = 0;
        for (int w : W) fixed += w;
        int base = fixed + amt * AMT_W;
        int extra = Math.max(0, vw - base);
        int accExtra = extra / 4, perAmt = (extra - accExtra) / amt;
        colW = new int[n];
        colW[0] = W[0]; colW[1] = W[1] + accExtra; colW[2] = W[2];
        for (int k = 0; k < amt; k++) colW[WorksheetBackend.FIRST + k] = AMT_W + perAmt;
        if (extra > 0) colW[n - 1] += vw - header.sum();
        TableColumnModel cm = table.getColumnModel();
        for (int c = 0; c < cm.getColumnCount() && c < n; c++) {
            cm.getColumn(c).setPreferredWidth(colW[c]);
            cm.getColumn(c).setWidth(colW[c]);
        }
        header.setPreferredSize(new Dimension(header.sum(), TITLE_H + SUB_H));
        header.revalidate();
        table.revalidate();
        header.repaint();
        table.repaint();
    }

    void applyTheme() {
        getContentPane().setBackground(bg());
        top.setBackground(bg());
        JPanel right = (JPanel) top.getComponent(1);
        right.setBackground(bg());
        for (Component c : right.getComponents()) if (c instanceof JButton) { c.setBackground(card()); c.setForeground(ink()); }
        title.setForeground(ink());
        hint.setForeground(mute());
        typeBox.setBackground(card());
        typeBox.setForeground(ink());
        table.setBackground(card());
        scroll.getViewport().setBackground(card());
        scroll.setBorder(new LineBorder(line()));
        refreshStatus();
        repaint();
    }

    LedgerlyColumnar() {
        super("Ledgerly \u2013 Columnar");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        table = buildTable();
        header = new GroupHeader();
        scroll = new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setColumnHeaderView(header);
        scroll.getVerticalScrollBar().setUnitIncrement(ROW_H);
        scroll.getHorizontalScrollBar().setUnitIncrement(AMT_W);
        installSmoothScroll(scroll);
        installKeys(table);
        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { fit(); }
        });

        model.addTableModelListener(e -> {
            if (e.getFirstRow() == javax.swing.event.TableModelEvent.HEADER_ROW) SwingUtilities.invokeLater(this::fit);
        });

        title.setFont(title.getFont().deriveFont(Font.BOLD, 17f));
        status.setFont(status.getFont().deriveFont(Font.BOLD, 12f));
        profit.setFont(profit.getFont().deriveFont(Font.BOLD, 12f));
        JButton clear = new JButton("Clear"), theme = new JButton("Theme"), fin = new JButton("Finalize");
        clearBtn = clear; finalizeBtn = fin;

        clear.addActionListener(e -> {
            if (table.isEditing()) table.getCellEditor().cancelCellEditing();
            if (model.finalized) {
                int ok = JOptionPane.showConfirmDialog(this,
                        "This period is finalized. Discard it and start a new period?",
                        "New period", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ok != JOptionPane.YES_OPTION) return;
            }
            model.clear();
        });
        theme.addActionListener(e -> { dark = !dark; applyTheme(); });

        fin.addActionListener(e -> {
            if (model.finalized) return;
            if (table.isEditing()) table.getCellEditor().stopCellEditing();
            String problem = model.verify();
            if (problem != null) {
                JOptionPane.showMessageDialog(this, problem, "Cannot finalize", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int ok = JOptionPane.showConfirmDialog(this,
                    "Finalize and lock this worksheet? No more changes will be allowed.",
                    "Finalize", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) model.finalizeReport();
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.add(profit); right.add(status); right.add(fin); right.add(clear); right.add(theme);
        top = new JPanel(new BorderLayout(12, 0));
        top.setBorder(new EmptyBorder(12, 18, 12, 18));
        top.add(title, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(0, 18, 0, 18));
        center.add(scroll);
        hint.setBorder(new EmptyBorder(8, 18, 12, 18));

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(hint, BorderLayout.SOUTH);

        setSize(1280, 720);
        setLocationRelativeTo(null);
        applyTheme();
        fit();
    }

    /** Read-only mode: nothing can be typed or changed, and the Finalize / Clear buttons are removed. */
    void setReadOnly(boolean ro) {
        readOnly = ro;
        if (ro && table.isEditing()) table.getCellEditor().cancelCellEditing();
        finalizeBtn.setVisible(!ro);
        clearBtn.setVisible(!ro);
        hint.setText(ro ? "Read-only  \u00B7  this worksheet is built from the Journal entries and cannot be edited here"
                        : "Tab / Shift+Tab = across  \u00B7  Enter / \u2193 = down (new row at the end)  \u00B7  \u2191 = up");
        table.repaint();
    }

    static void installSmoothScroll(JScrollPane sp) {
        sp.setWheelScrollingEnabled(false);
        final int[] target = new int[2];
        final javax.swing.Timer timer = new javax.swing.Timer(10, null);
        timer.addActionListener(e -> {
            boolean moving = false;
            for (int i = 0; i < 2; i++) {
                JScrollBar bar = i == 0 ? sp.getVerticalScrollBar() : sp.getHorizontalScrollBar();
                int diff = target[i] - bar.getValue();
                if (diff != 0) {
                    int step = diff / 4;
                    if (step == 0) step = diff > 0 ? 1 : -1;
                    bar.setValue(bar.getValue() + step);
                    moving = true;
                }
            }
            if (!moving) timer.stop();
        });
        sp.addMouseWheelListener(e -> {
            if (!timer.isRunning()) {
                target[0] = sp.getVerticalScrollBar().getValue();
                target[1] = sp.getHorizontalScrollBar().getValue();
            }
            int i = e.isShiftDown() ? 1 : 0;
            JScrollBar bar = i == 0 ? sp.getVerticalScrollBar() : sp.getHorizontalScrollBar();
            int max = bar.getMaximum() - bar.getVisibleAmount();
            int d = e.getWheelRotation() * (i == 0 ? ROW_H * 3 : AMT_W);
            target[i] = Math.max(0, Math.min(max, target[i] + d));
            timer.start();
        });
    }

    void installKeys(JTable t) {
        InputMap im = t.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = t.getActionMap();
        String[][] keys = {{"HOME", "rowStart"}, {"END", "rowEnd"}, {"ctrl HOME", "gridStart"}, {"ctrl END", "gridEnd"}};
        for (String[] k : keys) im.put(KeyStroke.getKeyStroke(k[0]), k[1]);
        am.put("rowStart", new AbstractAction() { public void actionPerformed(java.awt.event.ActionEvent e) {
            t.changeSelection(Math.max(0, t.getSelectedRow()), 1, false, false); } });
        am.put("rowEnd", new AbstractAction() { public void actionPerformed(java.awt.event.ActionEvent e) {
            t.changeSelection(Math.max(0, t.getSelectedRow()), t.getColumnCount() - 1, false, false); } });
        am.put("gridStart", new AbstractAction() { public void actionPerformed(java.awt.event.ActionEvent e) {
            t.changeSelection(0, 1, false, false); } });
        am.put("gridEnd", new AbstractAction() { public void actionPerformed(java.awt.event.ActionEvent e) {
            t.changeSelection(model.rows - 1, t.getColumnCount() - 1, false, false); } });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LedgerlyColumnar().setVisible(true));
    }
}