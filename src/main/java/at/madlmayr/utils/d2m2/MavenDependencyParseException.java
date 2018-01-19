package at.madlmayr.utils.d2m2;

/**
 * Exception that occure during creating a maven dependency
 */
public class MavenDependencyParseException extends Exception {

    public MavenDependencyParseException(final String msg){
        super(msg);
    }

    public MavenDependencyParseException(final Exception e){
        super(e);
    }

    public MavenDependencyParseException(final Throwable t){
        super(t);
    }

}
