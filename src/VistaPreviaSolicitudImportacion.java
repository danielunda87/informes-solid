/**
 * Genera PDFs de comparacion sin conectarse a la base de datos.
 */
public class VistaPreviaSolicitudImportacion {
    public static void main(String[] args) {
        try {
            String codigo = "VISTA-PREVIA";
            if (args.length > 0) {
                codigo = args[0];
            }

            for (ReporteSolicitudImportacion.FormatoImpresion formato
                    : ReporteSolicitudImportacion.FormatoImpresion.values()) {
                ReporteSolicitudImportacion reporte = new ReporteSolicitudImportacion(null, formato);
                reporte.generarVistaPrevia(codigo);
            }
            System.exit(0);
        } catch (Exception e) {
            System.err.println("Error generando vistas previas:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
