package jasperreportsmcp;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

abstract class CustomMcpTool {

	protected static final Logger log = LoggerFactory.getLogger(CustomMcpTool.class);

	protected abstract String name();

	protected abstract String title();

	protected String description() {
		return "";
	}

	protected McpSchema.JsonSchema schema() {
		return new McpSchema.JsonSchema(
				//@formatter:off
				"object", // type
				Map.of("data", Map.of("type", "string")), // properties
				List.of("data"), // required
				false, // additionalProperties
				null, // $defs
				null // definitions
				//@formatter:on
		);
	}

	protected abstract BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> handler();

	protected McpServerFeatures.SyncToolSpecification build() {
		var tool = McpSchema.Tool.builder()
				.name(name())
				.title(title())
				.description(description())
				.inputSchema(schema())
				.build();
		return new McpServerFeatures.SyncToolSpecification(tool, handler());
	}

}
