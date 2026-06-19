import java.io.File;
import java.io.FileOutputStream;
import java.awt.Color;
import java.awt.Toolkit;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

/**
 * Generador del informe PDF FT-ALM-32 — Solicitud de Dotación.
 * Produce una sola página con la planilla de entrega de dotaciones.
 */
public class ReporteSolicitudDotacion extends BaseInforme {

    private static final String CODIGO_FORMATO = "FT-ALM-32";
    private static final String FECHA_FORMATO = "mar-26";
    private static final String VERSION_FORMATO = "4";

    private ConexionDatos conexion;
    private String dirArchivo;
    private java.awt.Image imageLogo = null;
    private DecimalFormat formateadorNumero;

    public ReporteSolicitudDotacion(ConexionDatos inConexion) {
        super();
        this.conexion = inConexion;
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
        simbolos.setDecimalSeparator(',');
        simbolos.setGroupingSeparator('.');
        formateadorNumero = new DecimalFormat("#,##0.##", simbolos);
        cargarLogo();
    }

    private void cargarLogo() {
        try {
            String pathLocal = "logoBarra1.png";
            File archivoLogo = new File(pathLocal);
            if (archivoLogo.exists()) {
                imageLogo = Toolkit.getDefaultToolkit().getImage(archivoLogo.getAbsolutePath());
            } else if (conexion != null) {
                Imagen tmpImagen = new Imagen(null, conexion);
                tmpImagen.cargar("codigo", "3");
                imageLogo = ImageConverter.convertToImage(tmpImagen.representacion);
            }
        } catch (Exception e) {
            // Logo opcional
        }
    }

    @Override
    public void generar(String codigoSolicitud) throws Exception {
        String codigoEsc = escaparSql(codigoSolicitud);

        String sqlCabecera = "SELECT tipo, codigosolicitud, fecha, fechanecesidad, nombreusuario, "
                + "quienrecibe, numerocontacto, ciudadentrega, direccionentrega, observaciones, codigo "
                + "FROM solicitudmaterial WHERE codigo::text = '" + codigoEsc + "'";

        ResultSet rsCab = conexion.funcionConsultar(sqlCabecera);
        String[][] cabecera = ConexionDatos.armarArreglo(rsCab);

        if (cabecera == null || cabecera[0][0].equals("0")) {
            throw new Exception("No se encontró la solicitud: " + codigoSolicitud);
        }

        String[] h = cabecera[0];
        String codigoInterno = h[10];

        String sqlDetalle = "SELECT "
                + "e.nombre AS colaborador_nombre, "
                + "lsm.referenciaproducto, "
                + "lsm.descripcionproducto, "
                + "lsm.unidadmedida, "
                + "lsm.cantidad, "
                + "lsm.tipodestino, "
                + "lsm.observaciones, "
                + "lsm.nrocedula "
                + "FROM lineasolicitudmaterial lsm "
                + "LEFT JOIN establecimiento e ON e.nrocedula = lsm.nrocedula "
                + "WHERE lsm.solicitudmaterial = '" + escaparSql(codigoInterno) + "' "
                + "ORDER BY lsm.codigo";

        ResultSet rsDet = conexion.funcionConsultar(sqlDetalle);
        String[][] detalle = ConexionDatos.armarArreglo(rsDet);

        escribirPdf(codigoSolicitud, h, detalle, true);
    }

    public void generarVistaPrevia(String codigoSolicitud) throws Exception {
        String[] cabecera = {
                "DOTACION", codigoSolicitud, "2026-06-18", "2026-06-26", "HARRY OSORIO LENIS",
                "DANIEL UNDA", "3154887766", "BOGOTA", "Calle 80 Bodega Central", "Entrega programada primer semestre",
                "7"
        };

        String[][] detalle = {
                { "UNDA HERRERA DANIEL", "11-25-8", "CAMISA DRILL M/L T/S", "UND", "2", "2 - Enviar a obra",
                        "Con logo reflectivo", "1127620314" },
                { "UNDA HERRERA DANIEL", "11-25-9", "JEAN DRILL T/32", "UND", "2", "2 - Enviar a obra",
                        "Reforzado en rodillas", "1127620314" },
                { "OSORIO LENIS HARRY", "11-25-10", "BOTAS DE SEGURIDAD T/40", "PAR", "1", "1 - Entregar a planta", "",
                        "1127620315" }
        };

        escribirPdf(codigoSolicitud, cabecera, detalle, false);
    }

