package org.drone_remote_unit.view;

import org.drone_remote_unit.controller.SimpleLogger;
import org.drone_remote_unit.controller.IController;
import org.drone_remote_unit.model.SerialManager;
import org.drone_remote_unit.model.State;
import org.drone_remote_unit.view.ViewUtils.Form;
import jssc.SerialPortException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

/**
 * MainPage.
 */
public final class MainPage implements IView {

    private static final String CLASS_NAME = MainPage.class.getName();
    private static final Logger LOGGER = SimpleLogger.getLogger(CLASS_NAME);
    private static final int WINDOW_WIDTH = 700;
    private static final int WINDOW_HEIGHT = 600;
    private static final int GAP = 10;
    private static final String NOT_CONNECTED = "Not connected";
    private static final String UNKNOWN = "Unknown";
    private static final Border EMPTY_BORDER = new EmptyBorder(GAP, GAP, GAP, GAP);
    private static final float SMALL_LABEL_SIZE = 12.0f;
    private static final int SERIAL_MONITOR_HEIGHT = (int) (WINDOW_HEIGHT * 0.2);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final JFrame frame = new JFrame("DroneRemoteUnit");
    private final JPanel mainPanel;
    private final JButton takeOffButton = new JButton("Request Take Off");
    private final JButton landingButton = new JButton("Request Landing");
    private final JTextField state = new JTextField(NOT_CONNECTED);
    private final JTextField temperature = new JTextField(UNKNOWN);
    private final JTextField distance = new JTextField(UNKNOWN);
    private final JTextField response = new JTextField("No errors");
    private final JTextArea console = new JTextArea();
    private IController controller;

    /**
     * MainPage constructor, initialize the main window and its components.
     */
    public MainPage() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        frame.setResizable(false);

        final JPanel container = new JPanel(new BorderLayout(GAP, GAP));
        container.setBorder(EMPTY_BORDER);

        // Top panel: status
        final JPanel serialSettingsPanel = buildSerialSettingsPanel();
        container.add(serialSettingsPanel, BorderLayout.EAST);

        // Main panel: telemetry and actions
        mainPanel = buildMainPanel();
        disablePanel(mainPanel);
        container.add(mainPanel, BorderLayout.CENTER);

        // Bottom panel: serial monitor
        final JPanel bottomPanel = buildBottomPanel();
        container.add(bottomPanel, BorderLayout.SOUTH);

