package at.jku.cps.travart.plugin.ppr.dsl.parser;

import java.util.Objects;

import at.jku.cps.travart.core.helpers.TraVarTUtils;
import at.jku.cps.travart.plugin.ppr.dsl.exception.ParserException;
import at.sqi.ppr.model.AssemblySequence;
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
	private final AssemblySequence asq;

	public ConstraintDefinitionParser(final FeatureModel fm, final AssemblySequence asq) {
		this.fm = Objects.requireNonNull(fm);
		this.asq = Objects.requireNonNull(asq);
	}

	public de.vill.model.constraint.Constraint parse(final String str) throws ParserException {
		index = 0;
		input = TraVarTUtils.splitString(Objects.requireNonNull(str), REGEX);
		if (input.length > 0) {
			return parseConstraintNode();
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
			if (feature == null) {
				// at this point this may means that the product is non-abstract and could
				// indicate mandatory features. Replace by root feature if the product is not
				// abstract, otherwise by a null object
				if (!asq.getProducts().get(symbol).isAbstract()) {
					throw new ParserException(new NullPointerException(
							String.format("feature with identifier %s is not found (null)", symbol)));
				}
				n = new LiteralConstraint(TraVarTUtils.getFeatureName(TraVarTUtils.getRoot(fm)));
			} else {
				n = new LiteralConstraint(TraVarTUtils.getFeatureName(feature));
			}
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
