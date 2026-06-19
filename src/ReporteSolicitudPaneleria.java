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
 * Generador del informe PDF FT-ALM-35 — Solicitud de Panelería.
 * Produce una sola página con el detalle completo de paneles y su uso.
 */
public class ReporteSolicitudPaneleria extends BaseInforme {

    private static final String CODIGO_FORMATO = "FT-ALM-35";
    private static final String FECHA_FORMATO = "jul-2025";
    private static final String VERSION_FORMATO = "04";

    private ConexionDatos conexion;
    private String dirArchivo;
    private java.awt.Image imageLogo = null;
    private DecimalFormat formateadorNumero;
    private DecimalFormat formateadorDecimal;

    public ReporteSolicitudPaneleria(ConexionDatos inConexion) {
        super();
        this.conexion = inConexion;
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
        simbolos.setDecimalSeparator(',');
        simbolos.setGroupingSeparator('.');
        formateadorNumero = new DecimalFormat("#,##0.##", simbolos);
        formateadorDecimal = new DecimalFormat("#,##0.###", simbolos);
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

        String sqlCabecera = "SELECT tipo, codigosolicitud, ordentrabajo, uen, cliente, ubicacion, "
                + "fecha, fechanecesidad, nombreusuario, direccionentrega, proveedor, transporte, codigo "
                + "FROM solicitudmaterial WHERE codigo::text = '" + codigoEsc + "'";

        ResultSet rsCab = conexion.funcionConsultar(sqlCabecera);
        String[][] cabecera = ConexionDatos.armarArreglo(rsCab);

        if (cabecera == null || cabecera[0][0].equals("0")) {
            throw new Exception("No se encontró la solicitud: " + codigoSolicitud);
        }

        String[] h = cabecera[0];
        String codigoInterno = h[12];

        String sqlDetalle = "SELECT "
                + "lsm.codigo AS linea_codigo, "
                + "lsm.referenciaproducto, "
                + "lsm.descripcionproducto, "
                + "lsm.unidadmedida, "
                + "lsm.unidadmedidastd, "
                + "lsm.cantidad AS cantidad_total, "
                + "dcs.cantidadund, "
                + "dcs.cantidadmts, "
                + "dcs.uso "
                + "FROM lineasolicitudmaterial lsm "
                + "LEFT JOIN detallecantidadlineasolicitudpaneles dcs ON dcs.lineasolicitudmaterial = lsm.codigo "
                + "WHERE lsm.solicitudmaterial = '" + escaparSql(codigoInterno) + "' "
                + "ORDER BY lsm.codigo, dcs.codigo";

        ResultSet rsDet = conexion.funcionConsultar(sqlDetalle);
        String[][] detalle = ConexionDatos.armarArreglo(rsDet);

        escribirPdf(codigoSolicitud, h, detalle, true);
    }

    public void generarVistaPrevia(String codigoSolicitud) throws Exception {
        String[] cabecera = {
                "PANELERIA", codigoSolicitud, "22226", "RCE", "INDUSTRIAS REFRIDCOL", "YUMBO",
                "2026-06-19", "2026-06-27", "UNDA HERRERA DANIEL", "Bodega principal Yumbo",
                "-", "-", "8"
        };

        String[][] detalle = {
                { "37", "1-18-70", "PANEL MONOWALL 30 MM 9002 C-28 PIR C/MICRONERV", "MT", "MT2", "150", "3", "4", "PARED IZQ" },
                { "37", "1-18-70", "PANEL MONOWALL 30 MM 9002 C-28 PIR C/MICRONERV", "MT", "MT2", "150", "9", "7", "PISO" },
                { "37", "1-18-70", "PANEL MONOWALL 30 MM 9002 C-28 PIR C/MICRONERV", "MT", "MT2", "150", "4", "3", "PARED DER" },
                { "37", "1-18-70", "PANEL MONOWALL 30 MM 9002 C-28 PIR C/MICRONERV", "MT", "MT2", "150", "7", "9", "TECHO" },
                { "38", "1-18-44", "PANEL MONOWALL 50 MM 9002 C-28 PUR", "MT2", "MT2", "41", "4", "4", "PARED" },
                { "38", "1-18-44", "PANEL MONOWALL 50 MM 9002 C-28 PUR", "MT2", "MT2", "41", "5", "5", "PISO" },
                { "39", "1-18-265", "PANEL FRIGOWALL 125 MM 9002 C-24 C/MICRONERV PUR", "MT", "MT2", "93", "7", "3", "TECHO" },
                { "39", "1-18-265", "PANEL FRIGOWALL 125 MM 9002 C-24 C/MICRONERV PUR", "MT", "MT2", "93", "8", "9", "PARED" },
                { "40", "1-18-13", "PANEL TECHMET 80 MM 9002 C-28", "MT", "MT2", "253", "12", "3", "PRUEBA1" },
                { "40", "1-18-13", "PANEL TECHMET 80 MM 9002 C-28", "MT", "MT2", "253", "15", "7", "PRUEBA2" },
                { "40", "1-18-13", "PANEL TECHMET 80 MM 9002 C-28", "MT", "MT2", "253", "14", "8", "PRUEBA3" }
        };

        escribirPdf(codigoSolicitud, cabecera, detalle, false);
    }