        frame.setContentPane(container);
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVisible(final boolean b) {
        SwingUtilities.invokeLater(() -> frame.setVisible(b));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setController(final IController controller) {
        this.controller = controller;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setState(final State state) {
        SwingUtilities.invokeLater(() -> this.state.setText(state.toString()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTemperature(final float temperature) {
        SwingUtilities.invokeLater(() -> this.temperature.setText(String.format("%.2f°C", temperature)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDistance(final float distance) {
        SwingUtilities.invokeLater(() -> this.distance.setText(String.format("%.2fm", distance)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void pushSerialLine(final String text) {
        SwingUtilities.invokeLater(() -> {
            this.console.append(String.format("%1$tT %2$s", LocalTime.now(), text));
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showAlert(final String msg, final boolean isError) {
        SwingUtilities.invokeLater(() -> {
            this.response.setText(msg);
            if (isError) {
                this.response.setForeground(Color.RED);
            } else {
                this.response.setForeground(Color.BLACK);
            }
            this.response.repaint();
        });
    }

    private <T extends Comparable<T>> JComboBox<T> buildComboBox(final Collection<T> collection) {
        final SortedComboBoxModel<T> model = new SortedComboBoxModel<>(collection);
        final JComboBox<T> comboBox = new JComboBox<>(model);
        comboBox.setSelectedIndex(comboBox.getItemCount() - 1);
        return comboBox;
    }

    private <K, V extends Comparable<V>> JComboBox<Entry<K, V>> buildComboBox(final Map<K, V> map) {
        final Set<Entry<K, V>> entries = map.entrySet();
        final SortedComboBoxModel<Entry<K, V>> model =
                new SortedComboBoxModel<>(entries, (s1, s2) -> s1.getValue().compareTo(s2.getValue()));
        final JComboBox<Entry<K, V>> comboBox = new JComboBox<>(model);
        comboBox.setSelectedIndex(comboBox.getItemCount() - 1);
        final Border defBorder = ((JLabel) comboBox.getRenderer()).getBorder();
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                    final int index, final boolean isSelected, final boolean cellHasFocus) {
                final JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value != null ? ((Entry<K, V>) value).getKey() : "", index, isSelected, cellHasFocus
                );
                label.setBorder(defBorder);
                return label;
            }
        });

        return comboBox;
    }

    private JPanel buildSerialSettingsPanel() {
        final Form form = new Form();
        form.setBorder(new TitledBorder("Serial settings"));

        final JComboBox<String> portComboBox = buildComboBox(SerialManager.getPorts());
        form.addField("Port:", portComboBox);

        final JComboBox<Integer> baudRateComboBox = buildComboBox(SerialManager.getBaudRates());
        form.addField("Baud rate:", baudRateComboBox);

        final JComboBox<Integer> dataBitsComboBox = buildComboBox(SerialManager.getDataBits());
        form.addField("Data bits:", dataBitsComboBox);

        final JComboBox<Entry<String, Integer>> stopBitsComboBox = buildComboBox(SerialManager.getStopBits());
        form.addField("Stop bits:", stopBitsComboBox);

        final JComboBox<Entry<String, Integer>> parityComboBox = buildComboBox(SerialManager.getParity());
        form.addField("Parity:", parityComboBox);

        final JButton connectButton = new JButton("Connect");
        if (portComboBox.getItemCount() == 0) {
            connectButton.setEnabled(false);
        }
        form.addCenterComponent(connectButton);
        final JButton disconnectButton = new JButton("Disconnect");
        disconnectButton.setEnabled(false);
        form.addCenterComponent(disconnectButton);

        final JButton refreshButton = new JButton("Refresh ports");
        form.addCenterComponent(refreshButton);

        connectButton.addActionListener(f -> {
            if (controller == null) {
                return;
            }

            final String port = (String) portComboBox.getSelectedItem();
            final int baudRate = (int) baudRateComboBox.getSelectedItem();
            final int dataBits = (int) dataBitsComboBox.getSelectedItem();
            final int stopBits = ((Entry<String, Integer>) stopBitsComboBox.getSelectedItem()).getValue();
            final int parity = ((Entry<String, Integer>) parityComboBox.getSelectedItem()).getValue();

            try {
                controller.initSerial(port, baudRate, dataBits, stopBits, parity);
                enablePanel(mainPanel);
                connectButton.setEnabled(false);
                disconnectButton.setEnabled(true);
                refreshButton.setEnabled(false);

                scheduler.scheduleAtFixedRate(
                    () -> {
                        controller.queryState();
                        controller.queryTemperature();
                        if (state.getText().equals(State.LANDING.toString())) {
                            controller.queryDistance();
                        } else {
                            distance.setText(UNKNOWN);
                        }
                    },
                    4,
                    2,
                    TimeUnit.SECONDS
                );
            } catch (final SerialPortException e) {
                JOptionPane.showMessageDialog(
                    frame,
                   "Error opening serial port.",
                   "Error",
                   JOptionPane.ERROR_MESSAGE);
                LOGGER.logp(
                    Level.SEVERE,
                    "MainPage",
                    "ConnectActionListener",
                    e.getMessage(),
                    e);
            }
        });

        disconnectButton.addActionListener(f -> {
            if (controller == null) {
                return;
            }

            try {
                controller.closeSerial();
                disablePanel(mainPanel);
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
                refreshButton.setEnabled(true);
                state.setText(NOT_CONNECTED);
                temperature.setText(UNKNOWN);
                distance.setText(UNKNOWN);
                showAlert("No error", false);
            } catch (final SerialPortException e) {
                JOptionPane.showMessageDialog(
                    frame,
                   "Error closing serial port.",
                   "Error",
                   JOptionPane.ERROR_MESSAGE);
                LOGGER.logp(
                    Level.SEVERE,
                    "MainPage",
                    "DisconnectActionListener",
                    e.getMessage(),
                    e);
            }
        });

        refreshButton.addActionListener(f -> {
            SerialManager.refreshPorts();
            portComboBox.removeAllItems();
            for (final String s : SerialManager.getPorts()) {
                portComboBox.addItem(s);
            }
            if (portComboBox.getItemCount() > 0) {
                portComboBox.setSelectedIndex(0);
                connectButton.setEnabled(
                    controller != null && !controller.isConnected()
                );
            } else {
                connectButton.setEnabled(false);
            }
        });

        return form;
    }

    private JPanel buildMainPanel() {
        final JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Main"));

        final JPanel buttons = new JPanel();
        takeOffButton.addActionListener(e -> controller.requestTakeOff());
        // panel.add(takeOffButton, JLayeredPane.DEFAULT_LAYER);
        buttons.add(takeOffButton);
        landingButton.addActionListener(e -> controller.requestLanding());
        buttons.add(landingButton);
        panel.add(buttons, BorderLayout.NORTH);

        final Form labels = new Form();
        labels.setBorder(EMPTY_BORDER);
        state.setEditable(false);
        labels.addField("State", state);
        temperature.setEditable(false);
        labels.addField("Temperature", temperature);
        distance.setEditable(false);
        labels.addField("Distance", distance);
        panel.add(labels, BorderLayout.CENTER);

        final JPanel errors = new JPanel(new BorderLayout());
        errors.setBorder(new TitledBorder("Messages"));
        final JLabel notice = new JLabel(
            "TimeoutException may indicate the reception of an invalid response. See logs"
        );
        notice.setFont(notice.getFont().deriveFont(SMALL_LABEL_SIZE));
        errors.add(notice, BorderLayout.NORTH);
        response.setEditable(false);
        errors.add(response, BorderLayout.CENTER);
        panel.add(errors, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildBottomPanel() {
        final JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Serial monitor"));

        console.setEditable(false);
        console.setAutoscrolls(true);

        final JScrollPane scroller = new JScrollPane(console);
        scroller.setPreferredSize(new Dimension(0, SERIAL_MONITOR_HEIGHT));
        panel.add(scroller, BorderLayout.CENTER);

        return panel;
    }

    private void setComponentsEnabled(final Component comp, final boolean enabled) {
        comp.setEnabled(enabled);

        if (comp instanceof Container) {
            for (final Component child : ((Container) comp).getComponents()) {
                setComponentsEnabled(child, enabled);
            }
        }
    }

    private void disablePanel(final JPanel panel) {
        setComponentsEnabled(panel, false);
        panel.repaint();
    }

    private void enablePanel(final JPanel panel) {
        setComponentsEnabled(panel, true);
        panel.repaint();
    }
}
