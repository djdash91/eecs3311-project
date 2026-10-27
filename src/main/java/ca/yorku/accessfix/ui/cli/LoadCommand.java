package ca.yorku.accessfix.ui.cli;

import ca.yorku.accessfix.domain.Audit;
import ca.yorku.accessfix.facade.AccessFixFacade;
import ca.yorku.accessfix.facade.PageInput;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/** WORKED EXAMPLE of a CLI command: {@code accessfix load <url-or-file>} prints the new audit's ID. */
@Component
@Profile("cli")
@Command(name = "load", description = "Load a page from a URL or an HTML file and scan it.")
public class LoadCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "A URL (https://...) or the path of an HTML file")
    private String source;

    private final AccessFixFacade facade;

    public LoadCommand(AccessFixFacade facade) {
        this.facade = facade;
    }

    @Override
    public Integer call() throws Exception {
        Path file = Path.of(source);
        PageInput input = Files.isRegularFile(file)
                ? PageInput.ofHtml(Files.readString(file), file.getFileName().toString())
                : PageInput.ofUrl(source);

        Audit audit = facade.loadPage(input);
        System.out.println("Audit ID: " + audit.getId());
        System.out.println("Issues found: " + audit.latestScan().issueCount());
        return 0;
    }
}
