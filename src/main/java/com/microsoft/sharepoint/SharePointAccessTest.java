package com.microsoft.sharepoint;

import com.azure.identity.ClientCertificateCredential;
import com.azure.identity.ClientCertificateCredentialBuilder;
import okhttp3.*;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * SharePoint Site Access Test Application
 * 
 * This Java application tests SharePoint site access permissions using the Microsoft Graph API
 * with certificate-based authentication. It performs read and/or write tests to validate
 * proper permissions have been configured for the Azure AD application.
 * 
 * The application performs these main functions:
 * 1. Authenticates to Microsoft Graph API using a certificate
 * 2. Retrieves SharePoint site information
 * 3. Tests read access by retrieving site lists and document library content
 * 4. Tests write access by creating and deleting a temporary file
 * 5. Provides a detailed summary of test results
 * 
 * @author Mike Lee
 * @version 1.0.0
 * @since 2025-06-26
 */
public class SharePointAccessTest {
    
    private static final Logger logger = LoggerFactory.getLogger(SharePointAccessTest.class);
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Configuration properties
    private String tenantId;
    private String clientId;
    private String siteUrl;
    private String certificatePath;
    private String privateKeyPath;
    private String testType;
    
    // Parsed site URL components
    private String hostname;
    private String path;
    private String siteId;
    
    // Authentication
    private ClientCertificateCredential credential;
    private String accessToken;
    
    /**
     * Test results tracking
     */
    public static class TestResults {
        private Boolean readSuccess = null;
        private Boolean writeSuccess = null;
        
        public Boolean getReadSuccess() { return readSuccess; }
        public void setReadSuccess(Boolean readSuccess) { this.readSuccess = readSuccess; }
        
        public Boolean getWriteSuccess() { return writeSuccess; }
        public void setWriteSuccess(Boolean writeSuccess) { this.writeSuccess = writeSuccess; }
    }
    
