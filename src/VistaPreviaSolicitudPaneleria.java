/**
 * Genera PDF de vista previa/maqueta para Solicitud de Panelería.
 */
public class VistaPreviaSolicitudPaneleria {
    public static void main(String[] args) {
        try {
            String codigo = "PAN-S1006";
            if (args.length > 0) {
                codigo = args[0];
            }

            ReporteSolicitudPaneleria reporte = new ReporteSolicitudPaneleria(null);
            System.out.println("Generando vista previa de panelería para: " + codigo);
            reporte.generarVistaPrevia(codigo);
            System.out.println("Vista previa completada.");
            System.exit(0);
        } catch (Exception e) {
            System.err.println("Error generando vista previa de panelería:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
