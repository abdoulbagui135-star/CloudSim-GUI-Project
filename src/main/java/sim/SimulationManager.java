src/main/java/sim/SimulationManager.java
package sim;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.network.datacenter.NetworkTopology;

import java.util.*;
import java.io.FileWriter;
import java.io.IOException;

public class SimulationManager {

    private Datacenter datacenter;
    private DatacenterBroker broker;

    private final List<Host> hostList = new ArrayList<>();
    private final List<Vm> vmList = new ArrayList<>();
    private final List<Cloudlet> cloudletList = new ArrayList<>();

    private int lastId = 1000;

    public SimulationManager() {
        CloudSim.init(1, Calendar.getInstance(), false);
        try {
            broker = new DatacenterBroker("Broker");
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int getBrokerId() { return broker.getId(); }

    public int getFirstDatacenterId() {
        return datacenter == null ? -1 : datacenter.getId();
    }

    /** ------------------------------
     *  ADD DATACENTER
     *  ------------------------------ */
    public int addDatacenter() {
        List<Host> emptyHostList = new ArrayList<>();

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                "x86", "Linux", "Xen",
                emptyHostList,
                10.0, 3.0, 0.05, 0.001, 0.0
        );

        VmAllocationPolicy policy = new VmAllocationPolicySimple(emptyHostList);

        try {
            datacenter = new Datacenter(
                    "Datacenter-" + (++lastId),
                    characteristics,
                    policy,
                    new LinkedList<Storage>(),
                    0
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        hostList.clear();
        hostList.addAll(emptyHostList);

        return datacenter.getId();
    }

    /** ------------------------------
     *  ADD HOST
     *  ------------------------------ */
    public int addHost(int ram, long bw, long storage, int pes, String vmScheduler) {

        List<Pe> peList = new ArrayList<>();
        for (int i = 0; i < pes; i++) {
            peList.add(new Pe(i, new PeProvisionerSimple(1000)));
        }

        VmScheduler scheduler =
            vmScheduler.equals("SpaceShared") ?
                    new VmSchedulerSpaceShared(peList) :
                    new VmSchedulerTimeShared(peList);

        Host host = new Host(
                ++lastId,
                new RamProvisionerSimple(ram),
                new BwProvisionerSimple(bw),
                storage,
                peList,
                scheduler
        );

        hostList.add(host);

        if (datacenter != null) {
            datacenter.getHostList().add(host);
        }

        return host.getId();
    }

    /** ------------------------------
     *  ADD VM
     *  ------------------------------ */
    public int addVm(int mips, int pes, int ram, long bw, long size, String cloudletSchedulerType) {

        CloudletScheduler scheduler =
            cloudletSchedulerType.equals("SpaceShared") ?
                new CloudletSchedulerSpaceShared() :
                new CloudletSchedulerTimeShared();

        Vm vm = new Vm(
                ++lastId,
                broker.getId(),
                mips,
                pes,
                ram,
                bw,
                size,
                "Xen",
                scheduler
        );

        vmList.add(vm);

        return vm.getId();
    }

    /** ------------------------------
     *  ADD CLOUDLET
     *  ------------------------------ */
    public int addCloudlet(long length, int pes, long fileSize, long outputSize) {

        UtilizationModel um = new UtilizationModelFull();

        Cloudlet cl = new Cloudlet(
                ++lastId,
                length,
                pes,
                fileSize,
                outputSize,
                um, um, um
        );

        cl.setUserId(broker.getId());
        cloudletList.add(cl);

        return cl.getCloudletId();
    }

    /** ------------------------------
     *  ADD NETWORK LINK
     *  ------------------------------ */
    public boolean addLink(int a, int b, double bw, double lat) {
        try {
            NetworkTopology.addLink(a, b, bw, lat);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** ------------------------------
     *  RUN SIMULATION
     *  ------------------------------ */
    public ResultsModel run(boolean useCustomPolicy) {

        if (datacenter == null) {
            addDatacenter();
        }

        if (useCustomPolicy) {
            datacenter.setVmAllocationPolicy(
                    new CustomVmAllocationPolicy(datacenter.getHostList())
            );
        }

        broker.submitVmList(vmList);
        broker.submitCloudletList(cloudletList);

        CloudSim.startSimulation();
        CloudSim.stopSimulation();

        List<Cloudlet> results = broker.getCloudletReceivedList();

        return ResultsModel.from(results);
    }

    /** ------------------------------
     *  PAUSE / RESUME
     *  ------------------------------ */
    public void pause() {
        CloudSim.pauseSimulation();
    }

    public void resume() {
        CloudSim.resumeSimulation();
    }

    /** ------------------------------
     *  EXPORT CSV
     *  ------------------------------ */
    public String exportCsv() {
        String path = "results.csv";

        try(FileWriter fw = new FileWriter(path)) {
            fw.write("id,vm,dc,start,finish,status,length\n");

            for (Cloudlet c : broker.getCloudletReceivedList()) {
                fw.write(
                        c.getCloudletId() + "," +
                        c.getVmId() + "," +
                        c.getResourceId() + "," +
                        c.getExecStartTime() + "," +
                        c.getFinishTime() + "," +
                        Cloudlet.getStatusString(c.getStatus()) + "," +
                        c.getCloudletLength() + "\n"
                );
            }

        } catch(IOException e) {
            return "Erreur: " + e.getMessage();
        }

        return path;
    }
}