    /**
     * Main entry point
     */
    public static void main(String[] args) {
        try {
            SharePointAccessTest test = new SharePointAccessTest();
            test.loadConfiguration();
            test.run();
        } catch (Exception e) {
            logger.error("Application failed: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
    
    /**
     * Load configuration from properties file
     */
    private void loadConfiguration() throws Exception {
        logger.info("Loading configuration...");
        
        Configurations configs = new Configurations();
        Configuration config = configs.properties(new File("src/main/resources/config.properties"));
        
        this.tenantId = config.getString("azure.tenantId");
        this.clientId = config.getString("azure.clientId");
        this.siteUrl = config.getString("sharepoint.siteUrl");
        this.certificatePath = config.getString("certificate.certificatePath");
        this.privateKeyPath = config.getString("certificate.privateKeyPath");
        this.testType = config.getString("test.type", "Both");
        
        // Parse site URL components
        parseSiteUrl();
        
        logger.info("Configuration loaded successfully");
        logger.info("Tenant ID: {}", tenantId);
        logger.info("Client ID: {}", clientId);
        logger.info("Site URL: {}", siteUrl);
        logger.info("Test Type: {}", testType);
    }
    
    /**
     * Parse the site URL into components needed for Graph API calls
     */
    private void parseSiteUrl() throws URISyntaxException {
        URI uri = new URI(siteUrl);
        this.hostname = uri.getHost();
        this.path = uri.getPath();
        
        logger.info("Parsed site URL - Hostname: {}, Path: {}", hostname, path);
    }
    
    /**
     * Main execution method
     */
    private void run() throws Exception {
        logger.info("Starting SharePoint Site Access Test");
        
        // Step 1: Authenticate with certificate
        authenticate();
        
        // Step 2: Get site information
        getSiteInformation();
        
        // Step 3: Perform access tests
        TestResults results = testSiteAccess();
        
        // Step 4: Display final summary
        displaySummary(results);
        
        logger.info("SharePoint Site Access Test completed");
    }
    
    /**
     * Authenticate to Microsoft Graph using certificate-based authentication
     */
    private void authenticate() throws Exception {
        logger.info("Authenticating with certificate...");
        
        try {
            // Validate certificate and private key files exist
            File certFile = new File(certificatePath);
            File keyFile = new File(privateKeyPath);
            
            if (!certFile.exists()) {
                throw new IOException("Certificate file not found: " + certificatePath);
            }
            if (!keyFile.exists()) {
                throw new IOException("Private key file not found: " + privateKeyPath);
            }
            
            // Create client certificate credential using PEM file
            this.credential = new ClientCertificateCredentialBuilder()
                    .tenantId(tenantId)
                    .clientId(clientId)
                    .pemCertificate(certificatePath)
                    .build();
            
            // Get access token
            this.accessToken = credential.getToken(
                new com.azure.core.credential.TokenRequestContext()
                    .addScopes("https://graph.microsoft.com/.default")
            ).block().getToken();
            
            logger.info("Access Token: {}", accessToken);
            logger.info("Successfully authenticated using certificate");
            
        } catch (Exception e) {
            logger.error("Error authenticating with certificate: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Get SharePoint site information to validate connectivity
     */
    private void getSiteInformation() throws Exception {
        logger.info("Getting site information for: {}{}", hostname, path);
        
        try {
            // Build the Graph API URL for site information
            String url = String.format("https://graph.microsoft.com/v1.0/sites/%s:%s", hostname, path);
            
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Failed to get site information: " + response.code() + " " + response.message());
                }
                
                String responseBody = response.body().string();
                JsonNode siteInfo = objectMapper.readTree(responseBody);
                
                this.siteId = siteInfo.get("id").asText();
                String siteName = siteInfo.get("displayName").asText();
                
                logger.info("Site ID: {}", siteId);
                logger.info("Site Name: {}", siteName);
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving site information: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Test site access permissions
     */
    private TestResults testSiteAccess() {
        TestResults results = new TestResults();
        
        logger.info("Testing access to site: {}", siteUrl);
        logger.info("Test type: {}", testType);
        
        // Perform read access test
        if ("Read".equals(testType) || "Both".equals(testType)) {
            results.setReadSuccess(testReadAccess());
        }
        
        // Perform write access test
        if ("Write".equals(testType) || "Both".equals(testType)) {
            results.setWriteSuccess(testWriteAccess());
        }
        
        return results;
    }
    
    /**
     * Test read access to the SharePoint site
     */
    private boolean testReadAccess() {
        logger.info("\n=== READ ACCESS TEST ===");
        
        try {
            // Test 1: Get site lists
            logger.info("Test 1: Retrieving site lists...");
            String listsUrl = String.format("https://graph.microsoft.com/v1.0/sites/%s/lists", siteId);
            
            Request request = new Request.Builder()
                    .url(listsUrl)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("FAILED - Could not retrieve lists: {} {}", response.code(), response.message());
                    return false;
                }
                
                String responseBody = response.body().string();
                JsonNode listsResponse = objectMapper.readTree(responseBody);
                JsonNode lists = listsResponse.get("value");
                
                if (lists != null && lists.isArray() && lists.size() > 0) {
                    logger.info("SUCCESS - Found {} lists in the site", lists.size());
                    
                    // Display sample lists
                    logger.info("Sample lists:");
                    for (int i = 0; i < Math.min(3, lists.size()); i++) {
                        JsonNode list = lists.get(i);
                        logger.info("  - {} (ID: {})", 
                                   list.get("displayName").asText(), 
                                   list.get("id").asText());
                    }
                    
                    // Test 2: Get document library items
                    logger.info("Test 2: Retrieving document library items...");
                    return testDocumentLibraryAccess();
                    
                } else {
                    logger.error("FAILED - No lists found");
                    return false;
                }
            }
            
        } catch (Exception e) {
            logger.error("ERROR testing read access: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Test access to document library items
     */
    private boolean testDocumentLibraryAccess() {
        try {
            // Get drives (document libraries) in the site
            String drivesUrl = String.format("https://graph.microsoft.com/v1.0/sites/%s/drives", siteId);
            
            Request request = new Request.Builder()
                    .url(drivesUrl)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("FAILED - Could not retrieve drives: {} {}", response.code(), response.message());
                    return false;
                }
                
                String responseBody = response.body().string();
                JsonNode drivesResponse = objectMapper.readTree(responseBody);
                JsonNode drives = drivesResponse.get("value");
                
                if (drives != null && drives.isArray()) {
                    // Find the default document library
                    JsonNode defaultDrive = null;
                    for (JsonNode drive : drives) {
                        String driveName = drive.get("name").asText();
                        if ("Documents".equals(driveName) || "Shared Documents".equals(driveName)) {
                            defaultDrive = drive;
                            break;
                        }
                    }
                    
                    if (defaultDrive != null) {
                        // Get contents of the default document library
                        String driveId = defaultDrive.get("id").asText();
                        String itemsUrl = String.format("https://graph.microsoft.com/v1.0/sites/%s/drives/%s/root/children", 
                                                      siteId, driveId);
                        
                        Request itemsRequest = new Request.Builder()
                                .url(itemsUrl)
                                .addHeader("Authorization", "Bearer " + accessToken)
                                .addHeader("Content-Type", "application/json")
                                .build();
                        
                        try (Response itemsResponse = httpClient.newCall(itemsRequest).execute()) {
                            if (itemsResponse.isSuccessful()) {
                                String itemsResponseBody = itemsResponse.body().string();
                                JsonNode itemsJson = objectMapper.readTree(itemsResponseBody);
                                JsonNode items = itemsJson.get("value");
                                
                                int itemCount = items != null && items.isArray() ? items.size() : 0;
                                logger.info("SUCCESS - Found {} items in the default document library", itemCount);
                                logger.info("Read access to the site collection is working properly");
                                return true;
                            } else {
                                logger.error("FAILED - Could not retrieve document library items: {} {}", 
                                           itemsResponse.code(), itemsResponse.message());
                                return false;
                            }
                        }
                        
                    } else {
                        logger.warn("PARTIAL - Could retrieve drives but default document library not found");
                        return false;
                    }
                } else {
                    logger.error("FAILED - Could not retrieve drives");
                    return false;
                }
            }
            
        } catch (Exception e) {
            logger.error("FAILED - Could not retrieve document library items: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Test write access to the SharePoint site
     */
    private boolean testWriteAccess() {
        logger.info("\n=== WRITE ACCESS TEST ===");
        
        try {
            // Create a temporary test file with timestamp
            String tempFileName = "WriteAccessTest_" + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
            String tempFileContent = "This is a test file created at " + LocalDateTime.now() + 
                    " to verify write access.";
            
            // Get the default document library
            logger.info("Retrieving default document library...");
            String drivesUrl = String.format("https://graph.microsoft.com/v1.0/sites/%s/drives", siteId);
            
            Request request = new Request.Builder()
                    .url(drivesUrl)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("FAILED - Could not retrieve drives: {} {}", response.code(), response.message());
                    return false;
                }
                
                String responseBody = response.body().string();
                JsonNode drivesResponse = objectMapper.readTree(responseBody);
                JsonNode drives = drivesResponse.get("value");
                
                JsonNode defaultDrive = null;
                for (JsonNode drive : drives) {
                    String driveName = drive.get("name").asText();
                    if ("Documents".equals(driveName) || "Shared Documents".equals(driveName)) {
                        defaultDrive = drive;
                        break;
                    }
                }
                
                if (defaultDrive == null) {
                    logger.error("FAILED - Default document library not found");
                    return false;
                }
                
                logger.info("SUCCESS - Found default document library");
                
                // Upload the test file
                logger.info("Attempting to upload test file ({})...", tempFileName);
                String driveId = defaultDrive.get("id").asText();
                String uploadUrl = String.format("https://graph.microsoft.com/v1.0/sites/%s/drives/%s/root:/%s:/content", 
                                                siteId, driveId, tempFileName);
                
                RequestBody fileBody = RequestBody.create(tempFileContent, MediaType.parse("text/plain"));
                Request uploadRequest = new Request.Builder()
                        .url(uploadUrl)
                        .put(fileBody)
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .addHeader("Content-Type", "text/plain")
                        .build();
                
                try (Response uploadResponse = httpClient.newCall(uploadRequest).execute()) {
                    if (uploadResponse.isSuccessful()) {
                        String uploadResponseBody = uploadResponse.body().string();
                        JsonNode uploadedItem = objectMapper.readTree(uploadResponseBody);
                        String webUrl = uploadedItem.get("webUrl").asText();
                        String itemId = uploadedItem.get("id").asText();
                        
                        logger.info("SUCCESS - Test file uploaded successfully: {}", webUrl);
                        
                        // Clean up by deleting the test file
                        logger.info("Cleaning up test file...");
                        String deleteUrl = String.format("https://graph.microsoft.com/v1.0/sites/%s/drives/%s/items/%s", 
                                                        siteId, driveId, itemId);
                        
                        Request deleteRequest = new Request.Builder()
                                .url(deleteUrl)
                                .delete()
                                .addHeader("Authorization", "Bearer " + accessToken)
                                .build();
                        
                        try (Response deleteResponse = httpClient.newCall(deleteRequest).execute()) {
                            if (deleteResponse.isSuccessful()) {
                                logger.info("SUCCESS - Test file cleaned up");
                            } else {
                                logger.warn("FAILED - Cleanup failed but write test succeeded: {} {}", 
                                          deleteResponse.code(), deleteResponse.message());
                            }
                        }
                        
                        logger.info("Write access to the site collection is working properly");
                        return true;
                        
                    } else {
                        logger.error("FAILED - Could not upload test file: {} {}", 
                                   uploadResponse.code(), uploadResponse.message());
                        return false;
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("ERROR testing write access: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Display final test summary
     */
    private void displaySummary(TestResults results) {
        logger.info("\n=== TEST SUMMARY ===");
        
        if ("Read".equals(testType) || "Both".equals(testType)) {
            String readResult = Boolean.TRUE.equals(results.getReadSuccess()) ? "SUCCESS" : "FAILED";
            logger.info("Read Access: {}", readResult);
        }
        
        if ("Write".equals(testType) || "Both".equals(testType)) {
            String writeResult = Boolean.TRUE.equals(results.getWriteSuccess()) ? "SUCCESS" : "FAILED";
            logger.info("Write Access: {}", writeResult);
        }
    }
}
