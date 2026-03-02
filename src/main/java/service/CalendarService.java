package service;

import entities.Evenement;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class CalendarService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    public void exportEventAsIcs(Evenement event, Path filePath) throws IOException {
        Files.writeString(filePath, buildIcs(event), StandardCharsets.UTF_8);
    }

    public String buildIcs(Evenement event) {
        LocalDate start = event.getDateDebut();
        LocalDate endExclusive = event.getDateFin().plusDays(1);
        String nowUtc = java.time.ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
        String uid = UUID.randomUUID() + "@eventmanagement.local";

        String title = escape(event.getTitre());
        String desc = escape(event.getDescription());
        String location = escape(event.getLieu());

        return "BEGIN:VCALENDAR\r\n"
            + "VERSION:2.0\r\n"
            + "PRODID:-//EventManagementSystem//FR\r\n"
            + "CALSCALE:GREGORIAN\r\n"
            + "METHOD:PUBLISH\r\n"
            + "BEGIN:VEVENT\r\n"
            + "UID:" + uid + "\r\n"
            + "DTSTAMP:" + nowUtc + "\r\n"
            + "DTSTART;VALUE=DATE:" + DATE_FMT.format(start) + "\r\n"
            + "DTEND;VALUE=DATE:" + DATE_FMT.format(endExclusive) + "\r\n"
            + "SUMMARY:" + title + "\r\n"
            + "DESCRIPTION:" + desc + "\r\n"
            + "LOCATION:" + location + "\r\n"
            + "STATUS:CONFIRMED\r\n"
            + "END:VEVENT\r\n"
            + "END:VCALENDAR\r\n";
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n");
    }
}
