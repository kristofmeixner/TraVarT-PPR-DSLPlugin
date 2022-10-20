package at.jku.cps.travart.plugin.ppr.dsl.exception;

public class NotSupportedConstraintType extends Exception {

    private static final long serialVersionUID = 6564597146968910891L;

    public NotSupportedConstraintType(final String message) {
        super(message);
    }

    public NotSupportedConstraintType(final Exception e) {
        super(e);
    }

}
