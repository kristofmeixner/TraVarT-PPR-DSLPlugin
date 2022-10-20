package at.jku.cps.travart.plugin.ppr.dsl.parser;

import at.jku.cps.travart.core.common.Prop4JUtils;
import at.jku.cps.travart.core.common.TraVarTUtils;
import at.jku.cps.travart.ppr.dsl.exc.ParserException;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.impl.Constraint;
import org.prop4j.Node;

import java.util.Objects;

public class ConstraintDefinitionParser {

    public static final String DELIMITER = ",";
    public static final String DEFINITION_ARROW = "->";
    public static final String IMPLIES = "implies";
    public static final String OR = "or";
    public static final String AND = "and";
    public static final String NOT = "not";
    private static final String REGEX = "(?<=,)|(?=,)|(?<=->)|(?=->)|((?<= implies )|(?= implies ))|((?<= or )|(?= or ))|((?<= and )|(?= and ))|((?<= not )|(?= not ))";
    private static final String EOF = "EOF";
    private final IFeatureModel fm;
    private String[] input;
    private int index = 0;
    private String symbol;

    public ConstraintDefinitionParser(final IFeatureModel fm) {
        this.fm = Objects.requireNonNull(fm);
    }

    public de.vill.model.constraint.Constraint parse(final String str) throws ParserException {
        Objects.requireNonNull(str);
        this.index = 0;
        this.input = TraVarTUtils.splitString(str, REGEX);
        if (this.input.length > 0) {
            return new Constraint(this.fm, this.parseConstraintNode());
        }
        throw new ParserException("No action found in String " + str);
    }

    private Node parseConstraintNode() throws ParserException {
        Node n = null;
        this.nextSymbol();
        while (!this.symbol.equals(EOF)) {
            if (this.symbol.equals(DELIMITER)) {
                this.nextSymbol();
                continue;
            }
            if (this.symbol.equals(DEFINITION_ARROW) || this.symbol.equals(IMPLIES) || this.symbol.equals(OR) || this.symbol.equals(AND)) {
                n = this.implies();
            }
            this.nextSymbol();
        }
        return n;
    }

    private Node implies() {
        Node n = this.or();
        while (this.symbol.equals(IMPLIES)) {
            final Node r = this.or();
            n = Prop4JUtils.createImplies(n, r);
        }
        return n;
    }

    private Node or() {
        Node n = this.and();
        while (this.symbol.equals(OR)) {
            final Node r = this.and();
            n = Prop4JUtils.createOr(n, r);
        }
        return n;
    }

    private Node and() {
        Node n = this.feature();
        while (this.symbol.equals(AND)) {
            final Node r = this.feature();
            n = Prop4JUtils.createAnd(n, r);
        }
        return n;
    }

    private Node feature() {
        this.nextSymbol();
        Node n = null;
        if (this.symbol.equals(NOT)) {
            final Node v = this.feature();
            n = Prop4JUtils.createNot(v);
        } else {
            final IFeature feature = this.fm.getFeature(this.symbol);
            n = Prop4JUtils.createLiteral(feature);
            this.nextSymbol();
        }
        return n;
    }

    private void nextSymbol() {
        if (this.index == this.input.length) {
            this.symbol = EOF;
        } else {
            this.symbol = this.input[this.index++];
        }
    }
}
