package net.warp_scores.warpscores.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.export.naf.NafReport;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.TimeZone;

@Service
@Slf4j
public class NafXmlCreator {

    public String writeAsXml(NafReport nafReport) {
        try {
            XmlMapper xmlMapper = new XmlMapper();
            xmlMapper.enable(ToXmlGenerator.Feature.WRITE_XML_DECLARATION);
            xmlMapper.setLocale(Locale.GERMANY);
            xmlMapper.setTimeZone(TimeZone.getDefault());
            return xmlMapper.writeValueAsString(nafReport);
        } catch (JsonProcessingException e) {
            log.error("Error writing NafReport {} as XML.", nafReport, e);
            return null;
        }
    }
}
