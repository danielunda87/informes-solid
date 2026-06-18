/**
 * Punto de entrada para probar el informe FT-CE-01 - Solicitud de Importacion.
 */
public class PruebaSolicitudImportacion {
    public static void main(String[] args) {
        try {
            ConexionDatos conexion = new ConexionDatos("solidpruebas3");

            if (conexion.estaConectado()) {
                String codigo = "IMP-S1011";
                if (args.length > 0) {
                    codigo = args[0];
                }

                ReporteSolicitudImportacion.FormatoImpresion formato =
                        ReporteSolicitudImportacion.FormatoImpresion.CLASICO;
                if (args.length > 1) {
                    formato = ReporteSolicitudImportacion.FormatoImpresion.desdeTexto(args[1]);
                }

                ReporteSolicitudImportacion reporte = new ReporteSolicitudImportacion(conexion, formato);

                System.out.println("Generando solicitud de importacion para: " + codigo);
                System.out.println("Formato de impresion: " + formato.getCodigo());
                reporte.generar(codigo);
                System.out.println("Proceso finalizado. El PDF deberia abrirse automaticamente.");

                conexion.cerrarConexion();
            } else {
                System.err.println("No se pudo establecer la conexion a la base de datos.");
            }
        } catch (Exception e) {
            System.err.println("Error durante la generacion del informe:");
            e.printStackTrace();
        }
    }
}
