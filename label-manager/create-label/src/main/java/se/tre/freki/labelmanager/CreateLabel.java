package se.tre.freki.labelmanager;

import static com.google.common.base.Preconditions.checkNotNull;

import se.tre.freki.application.CommandLineApplication;
import se.tre.freki.application.CommandLineOptions;
import se.tre.freki.core.LabelClient;
import se.tre.freki.labels.LabelException;
import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.LabelType;
import se.tre.freki.storage.Store;
import se.tre.freki.utils.InvalidConfigException;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.typesafe.config.ConfigException;
import joptsimple.OptionException;
import joptsimple.OptionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * Command line tool to create new Label IDs.
 */
public final class CreateLabel {
  private static final Logger LOG = LoggerFactory.getLogger(CreateLabel.class);
  private static final String READ_STDIN_SYMBOL = "-";

  private final LabelClient labelClient;

  @Inject
  CreateLabel(final LabelClient labelClient) {
    this.labelClient = checkNotNull(labelClient);
  }

  /**
   * Entry-point for the createLabel application. The createLabel program is normally not executed
   * directly but rather through the main project.
   *
   * @param args The command-line arguments
   */
  public static void main(final String[] args) {
    final CommandLineApplication application = CommandLineApplication.builder()
        .command("label create")
        .usage("[OPTIONS] <TYPE> [NAME]...")
        .description("Create IDs for NAME(s), or names read from standard input of type TYPE.")
        .helpText("With no NAME, or when NAME is -, read standard input.")
        .build();

    final CommandLineOptions cmdOptions = new CommandLineOptions();

    try {
      final OptionSet options = cmdOptions.parseOptions(args);

      if (cmdOptions.shouldPrintHelp()) {
        application.printHelpAndExit(cmdOptions);
      }

      cmdOptions.configureLogger();

      final CreateLabelComponent createLabelComponent = DaggerCreateLabelComponent.builder()
          .configModule(cmdOptions.configModule())
          .build();

      final List<?> nonOptionArguments = options.nonOptionArguments();

      final LabelType type = type(nonOptionArguments);
      final Store store = createLabelComponent.store();
      final CreateLabel createLabel = createLabelComponent.createLabel();

      final List<ListenableFuture<LabelId>> assignments = createLabel.createLabels(
          nonOptionArguments, type);

      LOG.debug("Waiting for {} assignments to complete", assignments.size());
      Futures.successfulAsList(assignments).get();
      store.close();
    } catch (IllegalArgumentException | OptionException e) {
      application.printError(e.getMessage());
      System.exit(42);
    } catch (InvalidConfigException | ConfigException e) {
      System.err.println(e.getMessage());
      System.exit(42);
    } catch (Exception e) {
      LOG.error("Fatal error while creating id", e);
      System.exit(42);
    }
  }

  private List<ListenableFuture<LabelId>> createLabels(final List<?> nonOptionArguments,
                                                       final LabelType type) throws IOException {
    if (shouldReadFromStdin(nonOptionArguments)) {
      return readNamesFrom(new InputStreamReader(System.in), type);
    } else if (nonOptionArguments.size() > 1) {
      return readNamesFrom(nonOptionArguments, type);
    } else {
      throw new IllegalArgumentException("No names to read from args or from STDIN");
    }
  }

  private boolean shouldReadFromStdin(final List<?> nonOptionArguments) {
    return nonOptionArguments.size() == 1 || READ_STDIN_SYMBOL.equals(nonOptionArguments.get(1));
  }

  private List<ListenableFuture<LabelId>> readNamesFrom(final List<?> nonOptionArguments,
                                                        final LabelType type) {
    final ImmutableSet<?> names = ImmutableSet.copyOf(
        nonOptionArguments.subList(1, nonOptionArguments.size()));
    final List<ListenableFuture<LabelId>> assignments = new ArrayList<>(names.size());

    for (final Object name : names) {
      assignments.add(createLabel(name.toString(), type));
    }

    return assignments;
  }

  private List<ListenableFuture<LabelId>> readNamesFrom(final Reader nameSource,
                                                        final LabelType type) throws IOException {
    final LineNumberReader reader = new LineNumberReader(new BufferedReader(nameSource));
    final List<ListenableFuture<LabelId>> assignments = new ArrayList<>();

    String name = reader.readLine();

    while (name != null) {
      assignments.add(createLabel(name, type));
      name = reader.readLine();
    }

    return assignments;
  }

  private static LabelType type(final List<?> nonOptionArguments) {
    try {
      final String stringType = nonOptionArguments.get(0).toString();
      return LabelType.fromValue(stringType);
    } catch (IndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Missing identifier type to create label", e);
    }
  }

  private ListenableFuture<LabelId> createLabel(final String name, final LabelType type) {
    final ListenableFuture<LabelId> id = labelClient.createId(type, name);
    Futures.addCallback(id, new LogNewIdCallback(name, type));
    return id;
  }

  private static class LogNewIdCallback implements FutureCallback<LabelId> {
    private final String name;
    private final LabelType type;

    public LogNewIdCallback(final String name, final LabelType type) {
      this.name = name;
      this.type = type;
    }

    @Override
    public void onSuccess(@Nullable final LabelId id) {
      LOG.info("{} {}: {}", type, name, id);
    }

    @Override
    public void onFailure(final Throwable throwable) {
      if (throwable instanceof LabelException) {
        System.err.println(throwable.getMessage());
      } else {
        LOG.error("{} {}: {}", name, type, throwable.getMessage(), throwable);
      }
    }
  }
}
