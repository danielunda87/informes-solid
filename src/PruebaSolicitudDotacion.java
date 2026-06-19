/**
 * Punto de entrada para probar el informe FT-ALM-32 - Solicitud de Dotación.
 */
public class PruebaSolicitudDotacion {
    public static void main(String[] args) {
        try {
            ConexionDatos conexion = new ConexionDatos("solidpruebas3");

            if (conexion.estaConectado()) {
                String codigo = "7";
                if (args.length > 0) {
                    codigo = args[0];
                }

                ReporteSolicitudDotacion reporte = new ReporteSolicitudDotacion(conexion);

                System.out.println("Generando solicitud de dotación para el código de solicitud: " + codigo);
                reporte.generar(codigo);
                System.out.println("Proceso finalizado. El PDF debería abrirse automáticamente.");

                conexion.cerrarConexion();
            } else {
                System.err.println("No se pudo establecer la conexión a la base de datos.");
            }
        } catch (Exception e) {
            System.err.println("Error durante la generación del informe de dotación:");
            e.printStackTrace();
        }
    }
}
