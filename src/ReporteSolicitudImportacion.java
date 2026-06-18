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
 * Generador del informe PDF FT-CE-01 — Solicitud de Importación.
 * Produce dos páginas idénticas: ORIGINAL y COPIA.
 */
public class ReporteSolicitudImportacion extends BaseInforme {

    private static final String CODIGO_FORMATO = "FT-CE-01";
    private static final String FECHA_FORMATO = "dic-2024";
    private static final String VERSION_FORMATO = "14";

    public enum FormatoImpresion {
        CLASICO("clasico"),
        COMPACTO("compacto"),
        HORIZONTAL("horizontal");

        private final String codigo;

        FormatoImpresion(String codigo) {
            this.codigo = codigo;
        }

        public String getCodigo() {
            return codigo;
        }

        public static FormatoImpresion desdeTexto(String texto) {
            if (texto == null || texto.trim().isEmpty()) {
                return CLASICO;
            }
            String normalizado = texto.trim().toLowerCase();
            for (FormatoImpresion formato : values()) {
                if (formato.codigo.equals(normalizado) || formato.name().equalsIgnoreCase(normalizado)) {
                    return formato;
                }
            }
            throw new IllegalArgumentException(
                    "Formato no valido: " + texto + ". Use: clasico, compacto u horizontal.");
        }
    }

    private ConexionDatos conexion;
    private String dirArchivo;
    private java.awt.Image imageLogo = null;
    private DecimalFormat formateadorNumero;
    private DecimalFormat formateadorDecimal;
    private FormatoImpresion formatoImpresion;

    public ReporteSolicitudImportacion(ConexionDatos inConexion) {
        this(inConexion, FormatoImpresion.CLASICO);
    }

    public ReporteSolicitudImportacion(ConexionDatos inConexion, FormatoImpresion formatoImpresion) {
        super();
        this.conexion = inConexion;
        this.formatoImpresion = formatoImpresion == null ? FormatoImpresion.CLASICO : formatoImpresion;
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
        simbolos.setDecimalSeparator(',');
        simbolos.setGroupingSeparator('.');
        formateadorNumero = new DecimalFormat("#,##0.##", simbolos);
        formateadorDecimal = new DecimalFormat("#,##0.###", simbolos);
        ajustarFuentesPorFormato();
        cargarLogo();
    }

    private void ajustarFuentesPorFormato() {
        if (formatoImpresion == FormatoImpresion.COMPACTO) {
            fontTitulo = FontFactory.getFont("Helvetica", 11, Font.BOLD, AZUL_NAVY);
            fontSubtitulo = FontFactory.getFont("Helvetica", 9, Font.BOLD, AZUL_NAVY);
            fontNegritaBlancas = FontFactory.getFont("Helvetica", 7, Font.BOLD, Color.WHITE);
            fontNormal = FontFactory.getFont("Helvetica", 7, Font.NORMAL, Color.BLACK);
            fontNegrita = FontFactory.getFont("Helvetica", 7, Font.BOLD, Color.BLACK);
            fontMini = FontFactory.getFont("Helvetica", 6, Font.NORMAL, Color.BLACK);
        }
    }

