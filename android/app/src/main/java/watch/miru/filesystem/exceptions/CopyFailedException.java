package watch.miru.exceptions;

public class CopyFailedException extends Exception {

    public CopyFailedException(String s) {
        super(s);
    }

    public CopyFailedException(Throwable t) {
        super(t);
    }

    public CopyFailedException(String s, Throwable t) {
        super(s, t);
    }
}