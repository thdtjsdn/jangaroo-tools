package net.jangaroo.jooc.config;

import net.jangaroo.jooc.Jooc;
import org.apache.commons.cli.*;

import java.io.File;
import java.util.Arrays;

/**
 * Parses the jooc command line to produce a {@link JoocConfiguration}.
 */
public class JoocCommandLineParser {

  public static class CommandLineParseException extends Exception {
    private int exitCode;

    public CommandLineParseException(String message, int exitCode) {
      super(message);
      this.exitCode = exitCode;
    }

    public int getExitCode() {
      return exitCode;
    }
  }

  @SuppressWarnings({ "AccessStaticViaInstance" })
  public JoocConfiguration parse(String[] argv) throws Exception {
    JoocConfiguration config = new JoocConfiguration();

    Option help = new Option("help", "print this message");
    Option version = OptionBuilder
      .withDescription("print version information and exit")
      .create("version");
    Option verboseOption = OptionBuilder.withLongOpt("verbose")
      .withDescription("be extra verbose")
      .create("v");
    Option debugOption = OptionBuilder.withDescription( "generate debugging information " +
      "(possible modes: source, lines, none)" )
      .hasOptionalArgs()
      .withArgName("mode")
      .create("g");
    Option destinationDir = OptionBuilder.withArgName("dir")
      .hasArg()
      .withDescription("destination directory for generated JavaScript files")
      .create("d");
    Option enableAssertionsOption = OptionBuilder.withLongOpt("enableassertions")
            .withDescription("enable assertions")
            .create("ea");
    Option allowDuplicateLocalVariablesOption = OptionBuilder.withLongOpt("allowduplicatelocalvariables")
            .withDescription("allow multiple declarations of local variables")
            .create("ad");
    Option enableGuessingOption = OptionBuilder.withDescription(
      "Enable heuristic for guessing member access ('members'), classes in scope ('classes'), and type casts ('typecasts').")
      .withLongOpt("enableguessing")
      .hasOptionalArgs()
      .withArgName("mode")
      .create("eg");
    Options options = new Options();
    options.addOption(help);
    options.addOption(version);
    options.addOption(verboseOption);
    options.addOption(debugOption);
    options.addOption(destinationDir);
    options.addOption(enableAssertionsOption);
    options.addOption(allowDuplicateLocalVariablesOption);
    options.addOption(enableGuessingOption);
    CommandLineParser parser = new GnuParser();
    CommandLine line = null;

    try {
      line = parser.parse(options, argv);
    } catch (UnrecognizedOptionException e) {
      throw new CommandLineParseException(e.getMessage(), Jooc.RESULT_CODE_UNRECOGNIZED_OPTION);
    } catch (MissingArgumentException e) {
      throw new CommandLineParseException(e.getMessage(), Jooc.RESULT_CODE_MISSING_OPTION_ARGUMENT);
    }

    if (line.hasOption("help")) {
      printHelp(options);
      return null;
    }

    config.setVersion(line.hasOption("version"));
    config.setVerbose(line.hasOption(verboseOption.getOpt()));

    if (line.hasOption(destinationDir.getOpt())) {
      String destionationDirName = line.getOptionValue(destinationDir.getOpt());
      File destDir = new File(destionationDirName);
      if (!destDir.exists())
        throw new IllegalArgumentException("destination directory does not exist: " + destDir.getAbsolutePath());
      config.setOutputDirectory(destDir);
    }

    if (line.hasOption(enableAssertionsOption.getOpt()))
      config.setEnableAssertions(true);

    if (line.hasOption(allowDuplicateLocalVariablesOption.getOpt()))
      config.setAllowDuplicateLocalVariables(true);

    if (line.hasOption(debugOption.getOpt())) {
      String[] values = line.getOptionValues(debugOption.getOpt());
      config.setDebug(true);
      if (values == null || values.length == 0) {
        if (config.isVerbose()) {
          System.out.println("-g option present.");
        }
        config.setDebugLines(true);
        config.setDebugSource(true);
      } else {
        if (config.isVerbose()) {
          System.out.println("-g option value: " + Arrays.asList(values));
        }
        for (String value : values) {
          if (value.equals("source"))
            config.setDebugSource(true);
          else if (value.equals("lines"))
            config.setDebugLines(true);
          else if (value.equals("none")) {
            config.setDebug(false);
            config.setDebugSource(false);
            config.setDebugLines(false);
          } else
            throw new IllegalArgumentException("unknown -g argument: " + value);
        }
      }
    } else {
      config.setDebugSource(false);
      config.setDebugLines(true);
    }
    if (line.hasOption(enableGuessingOption.getOpt())) {
      String[] values = line.getOptionValues(enableGuessingOption.getOpt());
      if (values == null || values.length == 0) {
        if (config.isVerbose()) {
          System.out.println("-eg option present.");
        }
        config.setEnableGuessingMembers(true);
        config.setEnableGuessingClasses(true);
        config.setEnableGuessingTypeCasts(true);
      } else {
        if (config.isVerbose()) {
          System.out.println("-g option value: " + Arrays.asList(values));
        }
        for (String value : values) {
          if (value.equals("members"))
            config.setEnableGuessingMembers(true);
          else if (value.equals("classes"))
            config.setEnableGuessingClasses(true);
          else if (value.equals("typecasts")) {
            config.setEnableGuessingTypeCasts(true);
          } else
            throw new IllegalArgumentException("unknown -eg argument: " + value);
        }
      }
    } else {
      config.setEnableGuessingMembers(false);
      config.setEnableGuessingClasses(false);
      config.setEnableGuessingTypeCasts(false);
    }
    if (config.isVerbose()) {
      /*
      System.out.println("enableassertions=" +  enableAssertions);
      */
      System.out.println("-g option values:");
      System.out.println("source=" + config.isDebugSource());
      System.out.println("lines=" + config.isDebugLines());
    }

    String[] fileNames = line.getArgs();
    if (fileNames.length == 0) {
      printHelp(options);
      return null;
    }

    for (String fileName : fileNames)
      config.addSourceFile(fileName);

    return config;
  }

  protected void printHelp(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("jooc [options] <file> ...", options);
  }

}