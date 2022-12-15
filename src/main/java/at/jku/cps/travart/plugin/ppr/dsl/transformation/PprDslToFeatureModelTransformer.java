package at.jku.cps.travart.plugin.ppr.dsl.transformation;

import static at.jku.cps.travart.core.transformation.DefaultModelTransformationProperties.ABSTRACT_ATTRIBUTE;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.ATTRIBUTE_DEFAULT_VALUE_KEY_PRAEFIX;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.ATTRIBUTE_DESCRIPTION_KEY_PRAEFIX;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.ATTRIBUTE_ID_KEY_PRAEFIX;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.ATTRIBUTE_TYPE_KEY_PRAEFIX;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.ATTRIBUTE_UNIT_KEY_PRAEFIX;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.ATTRIBUTE_VALUE_KEY_PRAEFIX;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.CHILDREN_PRODUCTS_LIST_NAME_NR_;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.CHILDREN_PRODUCTS_LIST_SIZE;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.IMPLEMENTED_PRODUCTS_LIST_NAME_NR_;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.IMPLEMENTED_PRODUCTS_LIST_SIZE;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.NAME_ATTRIBUTE_KEY;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.logicng.formulas.FormulaFactory;

import at.jku.cps.travart.core.common.IModelTransformer.OPTIMIZING_LEVEL;
import at.jku.cps.travart.core.exception.NotSupportedVariabilityTypeException;
import at.jku.cps.travart.core.helpers.TraVarTUtils;
import at.jku.cps.travart.plugin.ppr.dsl.common.PprDslUtils;
import at.jku.cps.travart.plugin.ppr.dsl.parser.ConstraintDefinitionParser;
import at.sqi.ppr.model.AssemblySequence;
import at.sqi.ppr.model.NamedObject;
import at.sqi.ppr.model.constraint.Constraint;
import at.sqi.ppr.model.product.Product;
import at.sqi.ppr.model.vdi.product.IProduct;
import de.vill.model.Attribute;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.Group;
import de.vill.model.constraint.ImplicationConstraint;
import de.vill.model.constraint.LiteralConstraint;
import de.vill.model.constraint.NotConstraint;

public class PprDslToFeatureModelTransformer {
	private FeatureModel model;
	private final List<de.vill.model.constraint.Constraint> constraints = new ArrayList<>();

	public FeatureModel transform(final AssemblySequence asq, final String modelName, final OPTIMIZING_LEVEL level)
			throws NotSupportedVariabilityTypeException {
		model = new FeatureModel();
		transformProducts(asq);
		deriveFeatureTree(asq);
		transformConstraints(asq);
		final String rootName = TraVarTUtils.deriveFeatureModelRoot(model.getFeatureMap(), modelName);
		// the root feature is already a tree with all the features
		model.setRootFeature(model.getFeatureMap().get(rootName));

		// add constraints
		model.getOwnConstraints().addAll(constraints);

		optimizeFeatureModel();

		// return the final model
		return model;
	}

	private void transformProducts(final AssemblySequence asq) {
		for (final Product product : asq.getProducts().values()) {
			if (PprDslUtils.isPartialProduct(product)) {
				final Feature feature = new Feature(product.getId());
				if (product.isAbstract()) {
					feature.getAttributes().put(ABSTRACT_ATTRIBUTE, new Attribute<>(ABSTRACT_ATTRIBUTE, Boolean.TRUE));
				}
				storeNameAttributeAsProperty(feature, product.getName());
				storeChildrenAttributeAsProperty(feature, product.getChildren());
				for (final NamedObject attribute : product.getAttributes().values()) {
					storeCustomAttributesAsProperty(feature, attribute);
				}

				model.getFeatureMap().put(feature.getFeatureName(), feature);
			}
		}
	}

