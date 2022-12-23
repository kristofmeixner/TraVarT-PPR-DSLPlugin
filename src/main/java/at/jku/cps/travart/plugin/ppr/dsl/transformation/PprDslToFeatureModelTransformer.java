package at.jku.cps.travart.plugin.ppr.dsl.transformation;

import java.util.Set;

import at.jku.cps.travart.core.exception.NotSupportedVariabilityTypeException;
import at.jku.cps.travart.core.factory.impl.CoreModelFactory;
import at.jku.cps.travart.core.helpers.TraVarTUtils;
import at.jku.cps.travart.core.optimize.DefaultCoreModelOptimizer;
import at.jku.cps.travart.plugin.ppr.dsl.exception.ParserException;
import at.jku.cps.travart.plugin.ppr.dsl.parser.ConstraintDefinitionParser;
import at.jku.cps.travart.plugin.ppr.dsl.transformation.process.ProcessTransformerUtil;
import at.jku.cps.travart.plugin.ppr.dsl.transformation.product.ProductTransformerUtil;
import at.jku.cps.travart.plugin.ppr.dsl.transformation.resource.ResourceTransformerUtil;
import at.sqi.ppr.model.AssemblySequence;
import at.sqi.ppr.model.constraint.Constraint;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.Group;
import de.vill.model.constraint.LiteralConstraint;

public class PprDslToFeatureModelTransformer {

	private static final CoreModelFactory factory = CoreModelFactory.getInstance();

	public FeatureModel transform(final AssemblySequence asq, final String modelName)
			throws NotSupportedVariabilityTypeException {
		FeatureModel fm = factory.create();
		final Feature root = factory.createFeature(modelName);
		TraVarTUtils.addFeature(fm, root);
		TraVarTUtils.setAbstract(root, true);
		TraVarTUtils.setRoot(fm, root);
		ProductTransformerUtil.createProductFeatureModel(factory, fm, asq);
		ResourceTransformerUtil.createResoruceFeatureModel(factory, fm, asq);
		ProcessTransformerUtil.createProcessFeatureModel(factory, fm, asq);
		createGlobalConstraints(fm, asq);
		DefaultCoreModelOptimizer.getInstance().optimize(fm);
		return fm;
	}

	private static void createGlobalConstraints(final FeatureModel fm, final AssemblySequence asq) {
		for (final Constraint constr : asq.getGlobalConstraints()) {
			try {
				final ConstraintDefinitionParser parser = new ConstraintDefinitionParser(fm, asq);
				final de.vill.model.constraint.Constraint constraint = parser.parse(constr.getDefinition());
				// if the left side is the root feature it is a mandatory constraint
				if (isMandatoryRootConstraint(fm, constraint)) {
					Set<de.vill.model.constraint.Constraint> literals = TraVarTUtils
							.getLiterals(TraVarTUtils.getRightConstraint(constraint));
					for (de.vill.model.constraint.Constraint lit : literals) {
						if (lit instanceof LiteralConstraint) {
							Feature f = TraVarTUtils.getFeature(fm, ((LiteralConstraint) lit).getLiteral());
							TraVarTUtils.setGroup(fm, f, f.getParentFeature(), Group.GroupType.MANDATORY);
						}
					}
				} else {
					TraVarTUtils.addOwnConstraint(fm, constraint);
				}
			} catch (ParserException e) {
				// TODO: find better way then using the exception
			}
		}
	}

	private static boolean isMandatoryRootConstraint(final FeatureModel fm,
			final de.vill.model.constraint.Constraint constraint) {
		de.vill.model.constraint.Constraint left = TraVarTUtils.getLeftConstraint(constraint);
		if (!(left instanceof LiteralConstraint)) {
			return false;
		}
		return TraVarTUtils.isRoot(TraVarTUtils.getFeature(fm, ((LiteralConstraint) left).getLiteral()));
	}
}
