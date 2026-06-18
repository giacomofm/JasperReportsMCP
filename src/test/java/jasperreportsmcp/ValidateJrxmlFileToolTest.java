package jasperreportsmcp;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class ValidateJrxmlFileToolTest {

	private static final ValidateJrxmlFileTool tool = new ValidateJrxmlFileTool();

	private static String loadResourcePath(String name) {
		try {
			var url = ValidateJrxmlFileToolTest.class.getClassLoader().getResource(name);
			var src = Objects.requireNonNull(url, "Resource not found: " + name);
			return Path.of(src.toURI()).toString();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void name_shouldNotBeBlank() {
		assertNotNull(tool.name());
		assertFalse(tool.name().isBlank());
	}

	@Test
	void title_shouldNotBeBlank() {
		assertNotNull(tool.title());
		assertFalse(tool.title().isBlank());
	}

	@Test
	void nullPath_shouldReturnError() {
		var args = new HashMap<String, Object>();
		args.put("path", null);
		var req = McpSchema.CallToolRequest.builder().name(getClass().getName()).arguments(args).build();
		var res = tool.handler().apply(null, req);

		assertTrue(res.isError());
		assertFalse(res.content().isEmpty());

		var firstTextContent = (McpSchema.TextContent) res.content().getFirst();
		assertTrue(firstTextContent.text().contains("'path' is required"), "Expected error message about invalid path");
	}

	@Test
	void invalidPathType_shouldReturnError() {
		var args = Map.of("path", (Object) 42);
		var req = McpSchema.CallToolRequest.builder().name(getClass().getName()).arguments(args).build();
		var res = tool.handler().apply(null, req);

		assertTrue(res.isError());
		assertFalse(res.content().isEmpty());

		var firstTextContent = (McpSchema.TextContent) res.content().getFirst();
		assertTrue(firstTextContent.text().contains("must be a string"), "Expected error message about invalid path");
	}

	@Test
	void invalidPath_shouldReturnError() {
		var args = Map.of("path", (Object) "this_is_not_a_valid_path");
		var req = McpSchema.CallToolRequest.builder().name(getClass().getName()).arguments(args).build();
		var res = tool.handler().apply(null, req);

		assertTrue(res.isError());
		assertFalse(res.content().isEmpty());

		var firstTextContent = (McpSchema.TextContent) res.content().getFirst();
		assertTrue(firstTextContent.text().contains("Path provided is not valid"),
				"Expected error message about invalid path");
	}

	@Test
	void nonJrxmlExtension_shouldReturnError() {
		var args = Map.of("path", (Object) loadResourcePath("not-a-jrxml.txt"));
		var req = McpSchema.CallToolRequest.builder().name(getClass().getName()).arguments(args).build();
		var res = tool.handler().apply(null, req);

		assertTrue(res.isError());
		assertFalse(res.content().isEmpty());

		var firstTextContent = (McpSchema.TextContent) res.content().getFirst();
		assertTrue(firstTextContent.text().contains("File must have .jrxml extension"),
				"Expected error message about jrxml extension");
	}

	@Test
	void invalidJrxmlFile_shouldReturnErrors() {
		var args = Map.of("path", (Object) loadResourcePath("invalid-report.jrxml"));
		var req = McpSchema.CallToolRequest.builder().name(getClass().getName()).arguments(args).build();
		var res = tool.handler().apply(null, req);

		assertTrue(res.isError(), "Expected error for invalid .jrxml file");
		assertFalse(res.content().isEmpty());

		var firstTextContent = (McpSchema.TextContent) res.content().getFirst();
		assertTrue(firstTextContent.text().contains("expected </title>"),
				"Expected error message about missing </title> tag");
	}

	@Test
	void validJrxmlFile_shouldReturnOk() {
		var args = Map.of("path", (Object) loadResourcePath("valid-report.jrxml"));
		var req = McpSchema.CallToolRequest.builder().name(getClass().getName()).arguments(args).build();
		var res = tool.handler().apply(null, req);

		assertFalse(res.isError(), "Expected no error for valid .jrxml file");
		assertFalse(res.content().isEmpty());

		var firstTextContent = (McpSchema.TextContent) res.content().getFirst();
		assertTrue(firstTextContent.text().contains("Validation successful"));
	}

	@Test
	void reportWithFields_noContext_shouldReturnError() {
		var args = Map.of("path", (Object) loadResourcePath("report-with-fields.jrxml"));
		var req = McpSchema.CallToolRequest.builder().name(getClass().getName()).arguments(args).build();
		var res = tool.handler().apply(null, req);

		assertTrue(res.isError(), "Expected error when field not provided in context");
		assertFalse(res.content().isEmpty());

		var firstTextContent = (McpSchema.TextContent) res.content().getFirst();
		assertEquals("Field not found : userName", firstTextContent.text());
	}

	@Test
	void reportWithFields_withContext_shouldReturnOk() {
		var args = Map.of("path", (Object) loadResourcePath("report-with-fields.jrxml"), "data", "userName");
		var req = McpSchema.CallToolRequest.builder().name(getClass().getName()).arguments(args).build();
		var res = tool.handler().apply(null, req);

		assertFalse(res.isError(), "Expected no error when field provided via data");
		var text = ((McpSchema.TextContent) res.content().getFirst()).text();
		assertTrue(text.contains("Validation successful"));
	}

	@Test
	void reportWithFields_withMultipleFields_shouldReturnOk() {
		var args = Map.of("path", (Object) loadResourcePath("report-with-multi-fields.jrxml"), "data",
				"userName, userEmail, randomText");
		var req = McpSchema.CallToolRequest.builder().name(getClass().getName()).arguments(args).build();
		var res = tool.handler().apply(null, req);

		assertFalse(res.isError(), "Expected no error with extra fields");
		var text = ((McpSchema.TextContent) res.content().getFirst()).text();
		assertTrue(text.contains("Validation successful"));
	}

}