	private void deriveFeatureTree(final AssemblySequence asq) {
		for (final Product product : asq.getProducts().values()) {
			if (PprDslUtils.isPartialProduct(product)) {
				final Feature feature = model.getFeatureMap().get(product.getId());
				if (!product.getImplementedProducts().isEmpty() && product.getImplementedProducts().size() == 1) {
					final Product parentProduct = product.getImplementedProducts().get(0);
					final Feature parentFeature = model.getFeatureMap().get(parentProduct.getId());
					final Group optionalGroup = new Group(Group.GroupType.OPTIONAL);
					optionalGroup.getFeatures().add(feature);
					parentFeature.addChildren(optionalGroup);
					model.getFeatureMap().put(parentFeature.getFeatureName(), parentFeature);
				} else {
					// no tree can be derived from implemented products, but constraints for each of
					// the implemented products
					// (Product requires implemented products)
					for (final Product implemented : product.getImplementedProducts()) {
						final Feature impFeature = model.getFeatureMap().get(implemented.getId());
						assert impFeature != null;
						if (!TraVarTUtils.isParentOf(feature, impFeature)) {
							constraints.add(new ImplicationConstraint(new LiteralConstraint(feature.getFeatureName()),
									new LiteralConstraint(impFeature.getFeatureName())));
						}
					}
				}
				// store the implemented products also as attributes to restore them in the
				// roundtrip
				storeImplementedProductsAsProperties(feature, product.getImplementedProducts());
				// derive tree from children attribute
				if (!product.getChildProducts().isEmpty()) {
					final Group optionalGroup = new Group(Group.GroupType.OPTIONAL);
					for (final IProduct child : product.getChildProducts()) {
						final Product childProduct = (Product) child;
						final Feature childFeature = model.getFeatureMap().get(childProduct.getId());
						optionalGroup.getFeatures().add(childFeature);
					}
					feature.addChildren(optionalGroup);
					model.getFeatureMap().put(feature.getFeatureName(), feature);
				}
			}
			if (!PprDslUtils.isPartialProduct(product) && product.isAbstract()) {
				// if the dsl product is abstract the feature model e.g., mandatory features
				for (final Product required : product.getRequires()) {
					final Feature mandatoryFeature = model.getFeatureMap().get(required.getId());
					assert mandatoryFeature != null;
					final Feature parentFeature = mandatoryFeature.getParentFeature();
					mandatoryFeature.getParentGroup().getFeatures().remove(mandatoryFeature);
					final Group mandatoryGroup = new Group(Group.GroupType.MANDATORY);
					mandatoryGroup.getFeatures().add(mandatoryFeature);
					parentFeature.addChildren(mandatoryGroup);
					model.getFeatureMap().put(mandatoryFeature.getFeatureName(), mandatoryFeature);
					model.getFeatureMap().put(parentFeature.getFeatureName(), parentFeature);
				}
			}
		}
	}

	private void transformConstraints(final AssemblySequence asq) {
		for (final Product product : asq.getProducts().values()) {
			if (PprDslUtils.isPartialProduct(product)) {
				final Feature child = model.getFeatureMap().get(product.getId());
				assert child != null;
				// requires constraints
				for (final Product required : product.getRequires()) {
					final Feature parent = model.getFeatureMap().get(required.getId());
					if (parent != null && !TraVarTUtils.isParentOf(child, parent)) {
						constraints.add(new ImplicationConstraint(new LiteralConstraint(child.getFeatureName()),
								new LiteralConstraint(parent.getFeatureName())));
					}
				}
				// excludes constraints
				for (final Product excluded : product.getExcludes()) {
					final Feature parent = model.getFeatureMap().get(excluded.getId());
					if (parent != null && !TraVarTUtils.isParentOf(child, parent)) {
						constraints.add(new ImplicationConstraint(new LiteralConstraint(child.getFeatureName()),
								new NotConstraint(new LiteralConstraint(parent.getFeatureName()))));
					}
				}
			}
		}
		for (final Constraint constr : asq.getGlobalConstraints()) {
			final ConstraintDefinitionParser parser = new ConstraintDefinitionParser(model);
			final de.vill.model.constraint.Constraint constraint = parser.parse(constr.getDefinition());
			constraints.add(constraint);
		}
	}

	private void storeNameAttributeAsProperty(final Feature feature, final String name) {
		feature.getAttributes().put(NAME_ATTRIBUTE_KEY, new Attribute<>(NAME_ATTRIBUTE_KEY, name));
	}

	private void storeCustomAttributesAsProperty(final Feature feature, final NamedObject attribute) {
		feature.getAttributes().put(ATTRIBUTE_ID_KEY_PRAEFIX + attribute.getName(),
				new Attribute<>(ATTRIBUTE_ID_KEY_PRAEFIX + attribute.getName(), attribute.getName()));
		feature.getAttributes().put(ATTRIBUTE_DESCRIPTION_KEY_PRAEFIX + attribute.getName(),
				new Attribute<>(ATTRIBUTE_DESCRIPTION_KEY_PRAEFIX + attribute.getName(), attribute.getDescription()));
		feature.getAttributes().put(ATTRIBUTE_UNIT_KEY_PRAEFIX + attribute.getName(),
				new Attribute<>(ATTRIBUTE_UNIT_KEY_PRAEFIX + attribute.getName(), attribute.getUnit()));
		feature.getAttributes().put(ATTRIBUTE_TYPE_KEY_PRAEFIX + attribute.getName(),
				new Attribute<>(ATTRIBUTE_TYPE_KEY_PRAEFIX + attribute.getName(), attribute.getType()));
		feature.getAttributes().put(ATTRIBUTE_DEFAULT_VALUE_KEY_PRAEFIX + attribute.getName(),
				new Attribute<>(ATTRIBUTE_DEFAULT_VALUE_KEY_PRAEFIX + attribute.getName(),
						attribute.getDefaultValueObject().toString()));
		feature.getAttributes().put(ATTRIBUTE_VALUE_KEY_PRAEFIX + attribute.getName(), new Attribute<>(
				ATTRIBUTE_VALUE_KEY_PRAEFIX + attribute.getName(), attribute.getValueObject().toString()));
	}

