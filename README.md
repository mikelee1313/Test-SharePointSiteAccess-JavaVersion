# Test SharePoint Site Access  - Java Version

This Java application tests SharePoint site access permissions using the Microsoft Graph API with certificate-based authentication. It's a Java conversion of the original PowerShell script that performs the same functionality.

## Overview

The application performs these main functions:
1. **Authentication**: Uses certificate-based authentication to connect to Microsoft Graph API
2. **Site Information**: Retrieves SharePoint site details to validate connectivity
3. **Read Access Testing**: Tests access to site lists and document library content
4. **Write Access Testing**: Creates and deletes temporary files to verify write permissions
5. **Detailed Reporting**: Provides comprehensive success/failure reporting

## Key Features

✅ **Certificate-based Authentication** - Uses Azure AD app registration with PEM certificates  
✅ **Direct HTTP API Calls** - Uses OkHttp and Jackson for reliable Microsoft Graph API communication  
✅ **Comprehensive Testing** - Tests both read and write access to SharePoint sites  
✅ **Detailed Logging** - Provides extensive logging to console and file  
✅ **Error Handling** - Robust error handling and meaningful error messages  
✅ **Clean Resource Management** - Automatically cleans up test files after write tests  

## Prerequisites

- **Java 11 or later**
- **Maven 3.6 or later** (for building and dependency management)
- **Valid Azure AD app registration** with certificate authentication configured
- **PEM certificate file** for authentication
- **SharePoint site** with appropriate permissions configured

## Configuration

Before running the application, update the configuration file at `src/main/resources/config.properties`:

```properties
# Azure AD / Microsoft Entra ID configuration
azure.tenantId=your-tenant-id-here
azure.clientId=your-client-id-here

# SharePoint site configuration
sharepoint.siteUrl=https://your-sharepoint-site-url

# Certificate configuration (PEM format)
certificate.certificatePath=C:\\path\\to\\your\\certificate.pem
certificate.privateKeyPath=C:\\path\\to\\your\\private-key.pem

# Test configuration
test.type=Both
# Valid values: Read, Write, Both
```

## Building the Application

```bash
mvn clean compile
```

## Running the Application

### Using Maven:
```bash
mvn exec:java -Dexec.mainClass="com.microsoft.sharepoint.SharePointAccessTest"
```

### Using the VS Code tasks:
- **Build**: Run the "build" task
- **Run**: Run the "run" task  
- **Test**: Run the "test" task

## Test Types

The application supports three test modes:

- **Read**: Tests only read access to SharePoint lists and document libraries
- **Write**: Tests only write access by creating and deleting temporary files
- **Both**: Performs both read and write access tests (default)

## Dependencies

The project uses these key dependencies:

- **Azure Identity SDK**: For certificate-based authentication with Azure AD
- **OkHttp**: For HTTP client operations and Microsoft Graph API calls
- **Jackson**: For JSON processing and response parsing
- **SLF4J + Logback**: For comprehensive logging
- **Apache Commons Configuration**: For properties file handling
- **JUnit 5**: For unit testing

## Output

The application provides detailed console output showing:
- Authentication status with certificate information
- Site information retrieval results
- Read access test results (site lists and document library access)
- Write access test results (file upload/delete operations)
- Final summary of all test results

Sample output:
```
INFO Loading configuration...
INFO Configuration loaded successfully
INFO Tenant ID: 85612ccb-4c28-4a34-88df-a538cc139a51
INFO Starting SharePoint Site Access Test
INFO Authenticating with certificate...
INFO Successfully authenticated using certificate
INFO Getting site information for: m365x61250205.sharepoint.com/sites/commsite1
INFO Site ID: m365x61250205.sharepoint.com,abc123...
INFO Site Name: Communication Site 1

=== READ ACCESS TEST ===
INFO Test 1: Retrieving site lists...
INFO SUCCESS - Found 8 lists in the site
INFO Sample lists:
INFO   - Documents (ID: abc123...)
INFO   - Site Pages (ID: def456...)
INFO Test 2: Retrieving document library items...
INFO SUCCESS - Found 3 items in the default document library
INFO Read access to the site collection is working properly

=== WRITE ACCESS TEST ===
INFO Retrieving default document library...
INFO SUCCESS - Found default document library
INFO Attempting to upload test file (WriteAccessTest_20251226_143052.txt)...
INFO SUCCESS - Test file uploaded successfully: https://...
INFO Cleaning up test file...
INFO SUCCESS - Test file cleaned up
INFO Write access to the site collection is working properly

=== TEST SUMMARY ===
INFO Read Access: SUCCESS
INFO Write Access: SUCCESS
```

## Logging

Logs are written to both console and file (`logs/sharepoint-access-test.log`). The logging configuration can be modified in `src/main/resources/logback.xml`.

## Troubleshooting

### Common Issues:

1. **Certificate Loading Errors**: Ensure the PEM certificate and private key file paths are correct
2. **Authentication Failures**: Verify tenant ID, client ID, and certificate configuration in Azure AD
3. **Permission Errors**: Ensure the Azure AD app has proper SharePoint permissions configured
4. **Site Access Issues**: Verify the SharePoint site URL and that the app has access to the site

### Required Azure AD App Permissions:

- `Sites.Read.All` - For reading SharePoint site information
- `Sites.ReadWrite.All` - For write access testing (if using Write or Both test types)

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/microsoft/sharepoint/
│   │       └── SharePointAccessTest.java
│   └── resources/
│       ├── config.properties
│       ├── config.properties.sample
│       └── logback.xml
└── target/
    └── (compiled classes and dependencies)
```

## API Implementation Details

This Java version uses direct HTTP calls to the Microsoft Graph API rather than the Graph SDK to provide:
- Better error handling and debugging capabilities
- More control over request/response processing  
- Clearer understanding of the underlying API calls
- Reduced dependency complexity

The application makes calls to these Microsoft Graph endpoints:
- `GET /sites/{hostname}:{path}` - Get site information
- `GET /sites/{site-id}/lists` - Get site lists
- `GET /sites/{site-id}/drives` - Get document libraries
- `GET /sites/{site-id}/drives/{drive-id}/root/children` - Get library contents
- `PUT /sites/{site-id}/drives/{drive-id}/root:/{filename}:/content` - Upload file
- `DELETE /sites/{site-id}/drives/{drive-id}/items/{item-id}` - Delete file

## Contributing

This project follows standard Java development practices:
- Use proper exception handling and logging
- Follow Java naming conventions
- Handle certificate operations securely
- Provide clear console output for test results
- Write unit tests for new functionality

## License

This project is provided as-is for testing SharePoint site access permissions.
