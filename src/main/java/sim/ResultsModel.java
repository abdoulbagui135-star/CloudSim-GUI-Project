src/main/java/sim/ResultsModel.java

package sim;

import org.cloudbus.cloudsim.Cloudlet;

import java.util.*;

/**
 * Modèle de données pour l'affichage des résultats
 * dans l'interface graphique.
 */
public class ResultsModel {

    /** Objet représentant une ligne dans la table de résultats */
    public static class Row {
        public int cloudletId;
        public int vmId;
        public int dcId;
        public double start;
        public double finish;
        public String status;
        public long length;

        public Row(int cloudletId, int vmId, int dcId,
                   double start, double finish,
                   String status, long length) {

            this.cloudletId = cloudletId;
            this.vmId = vmId;
            this.dcId = dcId;
            this.start = start;
            this.finish = finish;
            this.status = status;
            this.length = length;
        }
    }

    /** Liste des résultats */
    public final List<Row> rows = new ArrayList<>();

    /** Résumé global (makespan, etc.) */
    public final String summary;

    private ResultsModel(List<Row> rows, String summary) {
        this.rows.addAll(rows);
        this.summary = summary;
    }

    /**
     * Création d'un ResultsModel à partir
     * de la liste de Cloudlets exécutés.
     */
    public static ResultsModel from(List<Cloudlet> list) {

        List<Row> rows = new ArrayList<>();
        double makespan = 0.0;

        for (Cloudlet c : list) {

            rows.add(new Row(
                    c.getCloudletId(),
                    c.getVmId(),
                    c.getResourceId(),
                    c.getExecStartTime(),
                    c.getFinishTime(),
                    Cloudlet.getStatusString(c.getStatus()),
                    c.getCloudletLength()
            ));

            if (c.getFinishTime() > makespan) {
                makespan = c.getFinishTime();
            }
        }

        String summary =
                "Cloudlets terminés : " + list.size() +
                " | Makespan : " + String.format(Locale.US, "%.2f", makespan);

        return new ResultsModel(rows, summary);
    }
}
  
