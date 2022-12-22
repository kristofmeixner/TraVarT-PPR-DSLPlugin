package at.jku.cps.travart.plugin.ppr.dsl.transformation;

import at.jku.cps.travart.core.exception.NotSupportedVariabilityTypeException;
import at.jku.cps.travart.core.factory.impl.CoreModelFactory;
import at.jku.cps.travart.core.helpers.TraVarTUtils;
import at.jku.cps.travart.plugin.ppr.dsl.parser.ConstraintDefinitionParser;
import at.jku.cps.travart.plugin.ppr.dsl.transformation.product.ProductTransformerUtil;
import at.sqi.ppr.model.AssemblySequence;
import at.sqi.ppr.model.constraint.Constraint;
import de.vill.model.FeatureModel;

public class PprDslToFeatureModelTransformer {

	private static final CoreModelFactory factory = CoreModelFactory.getInstance();

	public FeatureModel transform(final AssemblySequence asq, final String modelName)
			throws NotSupportedVariabilityTypeException {
		FeatureModel fm = factory.create();
		ProductTransformerUtil.createProductFeatureModel(factory, fm, asq);
//		ResourceTransformerUtil.createResoruceFeatureModel(factory, fm, asq);
//		ProcessTransformerUtil.createProcessFeatureModel(factory, fm, asq);
//		createGlobalConstraints(fm, asq);
//		DefaultCoreModelOptimizer.getInstance().optimize(fm);
		return fm;
	}

	private static void createGlobalConstraints(final FeatureModel fm, final AssemblySequence asq) {
		for (final Constraint constr : asq.getGlobalConstraints()) {
			final ConstraintDefinitionParser parser = new ConstraintDefinitionParser(fm);
			final de.vill.model.constraint.Constraint constraint = parser.parse(constr.getDefinition());
			TraVarTUtils.addOwnConstraint(fm, constraint);
		}
	}
}
