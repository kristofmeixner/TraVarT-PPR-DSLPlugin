package at.jku.cps.travart.plugin.ppr.dsl.transformation;

import at.jku.cps.travart.core.common.IModelTransformer;
import at.jku.cps.travart.core.exception.NotSupportedVariabilityTypeException;
import at.sqi.ppr.model.AssemblySequence;
import de.vill.model.FeatureModel;

public class ModelTransformerImpl implements IModelTransformer<AssemblySequence> {

	@Override
	public FeatureModel transform(final AssemblySequence assemblySequence, final String name,
			final OPTIMIZING_LEVEL level) throws NotSupportedVariabilityTypeException {
		final PprDslToFeatureModelTransformer pprDslToFeatureModelTransformer = new PprDslToFeatureModelTransformer();
		return pprDslToFeatureModelTransformer.transform(assemblySequence, name, level);
	}

	@Override
	public AssemblySequence transform(final FeatureModel featureModel, final String modelName,
			final OPTIMIZING_LEVEL level) throws NotSupportedVariabilityTypeException {
		final FeatureModelToPprDslTransformer featureModelToPprDslTransformer = new FeatureModelToPprDslTransformer();
		return featureModelToPprDslTransformer.transform(featureModel, level);
	}
}
