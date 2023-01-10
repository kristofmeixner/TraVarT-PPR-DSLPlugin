package at.jku.cps.travart.plugin.ppr.dsl.transformation;

import at.jku.cps.travart.core.common.IModelTransformer;
import at.jku.cps.travart.core.exception.NotSupportedVariabilityTypeException;
import at.sqi.ppr.model.AssemblySequence;
import de.vill.model.FeatureModel;

public class PprModelTransformerImpl implements IModelTransformer<AssemblySequence> {

	@Override
	public FeatureModel transform(final AssemblySequence assemblySequence, final String name,
			final TRANSFORMATION_LEVEL level) throws NotSupportedVariabilityTypeException {
		try {
			if (level == TRANSFORMATION_LEVEL.ROUNDTRIP) {
				final PprDslToFeatureModelRoundtripTransformer pprDslToFeatureModelRoundtripTransformer = new PprDslToFeatureModelRoundtripTransformer();
				return pprDslToFeatureModelRoundtripTransformer.transform(assemblySequence, name);
			}
			final PprDslToFeatureModelTransformer pprDslToFeatureModelTransformer = new PprDslToFeatureModelTransformer();

			return pprDslToFeatureModelTransformer.transform(assemblySequence, name);
		} catch (NotSupportedVariabilityTypeException | ReflectiveOperationException e) {
			throw new NotSupportedVariabilityTypeException(e);
		}
	}

	@Override
	public AssemblySequence transform(final FeatureModel featureModel, final String name,
			final TRANSFORMATION_LEVEL level) throws NotSupportedVariabilityTypeException {
		try {
			if (level == TRANSFORMATION_LEVEL.ROUNDTRIP) {
				final FeatureModelToPprDslRoundtripTransformer featureModelToPprDslRoundtripTransformer = new FeatureModelToPprDslRoundtripTransformer();
				return featureModelToPprDslRoundtripTransformer.transform(featureModel);
			}
			final FeatureModelToPprDslTransformer featureModelToPprDslTransformer = new FeatureModelToPprDslTransformer();
			return featureModelToPprDslTransformer.transform(featureModel);
		} catch (NotSupportedVariabilityTypeException | ReflectiveOperationException e) {
			throw new NotSupportedVariabilityTypeException(e);
		}
	}
}
