package at.jku.cps.travart.plugin.ppr.dsl.parser;

import at.jku.cps.travart.core.helpers.TraVarTUtils;
import at.jku.cps.travart.plugin.ppr.dsl.exception.ParserException;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.constraint.AndConstraint;
import de.vill.model.constraint.ImplicationConstraint;
import de.vill.model.constraint.LiteralConstraint;
import de.vill.model.constraint.NotConstraint;
import de.vill.model.constraint.OrConstraint;

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
    private final FeatureModel model;
    private String[] input;
    private int index = 0;
    private String symbol;

    public ConstraintDefinitionParser(final FeatureModel model) {
        this.model = Objects.requireNonNull(model);
    }

    public de.vill.model.constraint.Constraint parse(final String str) throws ParserException {
        Objects.requireNonNull(str);
        this.index = 0;
        this.input = TraVarTUtils.splitString(str, REGEX);
        if (this.input.length > 0) {
            this.model.getConstraints().add(this.parseConstraintNode());
        }
        throw new ParserException("No action found in String " + str);
    }

    private de.vill.model.constraint.Constraint parseConstraintNode() throws ParserException {
        de.vill.model.constraint.Constraint n = null;
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

    private de.vill.model.constraint.Constraint implies() {
        de.vill.model.constraint.Constraint n = this.or();
        while (this.symbol.equals(IMPLIES)) {
            final de.vill.model.constraint.Constraint r = this.or();
            n = new ImplicationConstraint(n, r);
        }
        return n;
    }

    private de.vill.model.constraint.Constraint or() {
        de.vill.model.constraint.Constraint n = this.and();
        while (this.symbol.equals(OR)) {
            final de.vill.model.constraint.Constraint r = this.and();
            n = new OrConstraint(n, r);
        }
        return n;
    }

    private de.vill.model.constraint.Constraint and() {
        de.vill.model.constraint.Constraint n = this.constraint();
        while (this.symbol.equals(AND)) {
            final de.vill.model.constraint.Constraint r = this.constraint();
            n = new AndConstraint(n, r);
        }
        return n;
    }

    private de.vill.model.constraint.Constraint constraint() {
        this.nextSymbol();
        de.vill.model.constraint.Constraint n = null;
        if (this.symbol.equals(NOT)) {
            final de.vill.model.constraint.Constraint v = this.constraint();
            n = new NotConstraint(v);
        } else {
            final Feature feature = this.model.getFeatureMap().get(this.symbol);
            n = new LiteralConstraint(feature.getFeatureName());
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
