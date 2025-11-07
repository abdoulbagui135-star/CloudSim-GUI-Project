src/main/java/sim/CustomVmAllocationPolicy.java

package sim;

import org.cloudbus.cloudsim.*;

import java.util.*;

/**
 * Politique personnalisée d'allocation des VMs.
 * Implémentation Best-Fit basée sur la RAM disponible.
 *
 * Objectif :
 * - choisir l'Host qui a suffisamment de RAM pour accueillir la VM
 * - parmi ces Hosts, sélectionner celui qui a
 *   la plus petite quantité de RAM libre (best-fit).
 */
public class CustomVmAllocationPolicy extends VmAllocationPolicy {

    private final List<? extends Host> hostList;
    private final Map<String, Host> vmTable = new HashMap<>();

    public CustomVmAllocationPolicy(List<? extends Host> list) {
        super(list);
        this.hostList = list;
    }

    private String vmUid(Vm vm) {
        return vm.getId() + "-" + vm.getUserId();
    }

    @Override
    public boolean allocateHostForVm(Vm vm) {

        Host bestHost = null;
        long requiredRam = vm.getRam();
        long bestFreeRam = Long.MAX_VALUE;

        for (Host h : hostList) {

            long freeRam = h.getRamProvisioner().getAvailableRam();

            if (h.isSuitableForVm(vm) && freeRam >= requiredRam) {

                if (freeRam < bestFreeRam) {
                    bestFreeRam = freeRam;
                    bestHost = h;
                }
            }
        }

        if (bestHost == null) {
            return false;
        }

        boolean success = bestHost.vmCreate(vm);

        if (success) {
            vmTable.put(vmUid(vm), bestHost);
        }

        return success;
    }

    @Override
    public boolean allocateHostForVm(Vm vm, Host host) {
        if (host.vmCreate(vm)) {
            vmTable.put(vmUid(vm), host);
            return true;
        }
        return false;
    }

    @Override
    public void deallocateHostForVm(Vm vm) {
        Host host = vmTable.remove(vmUid(vm));
        if (host != null) {
            host.vmDestroy(vm);
        }
    }

    @Override
    public Host getHost(Vm vm) {
        return vmTable.get(vmUid(vm));
    }

    @Override
    public Host getHost(int vmId, int userId) {
        return vmTable.get(vmId + "-" + userId);
    }
}
