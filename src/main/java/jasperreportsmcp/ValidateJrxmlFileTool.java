package jasperreportsmcp;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRValidationFault;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

class ValidateJrxmlFileTool extends CustomMcpTool {

	private static final Object JASPER_REPORTS_VALIDATION_LOCK = new Object();

	@Override
	protected String name() {
		return "validate-jrxml-file";
	}

	@Override
	protected String title() {
		return "Validate .JRXML file";
	}

	@Override
	protected McpSchema.JsonSchema schema() {
		return new McpSchema.JsonSchema("object",
				//@formatter:off
				Map.of(
"path", Map.of("type", "string", "description", "Absolute file path to the .jrxml file"),
"data", Map.of("type", "string", "description", "Comma-separated list of field names used in the template (e.g. 'name,age,email'). Fields are injected as java.lang.String for validation.")
				),
				//@formatter:on
				List.of("path"), false, null, null);
	}

	@Override
	protected BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> handler() {
		return (_, req) -> {
			var pathArg = req.arguments().get("path");
			var dataArg = req.arguments().get("data");

			log.debug("Received validate request with path: {}, data: {}", pathArg, dataArg);

			if (!(pathArg instanceof String strPath)) {
				return McpSchema.CallToolResult.builder()
						.isError(true)
						.addTextContent("Invalid input: 'path' is required and must be a string.")
						.build();
			}

			List<String> fieldNames = List.of();
			if (dataArg instanceof String strData && !strData.isBlank()) {
				fieldNames = Arrays.stream(strData.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
			}

			return validateJrxmlFile(strPath, fieldNames);
		};
	}

	private static McpSchema.CallToolResult validateJrxmlFile(String strPath, List<String> fieldNames) {
		if (!isValidFilePath(strPath)) {
			return McpSchema.CallToolResult.builder()
					.isError(true)
					.addTextContent("Path provided is not valid: " + strPath)
					.build();
		}

		if (!strPath.endsWith(".jrxml")) {
			return McpSchema.CallToolResult.builder()
					.isError(true)
					.addTextContent("File must have .jrxml extension: " + strPath)
					.build();
		}

		try {
			synchronized (JASPER_REPORTS_VALIDATION_LOCK) { // todo: understand how to really test
				var path = Path.of(strPath);
				var jasperDesign = JRXmlLoader.load(path.toFile());

				injectFields(jasperDesign, fieldNames);

				var designErrors = JasperCompileManager.verifyDesign(jasperDesign);
				if (!designErrors.isEmpty()) {
					var builder = McpSchema.CallToolResult.builder().isError(true);
					designErrors.stream().map(JRValidationFault::getMessage).forEach(builder::addTextContent);
					return builder.build();
				}

				JasperCompileManager.compileReport(jasperDesign);
			}
		} catch (Exception e) {
//			log.warn("Validation failed for file: " + strPath, e);
			return McpSchema.CallToolResult.builder().isError(true).addTextContent(e.getMessage()).build();
		}
		return McpSchema.CallToolResult.builder()
				.addTextContent("Validation successful. File valid: " + strPath)
				.build();
	}

	private static boolean isValidFilePath(String strPath) {
		try {
			var path = Path.of(strPath);
			return Files.isRegularFile(path);
		} catch (Exception e) {
			return false;
		}
	}

	private static void injectFields(JasperDesign design, List<String> fieldNames) throws JRException {
		for (String fieldName : fieldNames) {
			if (design.getFieldsMap().containsKey(fieldName)) {
				continue; // already declared in jrxml
			}
			var field = new JRDesignField();
			field.setName(fieldName);
			field.setValueClass(String.class);
			design.addField(field);
		}
	}

}
