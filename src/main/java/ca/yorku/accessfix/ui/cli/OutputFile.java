package ca.yorku.accessfix.ui.cli;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes report and export output to a file, or to the terminal when no file is given (F10). */
final class OutputFile {

    private OutputFile() {
    }

    static void write(String text, Path file, PrintWriter out, String what) throws IOException {
        if (file == null) {
            out.print(text);
            out.flush();
            return;
        }
        Files.writeString(file, text);
        out.println("Saved the " + what + " to " + file.toAbsolutePath());
    }
}
