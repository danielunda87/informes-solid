public class VistaPreviaSolicitudMateriales {
    public static void main(String[] args) {
        try {
            System.out.println("Generando vista previa offline de la planilla de materiales...");
            ReporteSolicitudMateriales reporte = new ReporteSolicitudMateriales(null);
            reporte.generarVistaPrevia("12");
            System.out.println("Proceso finalizado con éxito.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
