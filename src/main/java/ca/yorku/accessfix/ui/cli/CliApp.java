package ca.yorku.accessfix.ui.cli;

import ca.yorku.accessfix.agent.events.AgentEventPublisher;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

/**
 * Runs one picocli command, then the process exits (Stage 1: "Each command runs as its own process").
 * Only active in CLI mode (see AccessFixApplication).
 */
@Component
@Profile("cli")
public class CliApp implements CommandLineRunner, ExitCodeGenerator {

    private final ApplicationContext context;
    private final AgentEventPublisher publisher;
    private int exitCode;

    public CliApp(ApplicationContext context, AgentEventPublisher publisher) {
        this.context = context;
        this.publisher = publisher;
    }

    @Override
    public void run(String... args) {
        publisher.subscribe(new ConsoleEventPrinter());
        exitCode = new CommandLine(RootCommand.class, springFactory()).execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    /** Lets picocli create command classes as Spring beans, so they can receive the facade. */
    private CommandLine.IFactory springFactory() {
        return new CommandLine.IFactory() {
            @Override
            public <K> K create(Class<K> type) throws Exception {
                try {
                    return context.getBean(type);
                } catch (NoSuchBeanDefinitionException e) {
                    return CommandLine.defaultFactory().create(type);
                }
            }
        };
    }
}
