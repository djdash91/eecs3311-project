package ca.yorku.accessfix;

import java.util.Arrays;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Entry point. One application, two user interfaces (Stage 1, section 1.1, "Overall architecture").
 *
 * <ul>
 *   <li>GUI mode (default): starts the REST API on port 8080 for the React app.</li>
 *   <li>CLI mode: first argument is {@code cli}, for example {@code cli load samples/broken-page.html}.
 *       No web server starts; picocli handles the remaining arguments and the process exits.</li>
 * </ul>
 */
@SpringBootApplication
public class AccessFixApplication {

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("cli")) {
            String[] cliArgs = Arrays.copyOfRange(args, 1, args.length);
            ConfigurableApplicationContext context = new SpringApplicationBuilder(AccessFixApplication.class)
                    .web(WebApplicationType.NONE)
                    .profiles("cli")
                    .logStartupInfo(false)
                    .run(cliArgs);
            System.exit(SpringApplication.exit(context));
        } else {
            SpringApplication.run(AccessFixApplication.class, args);
        }
    }
}
