/**
 * Genera PDF de vista previa/maqueta para Solicitud de Dotación.
 */
public class VistaPreviaSolicitudDotacion {
    public static void main(String[] args) {
        try {
            String codigo = "DOT-S1002";
            if (args.length > 0) {
                codigo = args[0];
            }

            ReporteSolicitudDotacion reporte = new ReporteSolicitudDotacion(null);
            System.out.println("Generando vista previa de dotación para: " + codigo);
            reporte.generarVistaPrevia(codigo);
            System.out.println("Vista previa completada.");
            System.exit(0);
        } catch (Exception e) {
            System.err.println("Error generando vista previa de dotación:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
