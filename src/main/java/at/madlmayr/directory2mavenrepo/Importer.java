/*

 (c) by Gerald Madlmayr

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.

 For all third party components incorporated into this Software, those
 components are licensed under the original license provided by the owner of the
 applicable component.
*/

package at.madlmayr.directory2mavenrepo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main Class that traverses the directories and searches for JAR files to import.
 */
public class Importer {

  private static final Logger LOGGER = LoggerFactory.getLogger(Importer.class);
  private static final String EXT = ".jar";
  private static final String DRY_RUN_STRING = "dryrun";

  // cut of if the file tree is too deep.
  private static final int MAX_RECURSION_DEPTH = 25;
  private static final String ERROR_MSG_TOO_SHORT_PATH = "There are too little elements in the Path '%s'";
  private static final String ERROR_VERSION_EMPTY = "Version is empty";
  private static final String ERROR_ARTIFACT_ID_EMPTY = "ArtifactId is empty";
  private static final String ERROR_GROUP_ID_EMPTY = "Element of GroupId is empty";
  private static final String ERROR_PARAMETER_NAME_NOT_VALID = "Parameter '%s' is invalid";

  private static final String IMPORT_STRING = "mvn deploy:deploy-file -Dfile=%s -DgroupId=%s -DartifactId=%s -Dversion=%s -Dpackaging=jar -DrepositoryId=%s -Durl=%s";

  private Importer() {
  }


  public static void main(final String[] args) {


    // we require 3 or 4 arguments.
    if ((args.length != 3) && (args.length != 4)) {
      LOGGER.error("Invalid Amount of Parameters");
      LOGGER.info("Usage: java -jar d2m2.jar <Path> <RepositoryId> <Url-of-Repo> [dryrun]");
      return;
    }

    // Checking Parameter 1 - 4 for Strings and assign variables
    try {
      final String pathToMavenDepDirectory;
      final String idOfMavenRepo;
      final URL urlOfMavenRepo;

      // defensive. dryrun is true
      final boolean dryRun;

      pathToMavenDepDirectory = isStringOkayOrException(args[0], "Path");
      idOfMavenRepo = isStringOkayOrException(args[1], "RepositoryId");
      // Checking Parameter 3 for URL
      urlOfMavenRepo = new URL(isStringOkayOrException(args[2], "RepositoryUrl"));

      if (DRY_RUN_STRING.equals(isStringOkayOrException(args[3], "DryRun Parameter"))) {
        LOGGER.info("** Dry run only **");
        dryRun = true;
      } else {
        dryRun = false;
      }

      // This is the list of all maven Dependencies that we want to import.
      final List<MavenDependency> dependencies = new ArrayList<>();
      final File[] files = new File(pathToMavenDepDirectory).listFiles();
      if (files != null) {
        createFileList(files, pathToMavenDepDirectory, 0, dependencies);
      } else {
        LOGGER.error("Amount of Files in the given Path '{}', was null", pathToMavenDepDirectory);
        System.exit(-1);
      }

      // now importing all the dependencies into the given maven repo
      for (final MavenDependency dep : dependencies) {
        final String cmd = String.format(IMPORT_STRING, dep.getPath(), dep.getGroupId(),
            dep.getArtifactId(), dep.getVersion(), idOfMavenRepo, urlOfMavenRepo);
        if (dryRun) {
          LOGGER.info("Dryrun: '{}'", cmd);
        } else {
          executeCommand(cmd);
          LOGGER.info("Cmd Successful: {}", cmd);
        }
      }

      LOGGER.info("Import done.");

    } catch (final IllegalAccessException e) {
      LOGGER.error(e.getMessage());
      System.exit(-1);
    } catch (final MalformedURLException e) {
      LOGGER.error("Repo URL '{}' is not valid: '{}'", args[2], e.getMessage());
      System.exit(-1);
    }
  }