    void escribirPdf(String codigoSolicitud, String[] h, String[][] detalle, boolean abrirAutomaticamente)
            throws Exception {
        File carpetaTemp = new File("temp");
        if (!carpetaTemp.exists()) {
            carpetaTemp.mkdirs();
        }

        dirArchivo = "temp" + File.separator + "Planilla de Impresion Dotacion_" + codigoSolicitud + "_"
                + System.currentTimeMillis() + ".pdf";

        Document documento = new Document(PageSize.LETTER, 30, 30, 26, 36);
        PdfWriter writer = PdfWriter.getInstance(documento, new FileOutputStream(dirArchivo));
        MarcaAguaPagina eventHelper = new MarcaAguaPagina();
        writer.setPageEvent(eventHelper);
        documento.open();

        agregarPagina(documento, h, detalle);

        documento.close();
        System.out.println("PDF generado: " + dirArchivo);
        if (abrirAutomaticamente) {
            abrirArchivo();
        }
    }

    private void agregarPagina(Document documento, String[] h, String[][] detalle) throws Exception {
        documento.add(crearEncabezadoPrincipal());
        documento.add(new Paragraph("\n", fontMini));
        documento.add(crearFilaFormatoRadicado(h));
        documento.add(new Paragraph("\n", fontMini));
        documento.add(crearTituloSeccion("INFORMACIÓN DE ENTREGA"));
        documento.add(crearFilaInformacionEntrega(h));
        documento.add(new Paragraph("\n", fontMini));
        documento.add(crearTablaDetalle(detalle));
    }

