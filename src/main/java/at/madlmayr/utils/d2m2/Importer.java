package at.madlmayr.utils.d2m2;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;

/**
 * Main Class that traverses the directories and searches for JAR files to import.
 */
public class Importer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Importer.class);
    private static final String EXT = ".jar";

    // cut of if the file tree is too deep.
    private static final int MAX_RECURSION_DEPTH = 25;

    public static void main(String[] args) {

        if (args.length != 1) {
            LOGGER.error("Invalid Amount of Parameters");
            System.exit(-1);
        }

        final File[] files = new File(args[0]).listFiles();
        createFileList(files, args[0], 0);
    }

    private static void createFileList(final File[] files, final String baseDir, final int depth) {

        for (final File file : files) {
            if (file.isDirectory()) {
                // LOGGER.debug("Directory: " + file.getName());
                if(depth > MAX_RECURSION_DEPTH){
                    LOGGER.warn("Maximum Recursion Depth of {} reached in {}", depth, file.getAbsolutePath());
                    return;
                } else {
                    createFileList(file.listFiles(), baseDir, depth + 1); // Calls same method again.
                }
            } else {
                if (file.getName().endsWith(EXT)) {
                    final MavenDependency m2 = createDtoFromPath(baseDir, file);
                    LOGGER.info("Dep: {}", m2);
                } else {
                    // we are not interested in other files
                }
            }
        }
    }


    private static MavenDependency createDtoFromPath(final String baseDir, final File jarFile){

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
            LOGGER.error("There are too little elements in the Path");
            return null;
        }

        final String[] groupIdElement = Arrays.copyOfRange(elements, 0, elements.length - 3);
        final String groupId = String.join(".", groupIdElement);
        return new MavenDependency(groupId, elements[elements.length-3], elements[elements.length-2], jarFile);
    }
}
