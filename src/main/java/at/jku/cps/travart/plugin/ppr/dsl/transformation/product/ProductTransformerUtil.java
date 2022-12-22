package at.jku.cps.travart.plugin.ppr.dsl.transformation.product;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import at.jku.cps.travart.core.exception.NotSupportedVariabilityTypeException;
import at.jku.cps.travart.core.factory.impl.CoreModelFactory;
import at.jku.cps.travart.core.helpers.TraVarTUtils;
import at.jku.cps.travart.core.optimize.DefaultCoreModelOptimizer;
import at.jku.cps.travart.plugin.ppr.dsl.common.PprDslUtils;
import at.sqi.ppr.model.AssemblySequence;
import at.sqi.ppr.model.product.Product;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.Group;

public class ProductTransformerUtil {

	private static final String PRODUCT_ROOT = "product";

	private ProductTransformerUtil() {

	}

	private static CoreModelFactory factory;

	public static void createProductFeatureModel(final CoreModelFactory coreFactory, final FeatureModel fm,
			final AssemblySequence asq) throws NotSupportedVariabilityTypeException {
		factory = Objects.requireNonNull(coreFactory);
		Objects.requireNonNull(fm);
		Objects.requireNonNull(asq);
		transformProducts(fm, asq);
		deriveFeatureTree(fm, asq);
		transformConstraints(fm, asq);
		DefaultCoreModelOptimizer.getInstance().optimize(fm);
	}

	private static void transformProducts(final FeatureModel fm, final AssemblySequence asq) {
		final Feature parent = factory.createFeature(PRODUCT_ROOT);
		TraVarTUtils.addFeature(fm, parent);
		TraVarTUtils.setRoot(fm, parent);
		for (final Product product : asq.getProducts().values()) {
			if (PprDslUtils.isPartialProduct(product)) {
				final Feature feature = factory.createFeature(product.getId());
				if (product.isAbstract()) {
					TraVarTUtils.setAbstract(feature, true);
				}
				TraVarTUtils.setGroup(fm, feature, parent, Group.GroupType.OPTIONAL);
			}
		}
	}

	private static void deriveFeatureTree(final FeatureModel fm, final AssemblySequence asq) {
		for (final Product product : asq.getProducts().values()) {
			if (PprDslUtils.isPartialProduct(product)) {
				if (PprDslUtils.implementsSingleProduct(product)) {
					deriveFromImplementsAttribute(fm, product);
				} else if (PprDslUtils.hasChildren(product)) {
					deriveFromChildrenAttribute(fm, product);
				}
				deriveConstraintsFromImplementedProducts(fm, product);

			} else if (product.isAbstract()) {
				// for non-partial, abstract products, the required partial products are
				// mandatory
				for (final Product required : product.getRequires()) {
					if (PprDslUtils.isPartialProduct(required)) {
						final Feature requiredFeature = TraVarTUtils.getFeature(fm, required.getId());
						TraVarTUtils.setGroup(fm, requiredFeature, requiredFeature.getParentFeature(),
								Group.GroupType.MANDATORY);
					}
				}
			}
		}
	}

	private static void deriveFromImplementsAttribute(final FeatureModel fm, final Product childProduct) {
		final Feature childFeature = TraVarTUtils.getFeature(fm, childProduct.getId());
		final Product parentProduct = PprDslUtils.getFirstImplementedProduct(childProduct);
		final Feature parentFeature = TraVarTUtils.getFeature(fm, parentProduct.getId());
		TraVarTUtils.setGroup(fm, childFeature, parentFeature, Group.GroupType.OPTIONAL);
	}

	private static void deriveConstraintsFromImplementedProducts(final FeatureModel fm, final Product product) {
		final Feature feature = TraVarTUtils.getFeature(fm, product.getId());
		for (final Product implemented : product.getImplementedProducts()) {
			final Feature impFeature = TraVarTUtils.getFeature(fm, implemented.getId());
			if (!TraVarTUtils.isParentOf(feature, impFeature)) {
				TraVarTUtils.addOwnConstraint(fm,
						factory.createImplicationConstraint(
								factory.createLiteralConstraint(TraVarTUtils.getFeatureName(feature)),
								factory.createLiteralConstraint(TraVarTUtils.getFeatureName(impFeature))));
			}
		}
	}

	private static void deriveFromChildrenAttribute(final FeatureModel fm, final Product product) {
		final Feature feature = TraVarTUtils.getFeature(fm, product.getId());
		Set<Feature> childFeatures = new HashSet<>();
		product.getChildProducts().forEach(p -> childFeatures.add(TraVarTUtils.getFeature(fm, ((Product) p).getId())));
		TraVarTUtils.addGroup(fm, childFeatures, feature, Group.GroupType.OPTIONAL);
	}

	private static void transformConstraints(final FeatureModel fm, final AssemblySequence asq) {
		for (final Product product : asq.getProducts().values()) {
			if (PprDslUtils.isPartialProduct(product)) {
				// requires constraints
				createRequiresConstraints(fm, product);
				// excludes constraints
				createExcludeConstraints(fm, product);
			}
		}
	}

	private static void createRequiresConstraints(final FeatureModel fm, final Product product) {
		final Feature conditionalFeature = TraVarTUtils.getFeature(fm, product.getId());
		for (final Product required : product.getRequires()) {
			final Feature requiredFeature = TraVarTUtils.getFeature(fm, required.getId());
			if (requiredFeature != null && !TraVarTUtils.isParentOf(conditionalFeature, requiredFeature)) {
				TraVarTUtils.addOwnConstraint(fm,
						factory.createImplicationConstraint(
								factory.createLiteralConstraint(TraVarTUtils.getFeatureName(conditionalFeature)),
								factory.createLiteralConstraint(TraVarTUtils.getFeatureName(requiredFeature))));
			}
		}
	}

	private static void createExcludeConstraints(final FeatureModel fm, final Product product) {
		final Feature conditionalFeature = TraVarTUtils.getFeature(fm, product.getId());
		for (final Product excluded : product.getExcludes()) {
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
