package jasperreportsmcp;

import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

final class ServerMcpApp {

	private static final Logger log = LoggerFactory.getLogger(ServerMcpApp.class);

	static void main() {
		JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new JsonMapper());
		// Stdio Server Transport (Support for SSE also available)
		var transportProvider = new StdioServerTransportProvider(jsonMapper);

		// Sync tool specification
		var syncToolSpecification = List.of(
				//@formatter:off
				new ValidateJrxmlFileTool().build()
				//@formatter:on
		);

		// Create a server with custom configuration
		McpSyncServer syncServer = McpServer.sync(transportProvider)
				.serverInfo("jasperreportstools-mcp-server", "0.1")
				.instructions(instructions)
				.capabilities(McpSchema.ServerCapabilities.builder().tools(true).logging().build())
				.tools(syncToolSpecification) // Register tools, resources, and prompts
				.build();

		log.info("Started {} (ver. {}) ...", syncServer.getServerInfo().name(), syncServer.getServerInfo().version());
	}

	private static final String instructions = """
			# JasperReports Tools MCP Server — Usage Guide
			
			This MCP server validates JasperReport `.jrxml` files to ensure their compatibility with JasperReports compilation.
			
			## Available Tools
			
			### `validate-jrxml-file`
			Validates a `.jrxml` file by performing a full JasperReports compilation.
			
			**Input:**
			- `path` (string, required): Absolute file path to the `.jrxml` file to validate.
			- `data` (string, optional): Comma-separated list of field names used in the template
			  (e.g. `"name,age,email"`). These fields are injected as `java.lang.String` into the
			  report design before compilation, allowing validation of templates that reference
			  data source fields via `$F{fieldName}`. Fields already declared in the `.jrxml` are
			  skipped.
			
			**Output on success:**
			- `isError`: false
			- `content`: A single text element confirming the file is valid.
			
			**Output on error:**
			- `isError`: true
			- `content`: One or more text elements, each describing a specific error found during
			  compilation. Use these messages to identify and fix problems in the `.jrxml` file.
			
			## Workflow Recommendation
			1. Call `validate-jrxml-file` with `path` and optionally `data` (field names).
			2. If errors are returned, read each error message and apply fixes to the `.jrxml` file.
			3. Re-validate after applying fixes to confirm all issues are resolved.
			""";

}