	private void storeImplementedProductsAsProperties(final Feature feature, final List<Product> implementedProducts) {
		// store the size of implemented products
		feature.getAttributes().put(IMPLEMENTED_PRODUCTS_LIST_SIZE,
				new Attribute<>(IMPLEMENTED_PRODUCTS_LIST_SIZE, implementedProducts.size()));
		// store the names of the implemented products
		for (int i = 0; i < implementedProducts.size(); i++) {
			feature.getAttributes().put(IMPLEMENTED_PRODUCTS_LIST_NAME_NR_ + i,
					new Attribute<>(IMPLEMENTED_PRODUCTS_LIST_NAME_NR_ + i, implementedProducts.get(i).getId()));
		}
	}

	private void storeChildrenAttributeAsProperty(final Feature feature, final List<Product> childProducts) {
		// store the size of child products
		feature.getAttributes().put(CHILDREN_PRODUCTS_LIST_SIZE,
				new Attribute<>(CHILDREN_PRODUCTS_LIST_SIZE, childProducts.size()));
		// store the names of the implemented products
		for (int i = 0; i < childProducts.size(); i++) {
			feature.getAttributes().put(CHILDREN_PRODUCTS_LIST_NAME_NR_ + i,
					new Attribute<>(CHILDREN_PRODUCTS_LIST_NAME_NR_ + i, childProducts.get(i).getId()));
		}
	}

	private void optimizeFeatureModel() { // final AssemblySequence asq
		// find mandatory features within feature groups
		fixFalseOptionalFeaturesByFeatureGroupConstraints(model.getRootFeature());
		// find mandatory features within abstract feature groups
		fixFalseOptionalFeaturesByAbstractFeatureGroup(model.getRootFeature());
		// find mandatory features within requires constraints
		fixFalseOptionalFeaturesByConstraints();
		// find alternative groups
		transformConstraintsToAlternativeGroup(model.getRootFeature());
	}

	private void fixFalseOptionalFeaturesByConstraints() {
		final Set<de.vill.model.constraint.Constraint> toDelete = new HashSet<>();
		for (final de.vill.model.constraint.Constraint constr : model.getConstraints()) {
			final de.vill.model.constraint.Constraint cnf = TraVarTUtils.buildConstraintFromFormula(
					TraVarTUtils.buildFormulaFromConstraint(constr, new FormulaFactory()).cnf());

			if (TraVarTUtils.isRequires(constr)) {
				final de.vill.model.constraint.Constraint left = TraVarTUtils.getLeftConstraint(cnf);
				final de.vill.model.constraint.Constraint right = TraVarTUtils.getRightConstraint(cnf);
				if (left != null && right != null && TraVarTUtils.isLiteral(left) && TraVarTUtils.isLiteral(right)) {
					final Feature leftFeature = ((LiteralConstraint) left).getFeature();
					final Feature rightFeature = ((LiteralConstraint) right).getFeature();

					if (leftFeature.getParentGroup() != null
							&& Group.GroupType.MANDATORY.equals(leftFeature.getParentGroup().GROUPTYPE)
							&& rightFeature.getParentGroup() != null
							&& !Group.GroupType.MANDATORY.equals(rightFeature.getParentGroup().GROUPTYPE)) {
						rightFeature.getParentGroup().getFeatures().remove(rightFeature);
						final Group mandatoryGroup = new Group(Group.GroupType.MANDATORY);
						mandatoryGroup.getFeatures().add(rightFeature);
						rightFeature.getParentFeature().addChildren(mandatoryGroup);
						model.getFeatureMap().put(rightFeature.getFeatureName(), rightFeature);
						model.getFeatureMap().put(rightFeature.getParentFeature().getFeatureName(),
								rightFeature.getParentFeature());
						toDelete.add(constr);
					}

				}
			}
		}
		toDelete.forEach(constraints::remove);
	}

