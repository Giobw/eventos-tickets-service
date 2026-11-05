package cl.duoc.eventos.tickets.service;

import cl.duoc.eventos.tickets.exception.StorageException;
import cl.duoc.eventos.tickets.model.Ticket;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/** Genera el PDF personalizado de un ticket. */
@Component
public class TicketPdfGenerator {

    private static final Color PRIMARY = new Color(0x1F, 0x3A, 0x5F);
    private static final Color LIGHT = new Color(0xEE, 0xF2, 0xF7);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public byte[] generate(Ticket t) {
        try {
            Document doc = new Document(PageSize.A5.rotate(), 36, 36, 36, 36);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont = new Font(Font.HELVETICA, 22, Font.BOLD, Color.WHITE);
            Font subtitleFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.WHITE);
            Font labelFont = new Font(Font.HELVETICA, 10, Font.BOLD, PRIMARY);
            Font valueFont = new Font(Font.HELVETICA, 12, Font.NORMAL, Color.BLACK);
            Font footerFont = new Font(Font.HELVETICA, 8, Font.ITALIC, Color.GRAY);

            // Encabezado
            PdfPTable header = new PdfPTable(1);
            header.setWidthPercentage(100);
            PdfPCell headerCell = new PdfPCell();
            headerCell.setBackgroundColor(PRIMARY);
            headerCell.setBorder(Rectangle.NO_BORDER);
            headerCell.setPadding(14);
            headerCell.addElement(new Paragraph(safe(t.getEventName()), titleFont));
            headerCell.addElement(new Paragraph("TICKET DE ENTRADA  |  " + t.getStatus(), subtitleFont));
            header.addCell(headerCell);
            doc.add(header);
            doc.add(new Paragraph(" "));

            // Detalle
            PdfPTable detail = new PdfPTable(new float[]{1.2f, 3f});
            detail.setWidthPercentage(100);
            addRow(detail, "Código", t.getCode(), labelFont, valueFont);
            addRow(detail, "Evento (ID)", t.getEventId(), labelFont, valueFont);
            addRow(detail, "Asistente", t.getUserName() + " (" + t.getUserId() + ")", labelFont, valueFont);
            addRow(detail, "Asiento", t.getSeat() == null || t.getSeat().isBlank() ? "General" : t.getSeat(),
                    labelFont, valueFont);
            addRow(detail, "Precio", "$ " + t.getPrice().toPlainString(), labelFont, valueFont);
            addRow(detail, "Emitido", t.getCreatedAt() == null ? "-" : t.getCreatedAt().format(DATE_FMT),
                    labelFont, valueFont);
            doc.add(detail);

            Paragraph footer = new Paragraph(
                    "Presente este ticket en el acceso del evento. Documento generado automáticamente.", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(18);
            doc.add(footer);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new StorageException("No se pudo generar el PDF del ticket", e);
        }
    }

    private void addRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell l = new PdfPCell(new Phrase(label, labelFont));
        l.setBackgroundColor(LIGHT);
        l.setBorder(Rectangle.BOTTOM);
        l.setBorderColor(Color.WHITE);
        l.setPadding(7);
        PdfPCell v = new PdfPCell(new Phrase(safe(value), valueFont));
        v.setBorder(Rectangle.BOTTOM);
        v.setBorderColor(LIGHT);
        v.setPadding(7);
        table.addCell(l);
        table.addCell(v);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