    private void cargarLogo() {
        try {
            String pathLocal = "logoBarra1.png";
            File archivoLogo = new File(pathLocal);
            if (archivoLogo.exists()) {
                imageLogo = Toolkit.getDefaultToolkit().getImage(archivoLogo.getAbsolutePath());
            } else {
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
                + "fecha, fechanecesidad, nombreusuario, direccionentrega, proveedor, transporte, "
                + "factorimportacion, tipoimportacion, factoreurousd, trm, codigo "
                + "FROM solicitudmaterial WHERE codigo::text = '" + codigoEsc + "'";

        ResultSet rsCab = conexion.funcionConsultar(sqlCabecera);
        String[][] cabecera = ConexionDatos.armarArreglo(rsCab);

        if (cabecera == null || cabecera[0][0].equals("0")) {
            throw new Exception("No se encontró la solicitud: " + codigoSolicitud);
        }

        String[] h = cabecera[0];
        String codigoInterno = h[16];

        String sqlDetalle = "SELECT "
                + "CASE WHEN b IS NULL OR TRIM(b) = '' THEN '' ELSE 'B' || b END AS esp, "
                + "subindice, centrocosto, subcentrocosto, referenciaproducto, "
                + "descripcionproducto, unidadmedida, cantidad, costoproducto, largo, ancho, alto, peso, observaciones "
                + "FROM lineasolicitudmaterial WHERE solicitudmaterial = '" + escaparSql(codigoInterno) + "' "
                + "ORDER BY subindice";

        ResultSet rsDet = conexion.funcionConsultar(sqlDetalle);
        String[][] detalle = ConexionDatos.armarArreglo(rsDet);

        escribirPdf(codigoSolicitud, h, detalle, true);
    }

    public void generarVistaPrevia(String codigoSolicitud) throws Exception {
        String[] cabecera = {
                "IMPORTACION", codigoSolicitud, "OT-24018", "REFRIDCOL", "CLIENTE DEMO", "BOGOTA",
                "2026-06-18", "2026-07-05", "USUARIO DE PRUEBA", "Bodega principal - Calle 80",
                "Proveedor internacional demo", "MARITIMO", "1,35", "IMPORTACION DIRECTA", "1,08", "4.120", "0"
        };

        String[][] detalle = {
                { "B1", "1", "5101", "02", "L-G-E", "Compresor hermetico para unidad de refrigeracion industrial",
                        "UND", "2", "1850000", "40", "35", "30", "18", "Revisar ficha tecnica antes de compra" },
                { "B1", "2", "5101", "02", "L-G-E", "Kit de valvulas, accesorios y conexiones en cobre",
                        "KIT", "5", "320000", "25", "20", "15", "6", "Incluye empaque individual" },
                { "B2", "3", "5102", "01", "L-G-E", "Tarjeta electronica de control con sensores de temperatura",
                        "UND", "3", "950000", "20", "15", "10", "2", "Producto sensible, manipular con proteccion" }
        };

        escribirPdf(codigoSolicitud, cabecera, detalle, false);
    }

    void escribirPdf(String codigoSolicitud, String[] h, String[][] detalle, boolean abrirAutomaticamente)
            throws Exception {
        File carpetaTemp = new File("temp");
        if (!carpetaTemp.exists()) {
            carpetaTemp.mkdirs();
        }

        String sufijoFormato = formatoImpresion == FormatoImpresion.CLASICO ? "" : "_" + formatoImpresion.getCodigo();
        dirArchivo = "temp" + File.separator + "Solicitud_Importacion_" + codigoSolicitud + sufijoFormato + "_"
                + System.currentTimeMillis() + ".pdf";

        Document documento = new Document(obtenerTamanoPagina(), obtenerMargen(), obtenerMargen(), 26, 36);
        PdfWriter writer = PdfWriter.getInstance(documento, new FileOutputStream(dirArchivo));
        MarcaAguaPagina eventHelper = new MarcaAguaPagina();
        writer.setPageEvent(eventHelper);
        documento.open();

        eventHelper.setSeccion("ORIGINAL");
        agregarPagina(documento, h, detalle);
        documento.newPage();
        eventHelper.setSeccion("COPIA");
        agregarPagina(documento, h, detalle);

        documento.close();
        System.out.println("PDF generado: " + dirArchivo);
        if (abrirAutomaticamente) {
            abrirArchivo();
        }
    }

    private Rectangle obtenerTamanoPagina() {
        if (formatoImpresion == FormatoImpresion.HORIZONTAL) {
            return PageSize.LETTER.rotate();
        }
        return PageSize.LETTER;
    }

    private float obtenerMargen() {
        if (formatoImpresion == FormatoImpresion.COMPACTO) {
            return 22f;
        }
        if (formatoImpresion == FormatoImpresion.HORIZONTAL) {
            return 24f;
        }
        return 30f;
    }

    private void agregarEspacio(Document documento) throws Exception {
        if (formatoImpresion != FormatoImpresion.COMPACTO) {
            documento.add(new Paragraph("\n", fontMini));
        }
    }

    private void agregarPagina(Document documento, String[] h, String[][] detalle) throws Exception {
        documento.add(crearEncabezadoPrincipal());
        agregarEspacio(documento);
        documento.add(crearFilaFormatoRadicado(h));
        agregarEspacio(documento);
        documento.add(crearTituloSeccion("INFORMACIÓN GENERAL"));
        documento.add(crearFilaTresPares(
                "OT / OS", valor(h[2]),
                "UEN", valor(h[3]),
                "CLIENTE", formatearCliente(h[4], h[5])));
        agregarEspacio(documento);
        documento.add(crearTituloSeccion("INFORMACIÓN DE ENTREGA"));
        documento.add(crearFilaDosPares(
                "FECHA DE SOLICITUD", formatearFecha(h[6]),
                "FECHA TENTATIVA DE ENTREGA", formatearFecha(h[7])));
        documento.add(crearFilaDosPares(
                "SOLICITADO POR", valor(h[8]),
                "DIRECCION DE ENTREGA", valor(h[9])));
        documento.add(crearFilaDosPares(
                "PROVEEDOR", valor(h[10]),
                "TRANSPORTE", valor(h[11])));
        agregarEspacio(documento);
        documento.add(crearTituloSeccion("INFORMACIÓN DE IMPORTACIÓN"));
        documento.add(crearTablaImportacion(h));
        agregarEspacio(documento);
        documento.add(crearTablaDetalle(detalle));
        agregarEspacio(documento);
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

        PdfPCell cellTitulo = crearCelda("SOLICITUD DE IMPORTACIÓN", fontTitulo, Element.ALIGN_CENTER, null, 8);
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
        table.addCell(crearCeldaValorCompacto(valor(h[0])));

        // Espacio visual entre IMPORTACION y NUMERO DE RADICADO
        PdfPCell espacio = new PdfPCell(new Phrase(""));
        espacio.setBorder(PdfPCell.NO_BORDER);
        table.addCell(espacio);

        table.addCell(crearCeldaEtiqueta("NUMERO DE RADICADO"));

        PdfPCell celdaRadicado = crearCeldaValorCompacto(valor(h[1]));
        celdaRadicado.setPhrase(new Phrase(valor(h[1]), fontNegrita));
        table.addCell(celdaRadicado);

        return table;
    }

    private PdfPTable crearTablaImportacion(String[] h) throws Exception {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 30, 10, 30, 30 });

        agregarParCampo(table, "FACTOR DE IMPORTACION (PPTO)", formatearNumero(h[12]), true);
        agregarParCampo(table, "TIPO DE IMPORTACIÓN", valor(h[13]), true);
        agregarParCampo(table, "EUR - USD", formatearNumero(h[14]), true);
        agregarParCampo(table, "TRM", formatearNumero(h[15]), true);

        return table;
    }