	private void fixFalseOptionalFeaturesByAbstractFeatureGroup(final Feature feature) {
		// TODO: think about multiple getChildren calls
		// TODO: check if it works
		final Set<Feature> children = TraVarTUtils.getChildren(feature);
		for (final Feature child : children) {
			fixFalseOptionalFeaturesByAbstractFeatureGroup(child);
		}
		if (children.size() > 0 && TraVarTUtils.isAbstract(feature)
				&& (TraVarTUtils.checkGroupType(feature, Group.GroupType.OR)
						|| TraVarTUtils.checkGroupType(feature, Group.GroupType.ALTERNATIVE))) {
			// abstract features where all child features are mandatory are also mandatory
			boolean childMandatory = true;
			for (final Feature childFeature : children) {
				childMandatory = childMandatory && childFeature.getParentGroup() != null
						&& Group.GroupType.MANDATORY.equals(childFeature.getParentGroup().GROUPTYPE);
			}
			if (childMandatory) {
				feature.getParentGroup().getFeatures().remove(feature);
				final Group mandatoryGroup = new Group(Group.GroupType.MANDATORY);
				mandatoryGroup.getFeatures().add(feature);
				feature.getParentFeature().addChildren(mandatoryGroup);
				model.getFeatureMap().put(feature.getFeatureName(), feature);
				model.getFeatureMap().put(feature.getParentFeature().getFeatureName(), feature.getParentFeature());
			}
		}
	}

	private void fixFalseOptionalFeaturesByFeatureGroupConstraints(final Feature feature) {
		final Set<Feature> children = TraVarTUtils.getChildren(feature);
		for (final Feature child : children) {
			fixFalseOptionalFeaturesByFeatureGroupConstraints(child);
		}
		// if there is a requires constraint in the feature model between parent and
		// child, we can remove the constraint and make the child mandatory
		for (final Feature childFeature : children) {
			final de.vill.model.constraint.Constraint requiredConstraint = new ImplicationConstraint(
					new LiteralConstraint(feature.getFeatureName()),
					new LiteralConstraint(childFeature.getFeatureName()));
			final List<de.vill.model.constraint.Constraint> relevant = model.getConstraints().stream()
					.filter(constr -> constr.equals(requiredConstraint)).collect(Collectors.toList());
			if (relevant.size() == 1) {
				childFeature.getParentGroup().getFeatures().remove(childFeature);
				final Group mandatoryGroup = new Group(Group.GroupType.MANDATORY);
				mandatoryGroup.getFeatures().add(childFeature);
				childFeature.getParentFeature().addChildren(mandatoryGroup);
				model.getFeatureMap().put(childFeature.getFeatureName(), childFeature);
				model.getFeatureMap().put(childFeature.getParentFeature().getFeatureName(),
						childFeature.getParentFeature());
				constraints.remove(relevant.get(0));
			}
		}
	}

	private void transformConstraintsToAlternativeGroup(final Feature feature) {
		final Set<Feature> children = TraVarTUtils.getChildren(feature);
		final int childCount = children.size();
		if (childCount > 0) {
			final List<de.vill.model.constraint.Constraint> constraints = model.getConstraints();
			final Set<de.vill.model.constraint.Constraint> relevantExcludesConstraints = new HashSet<>();
			for (final Feature childFeature : children) {
				transformConstraintsToAlternativeGroup(childFeature);
				final Set<Feature> otherChildren = new HashSet<>(children);
				otherChildren.remove(childFeature);
				for (final de.vill.model.constraint.Constraint constr : constraints) {
					if (TraVarTUtils.isExcludes(constr) && constr.getConstraintSubParts().contains(childFeature)
							&& constr.getConstraintSubParts().stream().anyMatch(otherChildren::contains)) {
						for (final Feature other : otherChildren) {
							final de.vill.model.constraint.Constraint constraint = new ImplicationConstraint(
									new LiteralConstraint(childFeature.getFeatureName()),
									new LiteralConstraint(other.getFeatureName()));
							if (constr.equals(constraint)) {
								relevantExcludesConstraints.add(constr);
							}
						}

					}
				}
			}
			if (!relevantExcludesConstraints.isEmpty()
					// is <= really correct? we do not want to remove to much constraints
					&& childCount * (childCount - 1) <= relevantExcludesConstraints.size()) {
				feature.getParentGroup().getFeatures().remove(feature);
				final Group alternativeGroup = new Group(Group.GroupType.ALTERNATIVE);
				alternativeGroup.getFeatures().add(feature);
				feature.getParentFeature().addChildren(alternativeGroup);
				model.getFeatureMap().put(feature.getFeatureName(), feature);
				model.getFeatureMap().put(feature.getParentFeature().getFeatureName(), feature.getParentFeature());
				relevantExcludesConstraints.forEach(constraints::remove);
			}
		}
	}
}