    private PdfPTable crearEncabezadoPrincipal() throws Exception {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 12, 64, 24 });

        PdfPCell cellLogo;
        if (imageLogo != null) {
            Image img = Image.getInstance(imageLogo, null);
            img.scaleToFit(65, 38);
            cellLogo = new PdfPCell(img, false);
            cellLogo.setPadding(3);
            cellLogo.setFixedHeight(img.getScaledHeight() + 6);
        } else {
            cellLogo = new PdfPCell();
            cellLogo.setFixedHeight(80f);
        }
        cellLogo.setBorder(PdfPCell.BOX);
        cellLogo.setBorderColor(GRIS_BORDE);
        cellLogo.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cellLogo);

        PdfPCell cellTitulo = crearCelda("SOLICITUD DE DOTACIÓN", fontTitulo, Element.ALIGN_CENTER, null, 8);
        cellTitulo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cellTitulo);

        Font fontMetaEtiqueta = FontFactory.getFont("Helvetica", 7, Font.BOLD, Color.BLACK);
        Font fontMetaValor = FontFactory.getFont("Helvetica", 7, Font.NORMAL, Color.BLACK);

        PdfPTable meta = new PdfPTable(2);
        meta.setWidthPercentage(100);
        meta.addCell(crearCelda("CÓDIGO", fontMetaEtiqueta, Element.ALIGN_CENTER, GRIS_CLARO, 1));
        meta.addCell(crearCelda(CODIGO_FORMATO, fontMetaValor, Element.ALIGN_CENTER, null, 1));
        meta.addCell(crearCelda("FECHA", fontMetaEtiqueta, Element.ALIGN_CENTER, GRIS_CLARO, 1));
        meta.addCell(crearCelda(FECHA_FORMATO, fontMetaValor, Element.ALIGN_CENTER, null, 1));
        meta.addCell(crearCelda("VERSIÓN", fontMetaEtiqueta, Element.ALIGN_CENTER, GRIS_CLARO, 1));
        meta.addCell(crearCelda(VERSION_FORMATO, fontMetaValor, Element.ALIGN_CENTER, null, 1));

        PdfPCell cellMeta = new PdfPCell(meta);
        cellMeta.setPadding(1);
        cellMeta.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellMeta.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cellMeta);

        return table;
    }

    private PdfPTable crearFilaFormatoRadicado(String[] h) throws Exception {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.setWidths(new float[] { 22f, 20f, 15f, 25f, 18f });

        table.addCell(crearCeldaEtiqueta("TIPO DE SOLICITUD"));
        table.addCell(crearCeldaValorCompacto("DOTACION"));

        PdfPCell espacio = new PdfPCell(new Phrase(""));
        espacio.setBorder(PdfPCell.NO_BORDER);
        table.addCell(espacio);

        table.addCell(crearCeldaEtiqueta("RADICADO"));

        PdfPCell celdaRadicado = crearCeldaValorCompacto(valor(h[1]));
        celdaRadicado.setPhrase(new Phrase(valor(h[1]), fontNegrita));
        table.addCell(celdaRadicado);

        return table;
    }

    private PdfPTable crearTituloSeccion(String titulo) throws Exception {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.addCell(crearCelda(titulo, fontNegritaBlancas, Element.ALIGN_CENTER, AZUL_NAVY, 5));
        return table;
    }

    private PdfPTable crearFilaInformacionEntrega(String[] h) throws Exception {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 22f, 28f, 26f, 24f });

        // Row 1
        table.addCell(crearCeldaEtiqueta("FECHA DE SOLICITUD:"));
        table.addCell(crearCeldaValor(formatearFecha(h[2])));
        table.addCell(crearCeldaEtiqueta("FECHA TENTATIVA DE ENTREGA:"));
        table.addCell(crearCeldaValor(formatearFecha(h[3])));

        // Row 2
        table.addCell(crearCeldaEtiqueta("SOLICITADO POR:"));
        table.addCell(crearCeldaValor(valor(h[4])));
        table.addCell(crearCeldaEtiqueta("TIPO DE ENTREGA:"));
        table.addCell(crearCeldaValor("-"));

        // Row 3
        table.addCell(crearCeldaEtiqueta("NUMERO CONTACTO:"));
        table.addCell(crearCeldaValor(valor(h[6])));
        table.addCell(crearCeldaEtiqueta("QUIEN RECIBE:"));
        table.addCell(crearCeldaValor(valor(h[5])));

        // Row 4
        table.addCell(crearCeldaEtiqueta("DIRECCION DE ENTREGA:"));
        table.addCell(crearCeldaValor(valor(h[8])));
        table.addCell(crearCeldaEtiqueta("CIUDAD DE ENTREGA:"));
        table.addCell(crearCeldaValor(valor(h[7])));

        // Row 5
        table.addCell(crearCeldaEtiqueta("OBSERVACIONES:"));
        PdfPCell cellObs = crearCelda(valor(h[9]), fontNormal, Element.ALIGN_LEFT, null, 3f);
        cellObs.setColspan(3);
        table.addCell(cellObs);

        return table;
    }

    private PdfPCell crearCeldaValor(String texto) throws Exception {
        return crearCelda(texto, fontNormal, Element.ALIGN_LEFT, null, 3f);
    }

    private PdfPCell crearCeldaValorCompacto(String texto) throws Exception {
        PdfPCell celda = crearCelda(texto, fontNormal, Element.ALIGN_CENTER, null, 2);
        celda.setNoWrap(true);
        return celda;
    }

    private PdfPCell crearCeldaEtiqueta(String texto) throws Exception {
        PdfPCell celda = crearCelda(texto, fontNegrita, Element.ALIGN_LEFT, GRIS_CLARO, 3);
        celda.setNoWrap(true);
        return celda;
    }

    private PdfPCell crearCeldaEncabezadoTabla(String texto) throws Exception {
        PdfPCell celda = crearCelda(texto, fontNegritaBlancas, Element.ALIGN_CENTER, AZUL_NAVY, 2);
        celda.setMinimumHeight(22f);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return celda;
    }

    private PdfPTable crearTablaDetalle(String[][] datos) throws Exception {
        String[] headers = {
                "NOMBRE", "REFERENCIA", "DESCRIPCION", "UND", "CANTIDAD", "TIPO DESTINO", "OBSERVACIONES",
                "SALIDA ALMACEN", "FIRMA ALMACEN"
        };

        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 18f, 11f, 15f, 5f, 9f, 12f, 14f, 8f, 8f });

        for (String header : headers) {
            table.addCell(crearCeldaEncabezadoTabla(header));
        }

        if (datos != null && !datos[0][0].equals("0")) {
            int alterno = 0;
            for (String[] fila : datos) {
                Color bg = (alterno % 2 == 0) ? null : GRIS_CLARO;

                // Resolve colaborador name (first index). If null or empty, display the cedula
                // (last index)
                String nombreColaborador = fila[0];
                if (nombreColaborador == null || nombreColaborador.trim().isEmpty()) {
                    nombreColaborador = valor(fila[7]);
                }

                table.addCell(crearCelda(nombreColaborador, fontMini, Element.ALIGN_LEFT, bg, 3f));
                table.addCell(crearCelda(valor(fila[1]), fontMini, Element.ALIGN_CENTER, bg, 3f));
                table.addCell(crearCeldaDetalleConInterlineado(valor(fila[2]), fontMini, Element.ALIGN_LEFT, bg));
                table.addCell(crearCelda(valor(fila[3]), fontMini, Element.ALIGN_CENTER, bg, 3f));
                table.addCell(crearCelda(formatearValorOpcional(fila[4]), fontMini, Element.ALIGN_RIGHT, bg, 3f));
                table.addCell(crearCelda(valor(fila[5]), fontMini, Element.ALIGN_LEFT, bg, 3f));
                table.addCell(crearCelda(valor(fila[6]), fontMini, Element.ALIGN_LEFT, bg, 3f));

                // Columnas de salida y firma de almacén vacías para llenar a mano
                table.addCell(crearCelda(" ", fontMini, Element.ALIGN_CENTER, bg, 3f));
                table.addCell(crearCelda(" ", fontMini, Element.ALIGN_CENTER, bg, 3f));

                alterno++;
            }
        }

        return table;
    }

    private PdfPCell crearCeldaDetalleConInterlineado(String texto, Font fuente, int alineacion, Color bg)
            throws Exception {
        PdfPCell celda = crearCelda(texto, fuente, alineacion, bg, 2f);
        celda.setLeading(0f, 1.40f);
        celda.setPaddingTop(3f);
        celda.setPaddingBottom(3f);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return celda;
    }

    private String formatearFecha(String fecha) {
        if (fecha == null || fecha.trim().isEmpty()) {
            return "-";
        }
        try {
            if (fecha.length() >= 10) {
                String soloFecha = fecha.substring(0, 10);
                SimpleDateFormat entrada = new SimpleDateFormat("yyyy-MM-dd");
                Date d = entrada.parse(soloFecha);
                return new SimpleDateFormat("dd/MM/yyyy").format(d);
            }
        } catch (Exception e) {
            // ignore
        }
        return fecha;
    }

    private String formatearValorOpcional(String val) {
        if (val == null || val.trim().isEmpty()) {
            return "-";
        }
        try {
            double d = Double.parseDouble(val.trim());
            return formateadorNumero.format(d);
        } catch (Exception e) {
            return val;
        }
    }

    private double parseDouble(String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                return 0;
            }
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private String valor(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return "-";
        }
        return texto.trim();
    }

    private String escaparSql(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("'", "''");
    }

    private void abrirArchivo() {
        try {
            File file = new File(dirArchivo);
            if (file.exists()) {
                Runtime.getRuntime().exec("cmd /c \"" + dirArchivo + "\"");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class MarcaAguaPagina extends PdfPageEventHelper {
        private Image imgMarca = null;

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            try {
                File archivoMarca = new File("images/imagenMarca2.png");
                if (!archivoMarca.exists()) {
                    archivoMarca = new File("../images/imagenMarca2.png");
                }
                if (!archivoMarca.exists()) {
                    archivoMarca = new File(
                            "C:\\Users\\amejoramiento1\\Desktop\\INFORMES SOLID\\images\\imagenMarca2.png");
                }
                if (archivoMarca.exists()) {
                    imgMarca = Image.getInstance(archivoMarca.getAbsolutePath());
                    imgMarca.scaleToFit(65, 18);
                }
            } catch (Exception e) {
                // ignore
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                PdfContentByte canvas = writer.getDirectContent();
                PdfPTable footerTable = new PdfPTable(3);
                footerTable.setWidthPercentage(100);
                footerTable.setTotalWidth(document.right() - document.left());
                footerTable.setWidths(new float[] { 30, 40, 30 });

                PdfPCell cellLeft = new PdfPCell(new Phrase(""));
                cellLeft.setBorder(PdfPCell.NO_BORDER);
                footerTable.addCell(cellLeft);

                PdfPCell cellCenter = new PdfPCell(new Phrase(""));
                cellCenter.setBorder(PdfPCell.NO_BORDER);
                footerTable.addCell(cellCenter);

                PdfPCell cellRight;
                if (imgMarca != null) {
                    cellRight = new PdfPCell(imgMarca, false);
                } else {
                    Font fontSolid = FontFactory.getFont("Helvetica", 9, Font.BOLD | Font.ITALIC,
                            new Color(180, 180, 180));
                    cellRight = new PdfPCell(new Phrase("Solid-ERP", fontSolid));
                }
                cellRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
                cellRight.setVerticalAlignment(Element.ALIGN_BOTTOM);
                cellRight.setBorder(PdfPCell.NO_BORDER);
                footerTable.addCell(cellRight);

                footerTable.writeSelectedRows(0, -1, document.left(), 22, canvas);
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