  private static void createFileList(final File[] files, final String baseDir, final int depth, final List<MavenDependency> dependencies) {

    for (final File file : files) {
      // we have a directory - walk into it.
      if (file.isDirectory()) {
        if (depth > MAX_RECURSION_DEPTH) {
          LOGGER.warn("Maximum Recursion Depth of '{}' reached in '{}'", depth, file.getAbsolutePath());
          return;
        } else {
          if (file.listFiles() != null) {
            createFileList(Objects.requireNonNull(file.listFiles()), baseDir, depth + 1, dependencies); // Calls same method again.
          } else {
            LOGGER.error("Amount of Files in the given Path '{}', was null", baseDir);
          }
        }
      }
      // we have a file -- check if it is a JAR and add it to the list
      else {
        if (file.getName()
            .endsWith(EXT)) {
          try {
            final MavenDependency m2 = createDtoFromPath(baseDir, file);
            dependencies.add(m2);
          } catch (final MavenDependencyParseException e) {
            LOGGER.error("Unable to create M2-Dependency out of '{}': '{}'", file.getAbsoluteFile(), e.getMessage());
          }
        }
        // we are not interested in other files
        // TODO: if there is a POM, we might be interested in using that one.
      }
    }
  }

  private static MavenDependency createDtoFromPath(final String baseDir, final File jarFile) throws MavenDependencyParseException {

    // create the relative path
    final String relative = new File(baseDir).toURI()
        .relativize(new File(jarFile.getAbsolutePath()).toURI())
        .getPath();

    // now Parsing the relative path, eg
    // last Element is fileName
    // last - 1 = Version
    // last - 2 = artifactId
    // last - 3 to 0 is groupId.

    final String[] elements = relative.split("/");

    // in this case we have an error
    if (elements.length < 4) {
      throw new MavenDependencyParseException(String.format(ERROR_MSG_TOO_SHORT_PATH, relative));
    }

    // Artifact ID
    try {
      isStringOkayOrException(elements[elements.length - 3], "Artifact-ID");
    } catch (final IllegalAccessException e) {
      throw new MavenDependencyParseException(ERROR_ARTIFACT_ID_EMPTY);
    }

    // Version
    try {
      isStringOkayOrException(elements[elements.length - 2], "Version");
    } catch (final IllegalAccessException e) {
      throw new MavenDependencyParseException(ERROR_VERSION_EMPTY);
    }

    // Create a sub array with the elements of the groupId
    final String[] groupIdElement = Arrays.copyOfRange(elements, 0, elements.length - 3);

    for (final String groupElement : groupIdElement) {

      try {
        isStringOkayOrException(groupElement, "Group-Element");
      } catch (final IllegalAccessException e) {
        throw new MavenDependencyParseException(ERROR_GROUP_ID_EMPTY);
      }
    }

    // Create the "."-separated String for the GroupId
    final String groupId = String.join(".", groupIdElement);

    // Create the dependency
    return new MavenDependency(groupId, elements[elements.length - 3], elements[elements.length - 2], jarFile);
  }

  /**
   * Simple method to check if a String is null or 0 (= broken)
   *
   * @param toCheck String to check
   * @return returns the string to check.
   */

  private static String isStringOkayOrException(final String toCheck, final String parameterName) throws IllegalAccessException {
    if ((toCheck == null) || (toCheck.isEmpty())) {
      throw new IllegalAccessException(String.format(ERROR_PARAMETER_NAME_NOT_VALID, parameterName));
    }
    return toCheck;
  }


  /**
   * Nice method to execute a command and read the response. Taken from: https://www.mkyong.com/java/how-to-execute-shell-command-from-java/
   *
   * @param command command to run on the shell.
   */

  private static void executeCommand(final String command) {

    try {
      final Process p = Runtime.getRuntime()
          .exec(command);
      p.waitFor();

      // reading data from the shell
      final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

      // printing the output to the log.
      String line;
      while ((line = reader.readLine()) != null) {
        LOGGER.debug(line);
      }

    } catch (final IOException e) {
      LOGGER.error("Issue Importing Files: {}", e.getMessage());
    } catch (final InterruptedException e) {
      LOGGER.error("Shell Command 'mvn' did not return (waitFor failed): {}", e.getMessage());
    }
  }

}