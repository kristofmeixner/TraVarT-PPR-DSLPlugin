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

	private static final String RESOURCE_ROOT = "resource";

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
		final Feature subroot = factory.createFeature(RESOURCE_ROOT);
		TraVarTUtils.setAbstract(subroot, true);
		TraVarTUtils.setGroup(fm, subroot, TraVarTUtils.getRoot(fm), Group.GroupType.MANDATORY);
		TraVarTUtils.addFeature(fm, subroot);
		for (final Resource resource : asq.getResources().values()) {
			final Feature feature = factory.createFeature(resource.getId());
			if (resource.isAbstract()) {
				TraVarTUtils.setAbstract(feature, true);
			}
			if (PprDslUtils.hasDeltaFileAttribute(resource)) {
				TraVarTUtils.addAttribute(feature, DELTA_FILE, PprDslUtils.getAttributeValue(resource, DELTA_FILE));

			}
			TraVarTUtils.addFeature(fm, feature);
			TraVarTUtils.setGroup(fm, feature, subroot,
					resource.isAbstract() ? Group.GroupType.MANDATORY : Group.GroupType.OPTIONAL);
		}
	}

	private static void deriveFeatureTree(final FeatureModel fm, final AssemblySequence asq) {
		for (final Resource resoruce : asq.getResources().values()) {
			if (PprDslUtils.implementsSingleResource(resoruce)) {
				final Feature childFeature = TraVarTUtils.getFeature(fm, resoruce.getId());
				final Resource parentResoruce = PprDslUtils.getFirstImplementedResource(resoruce);
				final Feature parentFeature = TraVarTUtils.getFeature(fm, parentResoruce.getId());
				TraVarTUtils.setGroup(fm, childFeature, parentFeature, Group.GroupType.OPTIONAL);
			}
		}
	}

	// TODO: similar to product transformation - Generalize
	private static void transformConstraints(final FeatureModel fm, final AssemblySequence asq) {
		for (final Resource resource : asq.getResources().values()) {
			// requires constraints
			createRequiresConstraints(fm, resource);
			// excludes constraints
			createExcludeConstraints(fm, resource);
		}
	}

	private static void createRequiresConstraints(final FeatureModel fm, final Resource resource) {
		final Feature conditionalFeature = TraVarTUtils.getFeature(fm, resource.getId());
		for (final Resource required : resource.getRequires()) {
			final Feature requiredFeature = TraVarTUtils.getFeature(fm, required.getId());
			if (requiredFeature != null && !TraVarTUtils.isParentOf(conditionalFeature, requiredFeature)) {
				TraVarTUtils.addOwnConstraint(fm,
						factory.createImplicationConstraint(
								factory.createLiteralConstraint(TraVarTUtils.getFeatureName(conditionalFeature)),
								factory.createLiteralConstraint(TraVarTUtils.getFeatureName(requiredFeature))));
			}
		}
	}

	private static void createExcludeConstraints(final FeatureModel fm, final Resource resource) {
		final Feature conditionalFeature = TraVarTUtils.getFeature(fm, resource.getId());
		for (final Resource excluded : resource.getExcludes()) {
			final Feature excludedFeature = TraVarTUtils.getFeature(fm, excluded.getId());
			if (excludedFeature != null && !TraVarTUtils.isParentOf(conditionalFeature, excludedFeature)) {
				TraVarTUtils.addOwnConstraint(fm, factory.createImplicationConstraint(
						factory.createLiteralConstraint(TraVarTUtils.getFeatureName(conditionalFeature)),
						factory.createNotConstraint(
								factory.createLiteralConstraint(TraVarTUtils.getFeatureName(excludedFeature)))));
			}
		}

	}
}
