package com.ibm.es.ama.plugin.sample;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.ibm.es.ama.plugin.CrawlerPlugin;
import com.ibm.es.ama.plugin.CrawlerPluginConfiguration;
import com.ibm.es.ama.plugin.CrawlerPluginContent;
import com.ibm.es.ama.plugin.CrawlerPluginDocument;
import com.ibm.es.ama.plugin.CrawlerPluginException;

public class SampleCrawlerPlugin implements CrawlerPlugin {

    // use java.util.logging.Logger instance to output log messages
    private Logger logger = Logger.getLogger(SampleCrawlerPlugin.class.getName());

    private String crawlerName;

    @Override
    public void init(CrawlerPluginConfiguration configuration) throws CrawlerPluginException {
        Map<String, Object> generalSettings = configuration.getGeneralSettings();
        this.crawlerName = (String) generalSettings.get("crawler_name");
    }

    @Override
    public void updateDocument(CrawlerPluginDocument document) throws CrawlerPluginException {
        String crawlUrl = document.getCrawlUrl();
        //logger.info(String.format("{crawlerName: %s, crawlUrl: %s}", this.crawlerName, crawlUrl));

        if (document.getCrawlUrl().contains("confidential")) {
            document.setExclude(true);
            document.getNoticeMessages().add(String.format("The document %s is excluded by the crawler plugin.", crawlUrl));
            return;
        }

        Map<String, Object> fields = document.getFields();

        Object roleField = fields.get("__$role$__");
        if (roleField != null) {
            fields.put("__$xxrole$__", roleField.toString().trim()); // Update role
            fields.put("__$role$__", roleField.toString().trim());  // Retaining original role column
        }

        Object businessGroupField = fields.get("__$businessgroup$__");
        if (businessGroupField != null) {
            fields.put("__$xxbgroup$__", businessGroupField.toString().trim()); // Update businessgroup
            fields.put("__$businessgroup$__", businessGroupField.toString().trim());  // Retaining original business group column
        }

        Object lastUpdatedDateField = fields.get("__$lastupdateddate$__");
        if (lastUpdatedDateField != null) {
            String lastModified = convertToEpoch(lastUpdatedDateField.toString().trim());
            fields.put("__$last-modified$__", lastModified); // Update last-modified field
        }

        Object dateField = fields.get("__$date$__");
        if (dateField != null) {
            String newDate = convertToNewFormat(dateField.toString().trim());
            fields.put("__$new-date$__", newDate); // Update new-date field
        }

        // Object fileNameField = fields.get("__$FileName$__");
        // if (fileNameField != null) {
        //     fields.put("__$FileName$__", fileNameField.toString().replace("ibm", "IBM"));
        // }

        CrawlerPluginContent content = document.getContent();
        if (isTextContent(fields, content)) {
            replaceContent(crawlUrl, content);
        } else if (isCSVContent(fields, content)) {
            document.setContent(null);
        } else if (content == null) {
            addNewContent(crawlUrl, document);
        }
    }

    private String convertToEpoch(String epochStr) {
        if (epochStr.isEmpty()) return "Invalid Date";
        try {
            long epoch = Long.parseLong(epochStr);
            LocalDateTime dateTime = LocalDateTime.ofInstant(java.time.Instant.ofEpochSecond(epoch), ZoneId.systemDefault());
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
            return dateTime.format(outputFormatter);
        } catch (NumberFormatException e) {
            return "Invalid Date";
        }
    }

    private String convertToNewFormat(String dateStr) {
        if (dateStr.isEmpty()) return "Invalid Date";

        String[] possibleFormats = {
            "dd MMM yyyy HH:mm:ss",
            "dd MMM yyyy HH:mm",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm"
        };

        for (String format : possibleFormats) {
            try {
                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(format);
                DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
                LocalDateTime dateTime = LocalDateTime.parse(dateStr, inputFormatter);
                return dateTime.format(outputFormatter);
            } catch (DateTimeParseException ignored) {}
        }

        return "Invalid Date";
    }


    private boolean isTextContent(Map<String, Object> fields, CrawlerPluginContent content) {
        return isContent(fields, content, "text/plain", ".txt");
    }

    private boolean isCSVContent(Map<String, Object> fields, CrawlerPluginContent content) {
        return isContent(fields, content, "text/csv", ".csv");
    }

    private boolean isContent(Map<String, Object> fields, CrawlerPluginContent content, String contentType, String extension) {
        if (content == null) return false;
        if (content.getContentType() != null && content.getContentType().equals(contentType)) return true;

        Object extensionField = fields.get("__$Extension$__");
        if (extensionField != null && extensionField.toString().equals(extension)) return true;

        return false;
    }

    private void replaceContent(String crawlUrl, CrawlerPluginContent content) throws CrawlerPluginException {
        Charset charset = content.getCharset();
        if (charset == null) {
            charset = StandardCharsets.UTF_8;
        }

        try (
            InputStream inputStream = content.getInputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(content.getOutputStream()))) {

            LineIterator lines = IOUtils.lineIterator(inputStream, charset);
            while (lines.hasNext()) {
                String line = lines.next().replaceAll("IBM", "International Business Machines");
                writer.println(line);
            }

            content.setCharset(StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new CrawlerPluginException(String.format("The document %s cannot be updated by the crawler plugin.", crawlUrl), e);
        }
    }

    private void addNewContent(String crawlUrl, CrawlerPluginDocument document) throws CrawlerPluginException {
        Map<String, Object> fields = document.getFields();
        if (!fields.containsKey("__$ContentURL$__")) return;

        String contentUrl = (String) fields.get("__$ContentURL$__");
        CrawlerPluginContent pluginContent = document.newContent();

        try (CloseableHttpClient httpclient = HttpClients.createDefault();
             CloseableHttpResponse response = httpclient.execute(new HttpGet(contentUrl))) {

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                try (InputStream inputStream = entity.getContent();
                     OutputStream outputStream = pluginContent.getOutputStream()) {

                    IOUtils.copy(inputStream, outputStream);
                }

                pluginContent.setContentType(entity.getContentType() != null ? entity.getContentType().getValue() : null);
                pluginContent.setCharset(entity.getContentEncoding() != null ? Charset.forName(entity.getContentEncoding().getValue()) : null);
            }

        } catch (IOException e) {
            throw new CrawlerPluginException(String.format("The document %s cannot be updated by the crawler plugin.", crawlUrl), e);
        }

        document.setContent(pluginContent);
    }
    
    @Override
    public void term() throws CrawlerPluginException {
        // Terminate the plugin (if any necessary cleanup is needed)
    }
}
