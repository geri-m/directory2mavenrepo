package at.madlmayr.utils.d2m2;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Main Class that traverses the directories and searches for JAR files to import.
 */
public class Importer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Importer.class);
    private static final String EXT = ".jar";

    // cut of if the file tree is too deep.
    private static final int MAX_RECURSION_DEPTH = 25;
    private static final String ERROR_MSG_TOO_SHORT_PATH = "There are too little elements in the Path '%s'";
    private static final String ERROR_VERSION_EMPTY ="Version is empty";
    private static final String ERROR_ARTIFACT_ID_EMPTY ="ArtifactId is empty";
    private static final String ERROR_GROUP_ID_EMPTY ="Element of GroupId is empty";

    private static final String IMPORT_STRING = "mvn deploy:deploy-file -Dfile=%s -DgroupId=%s -DartifactId=%s -Dversion=%s -Dpackaging=jar -DrepositoryId=%s -Durl=%s";


    public static void main(final String[] args) {

        if (args.length < 3 || args.length > 4) {
            LOGGER.error("Invalid Amount of Parameters");
            LOGGER.info("Usage: java -jar d2m2.jar <Path> <RepositoryId> <Url> [dryrun]");
            System.exit(-1);
        }

        if(isStringNotOkay(args[0])){
            LOGGER.error("Path must not be empty");
            System.exit(-1);
        }

        if(isStringNotOkay(args[1])){
            LOGGER.error("RepositoryId must not be empty");
            System.exit(-1);
        }

        if(isStringNotOkay(args[2])){
            LOGGER.error("Repository Path must not be empty");
            System.exit(-1);
        }


        LOGGER.info("Base Directory: {}", args[0]);
        LOGGER.info("RepositoryId:   {}", args[1]);
        LOGGER.info("URL of M2 Repo: {}", args[2]);

        if(isStringNotOkay(args[2])){
            LOGGER.info("** Doing Import ** ");
        } else {
            LOGGER.info("** Dry run only **");
        }


        final List<MavenDependency> dependencies = new ArrayList<>();
        final File[] files = new File(args[0]).listFiles();
        createFileList(files, args[0], 0, dependencies);


        // now importing all the dependencies into the given maven repo
        for(final MavenDependency dep : dependencies){
            final String cmd = String.format(IMPORT_STRING, dep.getPath(), dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), args[1], args[2]);
            if(isStringNotOkay(args[2])){
                executeCommand(cmd);
                LOGGER.info("Cmd Successful: {}", cmd);
            } else {
                LOGGER.info(cmd);
            }
        }

        LOGGER.info("Completed.");
    }

    private static void createFileList(final File[] files, final String baseDir, final int depth, final List<MavenDependency> dependencies) {

        for (final File file : files) {
            // we have a directory - walk into it.
            if (file.isDirectory()) {
                if(depth > MAX_RECURSION_DEPTH){
                    LOGGER.warn("Maximum Recursion Depth of {} reached in {}", depth, file.getAbsolutePath());
                    return;
                } else {
                    createFileList(file.listFiles(), baseDir, depth + 1, dependencies); // Calls same method again.
                }

            }
            // we have a file -- check if it is a JAR and add it to the list
            else {
                if (file.getName().endsWith(EXT)) {
                    try {
                        final MavenDependency m2 = createDtoFromPath(baseDir, file);
                        dependencies.add(m2);
                    } catch (MavenDependencyParseException e) {
                        LOGGER.error("Unable to create M2-Dependency out of '': ", file.getAbsoluteFile(), e.getMessage());
                    }
                }
                // we are not interested in other files
            }
        }
    }

    private static MavenDependency createDtoFromPath(final String baseDir, final File jarFile) throws MavenDependencyParseException{

        // create the relative path
        final String relative = new File(baseDir).toURI().relativize(new File(jarFile.getAbsolutePath()).toURI()).getPath();

        // now Parsing the relative path, eg

        // last Element is fileName
        // last - 1 = Version
        // last - 2 = artifactId
        // last - 3 to 0 is groupId.

        // com/ibm/rational/test/ft/autbase/8.5.0/autbase-8.5.0.jar


        final String[] elements = relative.split("/");

        // in this case we have an error
        if(elements.length < 4){
            throw new MavenDependencyParseException(String.format(ERROR_MSG_TOO_SHORT_PATH, relative));
        }

        // Artifact ID
        if(isStringNotOkay(elements[elements.length-3])){
            throw new MavenDependencyParseException(ERROR_ARTIFACT_ID_EMPTY);
        }

        // Version
        if(isStringNotOkay(elements[elements.length-2])){
            throw new MavenDependencyParseException(ERROR_VERSION_EMPTY);
        }

        // Create a sub array with the elements of the groupId
        final String[] groupIdElement = Arrays.copyOfRange(elements, 0, elements.length - 3);

        for(final String groupElement : groupIdElement){
            if(isStringNotOkay(groupElement)){
                throw new MavenDependencyParseException(ERROR_GROUP_ID_EMPTY);
            }
        }

        // Create the "."-separated String for the GroupId
        final String groupId = String.join(".", groupIdElement);

        // Create the dependency
        return new MavenDependency(groupId, elements[elements.length-3], elements[elements.length-2], jarFile);
    }

    /**
     * Simple method to check if a String is null or 0 (= broken)
     *
     * @param toCheck String to check
     * @return true if the string is "broken", false if the string is "okay"
     */

    private static boolean isStringNotOkay(final String toCheck){
        return (toCheck == null || toCheck.length() == 0);
    }


    /**
     * Nice method to execute a command and read the response.
     * Taken from: https://www.mkyong.com/java/how-to-execute-shell-command-from-java/
     *
     * @param command command to run on the shell.
     */

    private static void executeCommand(final String command) {

        try {
            final Process p = Runtime.getRuntime().exec(command);
            p.waitFor();

            // reading data from the shell
            final BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));

            // printing the output to the log.
            String line;
            while ((line = reader.readLine())!= null) {
                LOGGER.debug(line);
            }

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

}
