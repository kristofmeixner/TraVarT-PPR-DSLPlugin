package at.jku.cps.travart.plugin.ppr.dsl.parser;

import java.util.Objects;

import at.jku.cps.travart.core.helpers.TraVarTUtils;
import at.jku.cps.travart.plugin.ppr.dsl.exception.ParserException;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.constraint.AndConstraint;
import de.vill.model.constraint.ImplicationConstraint;
import de.vill.model.constraint.LiteralConstraint;
import de.vill.model.constraint.NotConstraint;
import de.vill.model.constraint.OrConstraint;

public class ConstraintDefinitionParser {

	public static final String DELIMITER = ",";
	public static final String DEFINITION_ARROW = "->";
	public static final String IMPLIES = "implies";
	public static final String OR = "or";
	public static final String AND = "and";
	public static final String NOT = "not";
	private static final String REGEX = "(?<=,)|(?=,)|(?<=->)|(?=->)|((?<= implies )|(?= implies ))|((?<= or )|(?= or ))|((?<= and )|(?= and ))|((?<= not )|(?= not ))";
	private static final String EOF = "EOF";
	private final FeatureModel fm;
	private String[] input;
	private int index = 0;
	private String symbol;

	public ConstraintDefinitionParser(final FeatureModel fm) {
		this.fm = Objects.requireNonNull(fm);
	}

	public de.vill.model.constraint.Constraint parse(final String str) throws ParserException {
		Objects.requireNonNull(str);
		index = 0;
		input = TraVarTUtils.splitString(str, REGEX);
		if (input.length > 0) {
			TraVarTUtils.addGlobalConstraint(fm, parseConstraintNode());
		}
		throw new ParserException("No action found in String " + str);
	}

	private de.vill.model.constraint.Constraint parseConstraintNode() throws ParserException {
		de.vill.model.constraint.Constraint n = null;
		nextSymbol();
		while (!symbol.equals(EOF)) {
			if (symbol.equals(DELIMITER)) {
				nextSymbol();
				continue;
			}
			if (symbol.equals(DEFINITION_ARROW) || symbol.equals(IMPLIES) || symbol.equals(OR) || symbol.equals(AND)) {
				n = implies();
			}
			nextSymbol();
		}
		return n;
	}

	private de.vill.model.constraint.Constraint implies() {
		de.vill.model.constraint.Constraint n = or();
		while (symbol.equals(IMPLIES)) {
			final de.vill.model.constraint.Constraint r = or();
			n = new ImplicationConstraint(n, r);
		}
		return n;
	}

	private de.vill.model.constraint.Constraint or() {
		de.vill.model.constraint.Constraint n = and();
		while (symbol.equals(OR)) {
			final de.vill.model.constraint.Constraint r = and();
			n = new OrConstraint(n, r);
		}
		return n;
	}

	private de.vill.model.constraint.Constraint and() {
		de.vill.model.constraint.Constraint n = constraint();
		while (symbol.equals(AND)) {
			final de.vill.model.constraint.Constraint r = constraint();
			n = new AndConstraint(n, r);
		}
		return n;
	}

	private de.vill.model.constraint.Constraint constraint() {
		nextSymbol();
		de.vill.model.constraint.Constraint n = null;
		if (symbol.equals(NOT)) {
			final de.vill.model.constraint.Constraint v = constraint();
			n = new NotConstraint(v);
		} else {
			final Feature feature = TraVarTUtils.getFeature(fm, symbol);
			n = new LiteralConstraint(feature.getFeatureName());
			nextSymbol();
		}
		return n;
	}

	private void nextSymbol() {
		if (index == input.length) {
			symbol = EOF;
		} else {
			symbol = input[index++];
		}
	}
}