    void escribirPdf(String codigoSolicitud, String[] h, String[][] detalle, boolean abrirAutomaticamente)
            throws Exception {
        File carpetaTemp = new File("temp");
        if (!carpetaTemp.exists()) {
            carpetaTemp.mkdirs();
        }

        dirArchivo = "temp" + File.separator + "Planilla de Impresion Paneleria_" + codigoSolicitud + "_"
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
        documento.add(crearTituloSeccion("INFORMACION GENERAL"));
        documento.add(crearFilaInformacionGeneral(h));
        documento.add(new Paragraph("\n", fontMini));
        documento.add(crearTablaDetalle(detalle));
        documento.add(new Paragraph("\n", fontMini));
        documento.add(crearSeccionFirmas());
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

        PdfPCell cellTitulo = crearCelda("SOLICITUD DE PANELERÍA", fontTitulo, Element.ALIGN_CENTER, null, 8);
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
        table.setWidths(new float[] { 10, 20, 40, 20, 10 });

        table.addCell(crearCeldaEtiqueta("FORMATO"));
        table.addCell(crearCeldaValorCompacto("PANELERIA"));

        PdfPCell espacio = new PdfPCell(new Phrase(""));
        espacio.setBorder(PdfPCell.NO_BORDER);
        table.addCell(espacio);

        table.addCell(crearCeldaEtiqueta("NUMERO DE RADICADO"));

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

    private PdfPTable crearFilaInformacionGeneral(String[] h) throws Exception {
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 10f, 15f, 10f, 15f, 10f, 40f });

