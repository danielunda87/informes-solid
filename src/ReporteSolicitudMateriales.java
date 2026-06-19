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
 * Generador del informe PDF FT-ALM-18 — Planilla de Impresión de Materiales.
 * Produce una planilla de entrega de materiales con 3 filas por cada material.
 */
public class ReporteSolicitudMateriales extends BaseInforme {

    private static final String CODIGO_FORMATO = "FT-ALM-18";
    private static final String FECHA_FORMATO = "dic-24";
    private static final String VERSION_FORMATO = "02";

    private ConexionDatos conexion;
    private String dirArchivo;
    private java.awt.Image imageLogo = null;
    private DecimalFormat formateadorNumero;

    public ReporteSolicitudMateriales(ConexionDatos inConexion) {
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

        String sqlCabecera = "SELECT tipo, codigosolicitud, ordentrabajo, uen, cliente, ubicacion, "
                + "to_char(fecha + hora, 'DD/MM/YYYY HH24:MI') AS fecha_solicitud_form, fechanecesidad, "
                + "nombreusuario, observaciones, to_char(fecharadicacion, 'DD/MM/YYYY HH24:MI') AS fecha_radicacion_form, codigo "
                + "FROM solicitudmaterial WHERE codigo::text = '" + codigoEsc + "'";

        ResultSet rsCab = conexion.funcionConsultar(sqlCabecera);
        String[][] cabecera = ConexionDatos.armarArreglo(rsCab);

        if (cabecera == null || cabecera[0][0].equals("0")) {
            throw new Exception("No se encontró la solicitud: " + codigoSolicitud);
        }

        String[] h = cabecera[0];
        String codigoInterno = h[11];

        // Trae detalle del material con su ubicación física desde catalogoproducto
        String sqlDetalle = "SELECT "
                + "lsm.referenciaproducto, "
                + "lsm.descripcionproducto, "
                + "lsm.unidadmedida, "
                + "lsm.cantidad, "
                + "lsm.tipodestino, "
                + "lsm.observaciones, "
                + "lsm.codigo, "
                + "cp.ubicacionalmacen AS ubicacion_fisica "
                + "FROM lineasolicitudmaterial lsm "
                + "LEFT JOIN catalogoproducto cp ON cp.referencia = lsm.referenciaproducto "
                + "WHERE lsm.solicitudmaterial = '" + escaparSql(codigoInterno) + "' "
                + "ORDER BY ubicacion_fisica, lsm.referenciaproducto, lsm.codigo";

        ResultSet rsDet = conexion.funcionConsultar(sqlDetalle);
        String[][] detalle = ConexionDatos.armarArreglo(rsDet);

        escribirPdf(codigoSolicitud, h, detalle, true);
    }

    public void generarVistaPrevia(String codigoSolicitud) throws Exception {
        String[] cabecera = {
                "MATERIAL", codigoSolicitud, "22096", "RCC", "ALIMENTOS CARNICOS", "BOGOTA",
                "19/06/2026 13:48", "2026-06-19", "HARRY OSORIO LENIS", "OBSERVACION DE MATERIALES EN PRUEBA", "19/06/2026 14:15", "12"
        };

        String[][] detalle = {
                { "7-4-47", "ENFRIADOR INTERCAL EIC GL-6T504/ 307-AC(AL)", "UND", "5", "1 - Entregar a planta", "Revisar antes de entrega", "47", "" }
        };

        escribirPdf(codigoSolicitud, cabecera, detalle, false);
    }

    void escribirPdf(String codigoSolicitud, String[] h, String[][] detalle, boolean abrirAutomaticamente)
            throws Exception {
        File carpetaTemp = new File("temp");
        if (!carpetaTemp.exists()) {
            carpetaTemp.mkdirs();
        }

        dirArchivo = "temp" + File.separator + "Planilla de Impresion Materiales_" + codigoSolicitud + "_"
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
        documento.add(crearTituloSeccion("INFORMACIÓN GENERAL Y DE ENTREGA"));
        documento.add(crearFilaInformacionGeneralEntrega(h));
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

        PdfPCell cellTitulo = crearCelda("FORMATO DE ENTREGA DE MATERIALES EN PLANTA Y OBRA", fontTitulo, Element.ALIGN_CENTER, null, 8);
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
        table.addCell(crearCeldaValorCompacto("MATERIAL"));

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

    private PdfPTable crearFilaInformacionGeneralEntrega(String[] h) throws Exception {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 20f, 30f, 20f, 30f });

        // Fila 1
        table.addCell(crearCeldaEtiqueta("UEN:"));
        table.addCell(crearCeldaValor(valor(h[3])));
        table.addCell(crearCeldaEtiqueta("SOLICITADO POR:"));
        table.addCell(crearCeldaValor(valor(h[8])));

        // Fila 2
        table.addCell(crearCeldaEtiqueta("OT / OS:"));
        table.addCell(crearCeldaValor(valor(h[2])));
        table.addCell(crearCeldaEtiqueta("FECHA DE SOLICITUD:"));
        table.addCell(crearCeldaValor(valor(h[6])));

