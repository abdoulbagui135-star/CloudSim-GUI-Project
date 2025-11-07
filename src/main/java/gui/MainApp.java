src/main/java/gui/MainApp.java
package gui;

import sim.SimulationManager;
import sim.ResultsModel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class MainApp extends JFrame {
    private final SimulationManager manager = new SimulationManager();
    private final JTextArea log = new JTextArea(10, 60);
    private final JComboBox<String> vmScheduler = new JComboBox<>(new String[]{"TimeShared","SpaceShared"});
    private final JComboBox<String> cloudletScheduler = new JComboBox<>(new String[]{"TimeShared","SpaceShared"});
    private final JComboBox<String> vmAllocPolicy = new JComboBox<>(new String[]{"Simple","Custom"});
    private final JTable resultsTable = new JTable(
            new DefaultTableModel(new Object[]{"CloudletID","VM","DC","Start","Finish","Status","Length"},0)
    );

    public MainApp() {
        super("CloudSim GUI Project");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new GridLayout(2,1));

        JPanel configPanel = new JPanel();
        configPanel.add(new JLabel("VM Scheduler:"));
        configPanel.add(vmScheduler);

        configPanel.add(new JLabel("Cloudlet Scheduler:"));
        configPanel.add(cloudletScheduler);

        configPanel.add(new JLabel("VM Allocation:"));
        configPanel.add(vmAllocPolicy);

        topPanel.add(configPanel);

        JPanel actions = new JPanel();
        actions.add(button("Add DC", e -> addDC()));
        actions.add(button("Add Host", e -> addHost()));
        actions.add(button("Add VM", e -> addVM()));
        actions.add(button("Add Cloudlet", e -> addCloudlet()));
        actions.add(button("Add Link", e -> addLink()));
        actions.add(button("Run", e -> runSim()));
        actions.add(button("Pause", e -> pause()));
        actions.add(button("Resume", e -> resumeSim()));
        actions.add(button("Export CSV", e -> exportCsv()));

        topPanel.add(actions);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(log), BorderLayout.CENTER);
        add(new JScrollPane(resultsTable), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    private JButton button(String text, AbstractAction action) {
        JButton b = new JButton(action);
        b.setText(text);
        return b;
    }

    private int ask(String message, int def) {
        String s = JOptionPane.showInputDialog(this, message, ""+def);
        try { return Integer.parseInt(s); }
        catch(Exception e) { return def; }
    }

    private void addDC() {
        int id = manager.addDatacenter();
        log.append("Datacenter added: " + id + "\n");
    }

    private void addHost() {
        int ram = ask("RAM (MB)", 16384);
        long bw = ask("Bandwidth", 10000);
        long storage = ask("Storage", 1000000);
        int pes = ask("PEs", 4);
        int id = manager.addHost(ram,bw,storage,pes, vmScheduler.getSelectedItem().toString());
        log.append("Host added: " + id + "\n");
    }

    private void addVM() {
        int mips = ask("MIPS", 1000);
        int pes = ask("PEs", 1);
        int ram = ask("RAM (MB)", 1024);
        long bw = ask("Bandwidth", 1000);
        long size = ask("Size", 10000);
        int id = manager.addVm(mips,pes,ram,bw,size,cloudletScheduler.getSelectedItem().toString());
        log.append("VM added: " + id + "\n");
    }

    private void addCloudlet() {
        long length = ask("Length", 10000);
        int pes = ask("PEs", 1);
        int id = manager.addCloudlet(length,pes,300,300);
        log.append("Cloudlet added: " + id + "\n");
    }

    private void addLink() {
        int a = ask("Entity A ID", manager.getBrokerId());
        int b = ask("Entity B ID", manager.getFirstDatacenterId());
        double bw = ask("Bandwidth", 10000000);
        double lat = ask("Latency", 10);
        boolean ok = manager.addLink(a,b,bw,lat);
        log.append( ok ? "Link added\n" : "Failed to add link\n" );
    }

    private void runSim() {
        boolean custom = vmAllocPolicy.getSelectedItem().toString().equals("Custom");
        ResultsModel results = manager.run(custom);
        DefaultTableModel model = (DefaultTableModel) resultsTable.getModel();
        model.setRowCount(0);

        for(ResultsModel.Row r: results.rows){
            model.addRow(new Object[]{r.cloudletId,r.vmId,r.dcId,r.start,r.finish,r.status,r.length});
        }

        log.append(results.summary + "\n");
    }

    private void pause() {
        manager.pause();
        log.append("Simulation paused\n");
    }

    private void resumeSim() {
        manager.resume();
        log.append("Simulation resumed\n");
    }

    private void exportCsv() {
        String p = manager.exportCsv();
        log.append("Exported to: " + p + "\n");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainApp().setVisible(true));
    }
}