        table.addCell(crearCeldaEtiqueta("OT / OS:"));
        table.addCell(crearCeldaValor(valor(h[2])));
        table.addCell(crearCeldaEtiqueta("UEN"));
        table.addCell(crearCeldaValor(valor(h[3])));
        table.addCell(crearCeldaEtiqueta("CLIENTE"));
        table.addCell(crearCeldaValor(formatearCliente(h[4], h[5])));

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
                "L", "G", "E", "DESCRIPCION", "UND", "Cantidad / Und.", "Cantidad / Mts.", "Uso", "MT2 Total", "Unidad Medida STD", "CANTIDAD TOTAL"
        };

        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 3f, 3f, 3f, 28f, 4f, 6f, 6f, 10f, 6f, 5f, 7f });

        for (String header : headers) {
            table.addCell(crearCeldaEncabezadoTabla(header));
        }

        if (datos != null && !datos[0][0].equals("0")) {
            int i = 0;
            int alterno = 0;
            while (i < datos.length) {
                int j = i;
                // agrupar por el ID de la linea de solicitud material (datos[j][0])
                while (j < datos.length && datos[j][0].equals(datos[i][0])) {
                    j++;
                }
                int groupSize = j - i;
                Color bg = (alterno % 2 == 0) ? null : GRIS_CLARO;

                for (int k = i; k < j; k++) {
                    String[] fila = datos[k];
                    boolean isFirst = (k == i);

                    if (isFirst) {
                        String[] lge = dividirReferencia(fila[1]);
                        
                        PdfPCell cellL = crearCelda(lge[0], fontMini, Element.ALIGN_CENTER, bg, 3f);
                        cellL.setRowspan(groupSize);
                        table.addCell(cellL);

                        PdfPCell cellG = crearCelda(lge[1], fontMini, Element.ALIGN_CENTER, bg, 3f);
                        cellG.setRowspan(groupSize);
                        table.addCell(cellG);

                        PdfPCell cellE = crearCelda(lge[2], fontMini, Element.ALIGN_CENTER, bg, 3f);
                        cellE.setRowspan(groupSize);
                        table.addCell(cellE);

                        PdfPCell cellDesc = crearCeldaDetalleConInterlineado(valor(fila[2]), fontMini, Element.ALIGN_LEFT, bg);
                        cellDesc.setRowspan(groupSize);
                        table.addCell(cellDesc);

                        PdfPCell cellUnd = crearCelda(valor(fila[3]), fontMini, Element.ALIGN_CENTER, bg, 3f);
                        cellUnd.setRowspan(groupSize);
                        table.addCell(cellUnd);
                    }

                    table.addCell(crearCelda(formatearValorOpcional(fila[6]), fontMini, Element.ALIGN_RIGHT, bg, 3f));
                    table.addCell(crearCelda(formatearValorOpcional(fila[7]), fontMini, Element.ALIGN_RIGHT, bg, 3f));
                    table.addCell(crearCelda(valor(fila[8]), fontMini, Element.ALIGN_LEFT, bg, 3f));

                    String cantUndRaw = fila[6];
                    String cantMtsRaw = fila[7];
                    String mt2TotalStr = "-";
                    if (cantUndRaw != null && !cantUndRaw.trim().isEmpty() && cantMtsRaw != null && !cantMtsRaw.trim().isEmpty()) {
                        double u = parseDouble(cantUndRaw);
                        double m = parseDouble(cantMtsRaw);
                        mt2TotalStr = formateadorNumero.format(u * m);
                    }
                    table.addCell(crearCelda(mt2TotalStr, fontMini, Element.ALIGN_RIGHT, bg, 3f));

                    if (isFirst) {
                        PdfPCell cellMedidaStd = crearCelda(valor(fila[4]), fontMini, Element.ALIGN_CENTER, bg, 3f);
                        cellMedidaStd.setRowspan(groupSize);
                        table.addCell(cellMedidaStd);

                        PdfPCell cellCantTotal = crearCelda(formatearValorOpcional(fila[5]), fontMini, Element.ALIGN_RIGHT, bg, 3f);
                        cellCantTotal.setRowspan(groupSize);
                        table.addCell(cellCantTotal);
                    }
                }
                
                alterno++;
                i = j;
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

    private PdfPTable crearSeccionFirmas() throws Exception {
        String[] roles = {
                "SOLICITANTE",
                "CONTROL PRESUPUESTAL",
                "GERENTE DE PRODUCCIÓN",
                "ALMACÉN Y LOGÍSTICA"
        };

        PdfPTable contenedor = new PdfPTable(1);
        contenedor.setWidthPercentage(100);

        PdfPTable firmas = new PdfPTable(4);
        firmas.setWidthPercentage(100);
        firmas.setWidths(new float[] { 25, 25, 25, 25 });

        for (String rol : roles) {
            PdfPCell celda = new PdfPCell();
            celda.setBorder(PdfPCell.BOX);
            celda.setBorderColor(GRIS_BORDE);
            celda.setPadding(4);
            celda.setFixedHeight(75f);

            PdfPTable bloque = new PdfPTable(1);
            bloque.setWidthPercentage(100);
            bloque.addCell(crearCeldaSinBorde(rol, fontNegrita, Element.ALIGN_CENTER));

            PdfPCell lineaFirma = new PdfPCell(new Phrase(" ", fontMini));
            lineaFirma.setBorder(PdfPCell.BOTTOM);
            lineaFirma.setBorderWidthBottom(1f);
            lineaFirma.setBorderColor(GRIS_BORDE);
            lineaFirma.setFixedHeight(22f);
            lineaFirma.setPaddingBottom(2);
            bloque.addCell(lineaFirma);

            bloque.addCell(crearCeldaSinBorde("FIRMA", fontMini, Element.ALIGN_CENTER));

            PdfPTable fecha = new PdfPTable(2);
            fecha.setWidthPercentage(100);
            fecha.addCell(crearCelda("FECHA", fontNegrita, Element.ALIGN_CENTER, GRIS_CLARO, 2));
            fecha.addCell(crearCelda(" ", fontNormal, Element.ALIGN_CENTER, null, 2));
            bloque.addCell(fecha);

            celda.addElement(bloque);
            firmas.addCell(celda);
        }

        contenedor.addCell(firmas);
        return contenedor;
    }

    private String[] dividirReferencia(String referencia) {
        String ref = valor(referencia);
        if (ref.isEmpty() || "-".equals(ref)) {
            return new String[] { "", "", "" };
        }
        String[] partes = ref.split("-");
        String l = partes.length > 0 ? partes[0] : "";
        String g = partes.length > 1 ? partes[1] : "";
        String e = partes.length > 2 ? partes[partes.length - 1] : "";
        return new String[] { l, g, e };
    }

    private String formatearCliente(String cliente, String ubicacion) {
        String c = valor(cliente);
        String u = valor(ubicacion);
        if (c.isEmpty() || "-".equals(c)) {
            return u.isEmpty() || "-".equals(u) ? "-" : u;
        }
        if (u.isEmpty() || "-".equals(u)) {
            return c;
        }
        return c + " - " + u;
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
                    Font fontSolid = FontFactory.getFont("Helvetica", 9, Font.BOLD | Font.ITALIC, new Color(180, 180, 180));
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
