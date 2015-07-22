package se.tre.freki;

import se.tre.freki.labelmanager.CreateLabel;
import se.tre.freki.utils.KillingUncaughtHandler;
import se.tre.freki.web.HttpServer;

import java.util.Arrays;

public class Main {
  /**
   * The main entry-point of the project. This provides a git-like command line interface where
   * sub-commands delegate to more specific programs based on the first argument on the command
   * line.
   *
   * @param args The command-line arguments
   */
  public static void main(final String[] args) {
    KillingUncaughtHandler.install();

    if (args.length < 1) {
      usage(args);
      return;
    }

    switch (args[0]) {
      case "label":
        handleIdManager(args);
        break;
      case "web":
        handleWeb(args);
        break;
      default:
        usage(args);
    }
  }

  private static void usage(final String[] args) {
  }

  private static void handleIdManager(final String[] args) {
    if ("create".equals(args[1])) {
      CreateLabel.main(Arrays.copyOfRange(args, 2, args.length));
    }
  }

  private static void handleWeb(final String[] args) {
    HttpServer.main(Arrays.copyOfRange(args, 1, args.length));
  }
}