        // Fila 3
        table.addCell(crearCeldaEtiqueta("CLIENTE:"));
        table.addCell(crearCeldaValor(formatearCliente(h[4], h[5])));
        table.addCell(crearCeldaEtiqueta("FECHA TENTATIVA ENTREGA:"));
        table.addCell(crearCeldaValor(formatearFecha(h[7])));

        // Fila 4
        table.addCell(crearCeldaEtiqueta("MATERIALES DE:"));
        table.addCell(crearCeldaValor(valor(h[0])));
        table.addCell(crearCeldaEtiqueta("FECHA DE RADICACIÓN:"));
        table.addCell(crearCeldaValor(valor(h[10])));

        // Fila 5: Observaciones (100% de la línea)
        table.addCell(crearCeldaEtiqueta("OBSERVACIONES:"));
        table.addCell(crearCelda(valor(h[9]), fontNormal, Element.ALIGN_LEFT, null, 3f, 3));

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
                "L", "G", "E", "DESCRIPCIÓN", "UND", "CANT", "TIPO ENTREGA", "OBSERVACIONES", "# ENTREGA",
                "SALIDA ALMACEN", "REVISION CONFIR.", "FIRMA ALMACEN", "FIRMA PLANTA", "FECHA RECIBIDO", "UBICACIÓN"
        };

        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        // Anchos de columna sumando 100%
        table.setWidths(new float[] { 2.5f, 2.5f, 2.5f, 19f, 3f, 3.5f, 8.5f, 14f, 3.5f, 6.8f, 6.8f, 6.8f, 6.8f, 6.8f, 7f });

        for (String header : headers) {
            table.addCell(crearCeldaEncabezadoTabla(header));
        }

        if (datos != null && !datos[0][0].equals("0")) {
            int alterno = 0;
            for (String[] fila : datos) {
                Color bg = (alterno % 2 == 0) ? null : GRIS_CLARO;

                String[] lge = dividirReferencia(fila[0]);
                String descripcion = valor(fila[1]);
                String und = valor(fila[2]);
                String cant = formatearValorOpcional(fila[3]);
                String tipoEntrega = valor(fila[4]);
                String observacionesLote = valor(fila[5]);
                String ubicacionFisica = valor(fila[7]); // columna ficticia

                // --- FILA 1 ---
                // Celdas principales con rowspan = 3
                PdfPCell cellL = crearCelda(lge[0], fontMini, Element.ALIGN_CENTER, bg, 3f);
                cellL.setRowspan(3);
                table.addCell(cellL);

                PdfPCell cellG = crearCelda(lge[1], fontMini, Element.ALIGN_CENTER, bg, 3f);
                cellG.setRowspan(3);
                table.addCell(cellG);

                PdfPCell cellE = crearCelda(lge[2], fontMini, Element.ALIGN_CENTER, bg, 3f);
                cellE.setRowspan(3);
                table.addCell(cellE);

                PdfPCell cellDesc = crearCeldaDetalleConInterlineado(descripcion, fontMini, Element.ALIGN_LEFT, bg);
                cellDesc.setRowspan(3);
                table.addCell(cellDesc);

                PdfPCell cellUnd = crearCelda(und, fontMini, Element.ALIGN_CENTER, bg, 3f);
                cellUnd.setRowspan(3);
                table.addCell(cellUnd);

                PdfPCell cellCant = crearCelda(cant, fontMini, Element.ALIGN_RIGHT, bg, 3f);
                cellCant.setRowspan(3);
                table.addCell(cellCant);

                PdfPCell cellTipoDest = crearCelda(tipoEntrega, fontMini, Element.ALIGN_LEFT, bg, 3f);
                cellTipoDest.setRowspan(3);
                table.addCell(cellTipoDest);

                PdfPCell cellObs = crearCeldaDetalleConInterlineado(observacionesLote, fontMini, Element.ALIGN_LEFT, bg);
                cellObs.setRowspan(3);
                table.addCell(cellObs);

                // Celda de # Entrega y firmas individuales
                table.addCell(crearCelda("1", fontMini, Element.ALIGN_CENTER, bg, 3f));
                table.addCell(crearCelda(" ", fontMini, Element.ALIGN_CENTER, bg, 3f)); // Salida almacén
                table.addCell(crearCelda(" ", fontMini, Element.ALIGN_CENTER, bg, 3f)); // Revisión confirmada
                table.addCell(crearCelda(" ", fontMini, Element.ALIGN_CENTER, bg, 3f)); // Firma almacén
                table.addCell(crearCelda(" ", fontMini, Element.ALIGN_CENTER, bg, 3f)); // Firma planta
                table.addCell(crearCelda(" ", fontMini, Element.ALIGN_CENTER, bg, 3f)); // Fecha recibido

                // Ubicación con rowspan = 3
                PdfPCell cellUbic = crearCelda(ubicacionFisica, fontMini, Element.ALIGN_CENTER, bg, 3f);
                cellUbic.setRowspan(3);
                table.addCell(cellUbic);

                // --- FILA 2 ---
                table.addCell(crearCelda("2", fontMini, Element.ALIGN_CENTER, bg, 3f));
                table.addCell(crearCelda(" ", fontMini, Element.ALIGN_CENTER, bg, 3f)); // Salida almacén
                table.addCell(crearCelda(" ", fontMini, Element.ALIGN_CENTER, bg, 3f)); // Revisión confirmada
                table.addCell(crearCelda(" ", fontMini, Element.ALIGN_CENTER, bg, 3f)); // Firma almacén
                table.addCell(crearCelda(" ", fontMini, Element.ALIGN_CENTER, bg, 3f)); // Firma planta
                table.addCell(crearCelda(" ", fontMini, Element.ALIGN_CENTER, bg, 3f)); // Fecha recibido

                // --- FILA 3 ---
                table.addCell(crearCelda("3", fontMini, Element.ALIGN_CENTER, bg, 3f));
                table.addCell(crearCelda(" ", fontMini, Element.ALIGN_CENTER, bg, 3f)); // Salida almacén
                table.addCell(crearCelda(" ", fontMini, Element.ALIGN_CENTER, bg, 3f)); // Revisión confirmada
                table.addCell(crearCelda(" ", fontMini, Element.ALIGN_CENTER, bg, 3f)); // Firma almacén
                table.addCell(crearCelda(" ", fontMini, Element.ALIGN_CENTER, bg, 3f)); // Firma planta
                table.addCell(crearCelda(" ", fontMini, Element.ALIGN_CENTER, bg, 3f)); // Fecha recibido

                alterno++;
            }

            // Espacio vacío para colocación de sellos debajo de la última fila con datos
            // Equivale al espacio de unas 6 líneas del detalle (2 materiales completos)
            PdfPCell cellSellos = new PdfPCell(new Phrase("ESPACIO PARA CONTROL Y SELLOS", FontFactory.getFont("Helvetica", 8, Font.BOLD, new Color(180, 180, 180))));
            cellSellos.setColspan(headers.length);
            cellSellos.setFixedHeight(85f);
            cellSellos.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellSellos.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cellSellos.setBorder(PdfPCell.BOX);
            cellSellos.setBorderColor(GRIS_BORDE);
            cellSellos.setBackgroundColor(null);
            table.addCell(cellSellos);
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
        private PdfTemplate totalPagesTemplate;
        private BaseFont helv;
        private Image imgMarca = null;

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            try {
                totalPagesTemplate = writer.getDirectContent().createTemplate(30, 12);
                helv = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);

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
                int paginaActual = writer.getPageNumber();
                PdfContentByte canvasUnder = writer.getDirectContentUnder();

                // 1. Marca de agua "PAGINA X" en el centro de la página (diagonal)
                Font fontMarca = FontFactory.getFont("Helvetica", 90, Font.BOLD, new Color(245, 245, 245));
                Phrase PhraseMarca = new Phrase("PAGINA " + paginaActual, fontMarca);
                ColumnText.showTextAligned(
                        canvasUnder,
                        Element.ALIGN_CENTER,
                        PhraseMarca,
                        (document.right() + document.left()) / 2,
                        (document.top() + document.bottom()) / 2,
                        30);

                // 2. Pie de página
                PdfContentByte canvas = writer.getDirectContent();
                PdfPTable footerTable = new PdfPTable(3);
                footerTable.setWidthPercentage(100);
                footerTable.setTotalWidth(document.right() - document.left());
                footerTable.setWidths(new float[] { 30, 40, 30 });

                // Celda izquierda: vacía
                PdfPCell cellLeft = new PdfPCell(new Phrase(""));
                cellLeft.setBorder(PdfPCell.NO_BORDER);
                footerTable.addCell(cellLeft);

                // Celda central: Página X de Y
                Phrase PhrasePagina = new Phrase("Página " + paginaActual + " de ", fontNormal);
                if (totalPagesTemplate != null) {
                    Image img = Image.getInstance(totalPagesTemplate);
                    PhrasePagina.add(new Chunk(img, 0, 0));
                }
                PdfPCell cellCenter = new PdfPCell(PhrasePagina);
                cellCenter.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellCenter.setVerticalAlignment(Element.ALIGN_BOTTOM);
                cellCenter.setBorder(PdfPCell.NO_BORDER);
                footerTable.addCell(cellCenter);

                // Celda derecha: logotipo de Solid-ERP
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

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            if (totalPagesTemplate != null) {
                try {
                    totalPagesTemplate.beginText();
                    totalPagesTemplate.setFontAndSize(helv, fontNormal.getSize());
                    totalPagesTemplate.setTextMatrix(0, 1);
                    totalPagesTemplate.showText(String.valueOf(writer.getPageNumber() - 1));
                    totalPagesTemplate.endText();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }
}
