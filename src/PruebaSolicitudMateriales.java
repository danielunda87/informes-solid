public class PruebaSolicitudMateriales {
    public static void main(String[] args) {
        try {
            ConexionDatos conexion = new ConexionDatos("solidpruebas3");
            if (conexion.estaConectado()) {
                String codigoSolicitud = "12";
                if (args.length > 0) {
                    codigoSolicitud = args[0];
                }
                System.out.println("Generando reporte de materiales para código solicitud: " + codigoSolicitud);
                ReporteSolicitudMateriales reporte = new ReporteSolicitudMateriales(conexion);
                reporte.generar(codigoSolicitud);
                conexion.cerrarConexion();
                System.out.println("Proceso finalizado con éxito.");
            } else {
                System.err.println("No se pudo conectar a la base de datos.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
