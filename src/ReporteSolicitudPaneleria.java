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
                + "ORDER BY lsm.referenciaproducto, lsm.codigo, dcs.codigo";

        ResultSet rsDet = conexion.funcionConsultar(sqlDetalle);
        String[][] detalle = ConexionDatos.armarArreglo(rsDet);

        escribirPdf(codigoSolicitud, h, detalle, true);
    }

    public void generarVistaPrevia(String codigoSolicitud) throws Exception {
        String[] cabecera = {
                "PANELERIA", codigoSolicitud, "22236", "RCC", "FRIOGAN", "VILLAVICENCIO",
                "2026-06-19", "2026-06-30", "UNDA HERRERA DANIEL", "Bodega principal Villavicencio",
                "-", "-", "9"
        };

        String[][] detalle = {
                { "45", "1-18-42", "PANEL FRIGOWALL 80 MM 9002 C-28 PUR", "MT", "MT2", "92", "7", "8", "prueba16" },
                { "45", "1-18-42", "PANEL FRIGOWALL 80 MM 9002 C-28 PUR", "MT", "MT2", "92", "6", "6", "prueba17" },
                { "46", "1-18-42", "PANEL FRIGOWALL 80 MM 9002 C-28 PUR", "MT", "MT2", "82", "6", "5", "prueba13" },
                { "46", "1-18-42", "PANEL FRIGOWALL 80 MM 9002 C-28 PUR", "MT", "MT2", "82", "7", "4", "prueba14" },
                { "46", "1-18-42", "PANEL FRIGOWALL 80 MM 9002 C-28 PUR", "MT", "MT2", "82", "8", "3", "prueba15" },
                { "44", "1-18-70", "PANEL MONOWALL 30 MM 9002 C-28 PIR C/MICRONERV", "MT", "MT2", "30", "5", "6", "prueba10" },
                { "43", "1-18-88", "PANEL SUPERWALL CLEAN 80 MM 9002 C-26 (FRP) LISA", "MT2", "MT2", "40", "10", "4", "prueba8" },
                { "41", "1-18-90", "PANEL TECHMET 30 MM 9002 C-28", "MT", "MT2", "127", "5", "8", "prueba1" },
                { "41", "1-18-90", "PANEL TECHMET 30 MM 9002 C-28", "MT", "MT2", "127", "4", "6", "prueba2" },
                { "41", "1-18-90", "PANEL TECHMET 30 MM 9002 C-28", "MT", "MT2", "127", "7", "9", "prueba3" },
                { "42", "1-18-90", "PANEL TECHMET 30 MM 9002 C-28", "MT", "MT2", "110", "4", "6", "prueba4" },
                { "42", "1-18-90", "PANEL TECHMET 30 MM 9002 C-28", "MT", "MT2", "110", "9", "2", "prueba5" },
                { "42", "1-18-90", "PANEL TECHMET 30 MM 9002 C-28", "MT", "MT2", "110", "4", "5", "prueba6" },
                { "42", "1-18-90", "PANEL TECHMET 30 MM 9002 C-28", "MT", "MT2", "110", "8", "6", "prueba7" }
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
        table.setWidths(new float[] { 17f, 15f, 38f, 20f, 10f });

        table.addCell(crearCeldaEtiqueta("TIPO DE SOLICITUD"));
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
                String refActual = datos[i][1];
                int groupEnd = i;
                double totalCantidadReferencia = 0;
                while (groupEnd < datos.length && datos[groupEnd][1].equals(refActual)) {
                    groupEnd++;
                }

                String lastLineCodigo = "";
                for (int k = i; k < groupEnd; k++) {
                    String lineCodigo = datos[k][0];
                    if (!lineCodigo.equals(lastLineCodigo)) {
                        totalCantidadReferencia += parseDouble(datos[k][5]);
                        lastLineCodigo = lineCodigo;
                    }
                }

                int k = i;
                while (k < groupEnd) {
                    String lineCodigoActual = datos[k][0];
                    int subGroupEnd = k;
                    while (subGroupEnd < groupEnd && datos[subGroupEnd][0].equals(lineCodigoActual)) {
                        subGroupEnd++;
                    }
                    int subGroupSize = subGroupEnd - k;
                    Color bg = (alterno % 2 == 0) ? null : GRIS_CLARO;

                    for (int idx = k; idx < subGroupEnd; idx++) {
                        String[] fila = datos[idx];
                        boolean isFirst = (idx == k);

                        if (isFirst) {
                            String[] lge = dividirReferencia(fila[1]);
                            
                            PdfPCell cellL = crearCelda(lge[0], fontMini, Element.ALIGN_CENTER, bg, 3f);
                            cellL.setRowspan(subGroupSize);
                            table.addCell(cellL);

                            PdfPCell cellG = crearCelda(lge[1], fontMini, Element.ALIGN_CENTER, bg, 3f);
                            cellG.setRowspan(subGroupSize);
                            table.addCell(cellG);

                            PdfPCell cellE = crearCelda(lge[2], fontMini, Element.ALIGN_CENTER, bg, 3f);
                            cellE.setRowspan(subGroupSize);
                            table.addCell(cellE);

                            PdfPCell cellDesc = crearCeldaDetalleConInterlineado(valor(fila[2]), fontMini, Element.ALIGN_LEFT, bg);
                            cellDesc.setRowspan(subGroupSize);
                            table.addCell(cellDesc);

                            PdfPCell cellUnd = crearCelda(valor(fila[3]), fontMini, Element.ALIGN_CENTER, bg, 3f);
                            cellUnd.setRowspan(subGroupSize);
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
                            cellMedidaStd.setRowspan(subGroupSize);
                            table.addCell(cellMedidaStd);

                            Color bgGrisOscuro = new Color(215, 215, 215);
                            PdfPCell cellCantTotal = crearCelda(formatearValorOpcional(fila[5]), fontMini, Element.ALIGN_RIGHT, bgGrisOscuro, 3f);
                            cellCantTotal.setRowspan(subGroupSize);
                            table.addCell(cellCantTotal);
                        }
                    }

                    alterno++;
                    k = subGroupEnd;
                }

                Color bgTotal = new Color(230, 230, 230);
                PdfPCell cellTotalLabel = crearCelda("TOTAL REFERENCIA " + refActual, fontNegrita, Element.ALIGN_RIGHT, bgTotal, 4f, 10);
                table.addCell(cellTotalLabel);

                Color bgTotalCant = new Color(195, 195, 195);
                PdfPCell cellTotalValue = crearCelda(formateadorNumero.format(totalCantidadReferencia), fontNegrita, Element.ALIGN_RIGHT, bgTotalCant, 4f);
                table.addCell(cellTotalValue);

                i = groupEnd;
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
                if (!archivoMarca.exists()) {
                    archivoMarca = new File("../images/imagenMarca2.png");
                }
                if (!archivoMarca.exists()) {
                    archivoMarca = new File("C:\\Users\\amejoramiento1\\Desktop\\INFORMES SOLID\\images\\imagenMarca2.png");
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
