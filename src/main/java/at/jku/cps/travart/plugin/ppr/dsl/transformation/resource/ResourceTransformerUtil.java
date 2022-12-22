package at.jku.cps.travart.plugin.ppr.dsl.transformation.resource;

import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.DELTA_FILE;

import java.util.Objects;

import at.jku.cps.travart.core.exception.NotSupportedVariabilityTypeException;
import at.jku.cps.travart.core.factory.impl.CoreModelFactory;
import at.jku.cps.travart.core.helpers.TraVarTUtils;
import at.jku.cps.travart.core.optimize.DefaultCoreModelOptimizer;
import at.jku.cps.travart.plugin.ppr.dsl.common.PprDslUtils;
import at.sqi.ppr.model.AssemblySequence;
import at.sqi.ppr.model.resource.Resource;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.Group;

public class ResourceTransformerUtil {

	private ResourceTransformerUtil() {

	}

	private static CoreModelFactory factory;

	public static FeatureModel createResoruceFeatureModel(final CoreModelFactory coreFactory, final FeatureModel fm,
			final AssemblySequence asq) throws NotSupportedVariabilityTypeException {
		factory = Objects.requireNonNull(coreFactory);
		Objects.requireNonNull(fm);
		Objects.requireNonNull(asq);
		transformResources(fm, asq);
		deriveFeatureTree(fm, asq);
		transformConstraints(fm, asq);
		DefaultCoreModelOptimizer.getInstance().optimize(fm);
		return fm;
	}

	private static void transformResources(final FeatureModel fm, final AssemblySequence asq) {
		for (final Resource resource : asq.getResources().values()) {
			final Feature feature = factory.createFeature(resource.getId());
			if (resource.isAbstract()) {
				TraVarTUtils.setAbstract(feature, true);
			}
			if (PprDslUtils.hasDeltaFileAttribute(resource)) {
				TraVarTUtils.addAttribute(feature, DELTA_FILE, PprDslUtils.getAttributeValue(resource, DELTA_FILE));

			}
			TraVarTUtils.addFeature(fm, feature);
		}
	}

	private static void deriveFeatureTree(final FeatureModel fm, final AssemblySequence asq) {
		for (final Resource resoruce : asq.getResources().values()) {
			if (resoruce.isAbstract()) {
				final Feature feature = TraVarTUtils.getFeature(fm, resoruce.getId());
				TraVarTUtils.setGroup(fm, feature, feature.getParentFeature(), Group.GroupType.MANDATORY);
			}
			if (PprDslUtils.implementsSingleResource(resoruce)) {
				deriveFromImplementsAttribute(fm, resoruce);
			}

		}
	}

	private static void deriveFromImplementsAttribute(final FeatureModel fm, final Resource resoruce) {
		final Feature childFeature = TraVarTUtils.getFeature(fm, resoruce.getId());
		final Resource parentResoruce = PprDslUtils.getFirstImplementedResource(resoruce);
		final Feature parentFeature = TraVarTUtils.getFeature(fm, parentResoruce.getId());
		TraVarTUtils.setGroup(fm, childFeature, parentFeature, Group.GroupType.OPTIONAL);
	}

	private static void transformConstraints(final FeatureModel fm, final AssemblySequence asq) {
		// TODO Auto-generated method stub

	}
}