    private PdfPTable crearTituloSeccion(String titulo) throws Exception {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.addCell(crearCelda(titulo, fontNegritaBlancas, Element.ALIGN_CENTER, AZUL_NAVY, 5));
        return table;
    }

    private PdfPTable crearFilaTresPares(String l1, String v1, String l2, String v2, String l3, String v3)
            throws Exception {
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 11, 8, 9, 17, 11, 44 });

        agregarParCampo(table, l1, v1);
        agregarParCampo(table, l2, v2);
        agregarParCampo(table, l3, v3);

        return table;
    }

    private PdfPTable crearFilaDosPares(String l1, String v1, String l2, String v2) throws Exception {
        return crearFilaDosPares(l1, v1, l2, v2, new float[] { 20, 26, 28, 26 });
    }

    private PdfPTable crearFilaDosPares(String l1, String v1, String l2, String v2, float[] anchos)
            throws Exception {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(anchos);

        agregarParCampo(table, l1, v1, anchos[1] <= 12);
        agregarParCampo(table, l2, v2, anchos[3] <= 12);

        return table;
    }

    private PdfPTable crearFilaCuatroPares(String l1, String v1, String l2, String v2, String l3, String v3,
            String l4, String v4) throws Exception {
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 18, 7, 14, 9, 10, 9, 8, 25 });

        agregarParCampo(table, l1, v1);
        agregarParCampo(table, l2, v2);
        agregarParCampo(table, l3, v3);
        agregarParCampo(table, l4, v4);

        return table;
    }

    private void agregarParCampo(PdfPTable table, String etiqueta, String valor) throws Exception {
        agregarParCampo(table, etiqueta, valor, false);
    }

    private void agregarParCampo(PdfPTable table, String etiqueta, String valor, boolean valorCompacto)
            throws Exception {
        table.addCell(crearCeldaEtiqueta(etiqueta));
        if (valorCompacto) {
            table.addCell(crearCeldaValorCompacto(valor));
        } else {
            table.addCell(crearCelda(valor, fontNormal, Element.ALIGN_LEFT, null, 3));
        }
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
                "ESP", "SUB", "CC", "SUBC", "L", "G", "E", "DESCRIPCION", "UND",
                "CANTIDAD\nTOTAL", "COSTO\nPRODUCTO", "MEDIDAS\n(CM)", "PESO\n(KG)", "OBSERVACIONES"
        };

        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setWidths(obtenerAnchosDetalle());

        for (String header : headers) {
            table.addCell(crearCeldaEncabezadoTabla(header));
        }

        if (datos != null && !datos[0][0].equals("0")) {
            int idx = 0;
            for (int i = 0; i < datos.length; i++) {
                if (!filaTieneDatos(datos[i])) {
                    continue;
                }
                agregarFilaDetalle(table, datos[i], idx % 2 == 0 ? null : GRIS_CLARO);
                idx++;
            }
        }

        return table;
    }

    private float[] obtenerAnchosDetalle() {
        if (formatoImpresion == FormatoImpresion.HORIZONTAL) {
            return new float[] { 3, 3, 3, 4, 2.5f, 2.5f, 2.5f, 23, 3.5f, 7, 8, 7, 5, 20 };
        }
        if (formatoImpresion == FormatoImpresion.COMPACTO) {
            return new float[] { 2.8f, 2.8f, 3, 3.6f, 2.3f, 2.3f, 2.3f, 16, 3.2f, 6.5f, 7.5f, 5.5f, 4.8f, 11.4f };
        }
        return new float[] { 3, 3, 3, 4, 2.5f, 2.5f, 2.5f, 15, 3.5f, 7, 8, 6, 5, 12.5F };
    }

    private boolean filaTieneDatos(String[] fila) {
        if (fila == null) {
            return false;
        }
        return !esVacio(fila[4]) || !esVacio(fila[5]) || !esVacio(fila[7]) || !esVacio(fila[0]);
    }

    private boolean esVacio(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return true;
        }
        return "-".equals(texto.trim());
    }

    private PdfPCell crearCeldaDetalleConInterlineado(String texto, Font fuente, int alineacion, Color bg)
            throws Exception {

        PdfPCell celda = crearCelda(texto, fuente, alineacion, bg, 2);

        // Aumenta el interlineado cuando el texto ocupa varias líneas
        celda.setLeading(0f, 1.40f);

        // Aumenta el espacio interno superior e inferior
        celda.setPaddingTop(3f);
        celda.setPaddingBottom(3f);

        // Ubicación vertical del texto dentro del cuadro
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);

        return celda;
    }

    private void agregarFilaDetalle(PdfPTable table, String[] fila, Color bg) throws Exception {
        String[] lge = dividirReferencia(fila[4]);

        table.addCell(crearCelda(valor(fila[0]), fontMini, Element.ALIGN_CENTER, bg, 1));
        table.addCell(crearCelda(valor(fila[1]), fontMini, Element.ALIGN_CENTER, bg, 1));
        table.addCell(crearCelda(valor(fila[2]), fontMini, Element.ALIGN_CENTER, bg, 1));
        table.addCell(crearCelda(valor(fila[3]), fontMini, Element.ALIGN_CENTER, bg, 1));
        table.addCell(crearCelda(lge[0], fontMini, Element.ALIGN_CENTER, bg, 1));
        table.addCell(crearCelda(lge[1], fontMini, Element.ALIGN_CENTER, bg, 1));
        table.addCell(crearCelda(lge[2], fontMini, Element.ALIGN_CENTER, bg, 1));
        table.addCell(crearCeldaDetalleConInterlineado(valor(fila[5]), fontMini, Element.ALIGN_LEFT, bg));
        table.addCell(crearCeldaDetalleConInterlineado(valor(fila[6]), fontMini, Element.ALIGN_CENTER, bg));
        table.addCell(crearCeldaDetalleConInterlineado(formatearNumero(fila[7]), fontMini, Element.ALIGN_RIGHT, bg));
        table.addCell(crearCeldaDetalleConInterlineado(formatearMoneda(fila[8]), fontMini, Element.ALIGN_RIGHT, bg));
        table.addCell(crearCeldaDetalleConInterlineado(calcularMedidas(fila[9], fila[10], fila[11]), fontMini, Element.ALIGN_RIGHT, bg));
        table.addCell(crearCeldaDetalleConInterlineado(formatearNumero(fila[12]), fontMini, Element.ALIGN_RIGHT, bg));
        table.addCell(crearCeldaDetalleConInterlineado(valor(fila[13]), fontMini, Element.ALIGN_LEFT, bg));
    }

    private PdfPTable crearSeccionFirmas() throws Exception {
        String[] roles = {
                "SOLICITANTE",
                "CONTROL PRESUPUESTAL",
                "GERENTE DE PRODUCCIÓN",
                "COMERCIO EXTERIOR"
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
            celda.setFixedHeight(formatoImpresion == FormatoImpresion.COMPACTO ? 58f : 75f);

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
        return new String[] {
                partes.length > 0 ? partes[0] : "",
                partes.length > 1 ? partes[1] : "",
                partes.length > 2 ? partes[2] : ""
        };
    }

    private String calcularMedidas(String largo, String ancho, String alto) {
        try {
            double l = parseNumero(largo);
            double a = parseNumero(ancho);
            double h = parseNumero(alto);
            return formateadorDecimal.format(l * a * h);
        } catch (Exception e) {
            return "-";
        }
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
        return c + "-" + u;
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
            // Usar valor original si no se puede parsear
        }
        return fecha;
    }

    private String formatearMoneda(String valor) {
        try {
            return formateadorMoneda.format(parseNumero(valor));
        } catch (Exception e) {
            return valor(valor);
        }
    }

    private String formatearNumero(String valor) {
        try {
            return formateadorNumero.format(parseNumero(valor));
        } catch (Exception e) {
            return valor(valor);
        }
    }

    private double parseNumero(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return 0;
        }
        String v = valor.trim();
        if (v.contains(",")) {
            v = v.replace(".", "").replace(",", ".");
        }
        return Double.parseDouble(v);
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

    /**
     * Marca de agua "Página N" al fondo de cada hoja y pie de página con "ORIGINAL/COPIA" y "hoja X de Y".
     */
    private class MarcaAguaPagina extends PdfPageEventHelper {
        private String seccion = "ORIGINAL";
        private int paginaOriginal = 0;
        private int paginaCopia = 0;

        private PdfTemplate totalPagesTemplate;
        private BaseFont helv;

        public void setSeccion(String seccion) {
            this.seccion = seccion;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            try {
                totalPagesTemplate = writer.getDirectContent().createTemplate(30, 12);
                helv = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            } catch (Exception e) {
                // ignore
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                int paginaActualSeccion = 0;
                if ("ORIGINAL".equals(seccion)) {
                    paginaOriginal++;
                    paginaActualSeccion = paginaOriginal;
                } else {
                    paginaCopia++;
                    paginaActualSeccion = paginaCopia;
                }

                // 1. Marca de agua en el centro de la página (en el fondo, muy tenue y grande)
                PdfContentByte canvasUnder = writer.getDirectContentUnder();
                Font fontMarca = FontFactory.getFont("Helvetica", 130, Font.BOLD, new Color(242, 242, 242));
                Phrase marca = new Phrase(seccion, fontMarca);

                ColumnText.showTextAligned(
                        canvasUnder,
                        Element.ALIGN_CENTER,
                        marca,
                        (document.right() + document.left()) / 2,
                        (document.top() + document.bottom()) / 2,
                        30); // Rotación diagonal de 30 grados

                // 2. Pie de página
                PdfContentByte canvas = writer.getDirectContent();
                PdfPTable footerTable = new PdfPTable(3);
                footerTable.setWidthPercentage(100);
                footerTable.setTotalWidth(document.right() - document.left());
                footerTable.setWidths(new float[] { 30, 40, 30 });

                // Celda izquierda: "Página X de Y"
                Phrase p = new Phrase("Página " + paginaActualSeccion + " de ", fontNormal);
                if (totalPagesTemplate != null) {
                    Image img = Image.getInstance(totalPagesTemplate);
                    p.add(new Chunk(img, 0, 0));
                }
                PdfPCell cellLeft = new PdfPCell(p);
                cellLeft.setHorizontalAlignment(Element.ALIGN_LEFT);
                cellLeft.setVerticalAlignment(Element.ALIGN_BOTTOM);
                cellLeft.setBorder(PdfPCell.NO_BORDER);
                footerTable.addCell(cellLeft);

                // Celda central: Etiqueta ORIGINAL / COPIA
                Font fontEtiqueta = FontFactory.getFont("Helvetica", 11, Font.BOLD, Color.BLACK);
                String texto = seccion;
                if (formatoImpresion != FormatoImpresion.CLASICO) {
                    texto += " - FORMATO " + formatoImpresion.getCodigo().toUpperCase();
                }
                PdfPCell cellCenter = new PdfPCell(new Phrase(texto, fontEtiqueta));
                cellCenter.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellCenter.setVerticalAlignment(Element.ALIGN_BOTTOM);
                cellCenter.setBorder(PdfPCell.NO_BORDER);
                footerTable.addCell(cellCenter);

                // Celda derecha: "Solid-ERP" (Marca de agua sutil abajo a la derecha)
                Font fontSolid = FontFactory.getFont("Helvetica", 9, Font.BOLD | Font.ITALIC, new Color(180, 180, 180));
                PdfPCell cellRight = new PdfPCell(new Phrase("Solid-ERP", fontSolid));
                cellRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
                cellRight.setVerticalAlignment(Element.ALIGN_BOTTOM);
                cellRight.setBorder(PdfPCell.NO_BORDER);
                footerTable.addCell(cellRight);

                footerTable.writeSelectedRows(0, -1, document.left(), 22, canvas);

            } catch (Exception e) {
                // Pie de página opcional
            }
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            if (totalPagesTemplate != null) {
                totalPagesTemplate.beginText();
                totalPagesTemplate.setFontAndSize(helv, fontNormal.getSize());
                totalPagesTemplate.setTextMatrix(0, 1);
                totalPagesTemplate.showText(String.valueOf(paginaOriginal));
                totalPagesTemplate.endText();
            }
        }
    }
}
