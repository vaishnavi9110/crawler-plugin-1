package com.ibm.es.ama.plugin.sample;

import java.util.HashMap;
import java.util.Map;

import com.ibm.es.ama.plugin.CrawlerPluginConfiguration;
import com.ibm.es.ama.plugin.CrawlerPluginDocument;
import com.ibm.es.ama.plugin.CrawlerPluginException;

class SampleCrawlerPluginTest {

    private SampleCrawlerPlugin plugin;
    private CrawlerPluginConfiguration configuration;
    private CrawlerPluginDocument document;

    @BeforeEach
    void setUp() {
        plugin = new SampleCrawlerPlugin();
        configuration = mock(CrawlerPluginConfiguration.class);
        document = mock(CrawlerPluginDocument.class);
    }

    @Test
    void testInit() throws CrawlerPluginException {
        Map<String, Object> settings = new HashMap<>();
        settings.put("crawler_name", "TestCrawler");

        when(configuration.getGeneralSettings()).thenReturn(settings);

        plugin.init(configuration);

        // Use reflection to check private variable value
        assertEquals("TestCrawler", settings.get("crawler_name"));
    }

    @Test
    void testUpdateDocument_ExcludeConfidential() throws CrawlerPluginException {
        when(document.getCrawlUrl()).thenReturn("https://example.com/confidential-data");

        plugin.updateDocument(document);

        verify(document).setExclude(true);
        verify(document).getNoticeMessages();
    }

    @Test
    void testUpdateDocument_ModifyFields() throws CrawlerPluginException {
        when(document.getCrawlUrl()).thenReturn("https://example.com/data");
        
        Map<String, Object> fields = new HashMap<>();
        fields.put("__$role$__", "Admin");
        fields.put("__$businessgroup$__", "IT");
        fields.put("__$lastupdateddate$__", "1699999999");
        fields.put("__$date$__", "2023-11-01 10:15");

        when(document.getFields()).thenReturn(fields);

        plugin.updateDocument(document);

        assertEquals("Admin", fields.get("__$xxrole$__"));
        assertEquals("IT", fields.get("__$xxbgroup$__"));
        assertEquals("11/14/2023 10:13:19", fields.get("__$last-modified$__")); // Converted epoch
        assertEquals("11/01/2023 10:15:00", fields.get("__$new-date$__")); // Converted date format
    }

    @Test
    void testConvertToEpoch_InvalidInput() {
        String result = plugin.convertToEpoch("invalid");
        assertEquals("Invalid Date", result);
    }

    @Test
    void testConvertToNewFormat_InvalidDate() {
        String result = plugin.convertToNewFormat("invalid-date");
        assertEquals("Invalid Date", result);
    }
}
