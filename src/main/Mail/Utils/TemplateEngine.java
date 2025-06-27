package Mail.Utils;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class TemplateEngine {
    public static String renderTemplate(String templateName, Map<String, String> variables) throws IOException {
        String templatePath = "src/main/Mail/email_templates/" + templateName;
        String content = Files.readString(Paths.get(templatePath));

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            content = content.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }

        return content;
    }
}
