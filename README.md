# JasperReportsMCP

**MCP Server for JasperReports - Fully based on Java MCP SDK and JasperReports library**  
JasperReportsMCP exposes JasperReports as MCP tools that any compatible AI agent can call directly: validate `.jrxml` report templates against real JasperReports compilation without leaving the agent workflow.

## Quick Start

Download `jasperreportsmcp.jar` from the latest release and add it to your MCP client config:

```json
{
  "mcpServers": {
    "JasperReportsMCP": {
      "type": "local",
      "command": "java",
      "args": ["-jar", "/path/to/jasperreportsmcp.jar"]
    }
  }
}
```

## Tools

| Tool | Input | Output |
|------|-------|--------|
| `validate-jrxml-file` | `path`: absolute path to `.jrxml` (required)<br>`data`: comma-separated field names (optional) | `"Validation successful"` or compilation errors |

`data` injects extra fields as `java.lang.String` before compilation — useful for templates referencing data source fields via `$F{fieldName}` not declared in the `.jrxml`.

## Build

```bash
./mvnw test       # run tests
./mvnw package    # package jar
```

## Stack

- Java 25
- [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk) v1.1.2
- [JasperReports](https://community.jaspersoft.com) v7.0.6 _(v6 support under evaluation)_
