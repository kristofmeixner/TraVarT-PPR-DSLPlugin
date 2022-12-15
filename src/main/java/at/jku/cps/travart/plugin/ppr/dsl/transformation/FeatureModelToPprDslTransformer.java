package at.jku.cps.travart.plugin.ppr.dsl.transformation;

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
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.PARTIAL_PRODUCT_ATTRIBUTE;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.PARTIAL_PRODUCT_DEFAULT_VALUE;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.PARTIAL_PRODUCT_DESCRIPTION;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.PARTIAL_PRODUCT_TYPE;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.logicng.formulas.FormulaFactory;

import at.jku.cps.travart.core.common.IConfigurable;
import at.jku.cps.travart.core.common.IModelTransformer.OPTIMIZING_LEVEL;
import at.jku.cps.travart.core.exception.NotSupportedVariabilityTypeException;
import at.jku.cps.travart.core.helpers.TraVarTUtils;
import at.jku.cps.travart.plugin.ppr.dsl.common.PprDslUtils;
import at.jku.cps.travart.plugin.ppr.dsl.exception.NotSupportedConstraintType;
import at.jku.cps.travart.plugin.ppr.dsl.parser.ConstraintDefinitionParser;
import at.sqi.ppr.dsl.reader.constants.DslConstants;
import at.sqi.ppr.model.AssemblySequence;
import at.sqi.ppr.model.NamedObject;
import at.sqi.ppr.model.constraint.Constraint;
import at.sqi.ppr.model.product.Product;
import de.vill.model.Attribute;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.Group;
import de.vill.model.constraint.AndConstraint;
import de.vill.model.constraint.ImplicationConstraint;
import de.vill.model.constraint.LiteralConstraint;
import de.vill.model.constraint.NotConstraint;
import de.vill.model.constraint.OrConstraint;

public class FeatureModelToPprDslTransformer {
	private AssemblySequence asq;
	private Set<Map<IConfigurable, Boolean>> samples;

//    public AssemblySequence transform(final FeatureModel model, final Set<Map<IConfigurable, Boolean>> samples)
//            throws NotSupportedVariabilityTypeException {
//        this.samples = samples;
//        return this.transform(model);
//    }

	public AssemblySequence transform(final FeatureModel model, final OPTIMIZING_LEVEL level)
			throws NotSupportedVariabilityTypeException {
		try {
			asq = new AssemblySequence();
			convertFeature(model.getRootFeature());
			restoreAttributes(model.getRootFeature());
			restoreAttributesFromFeatureTree(model.getRootFeature());
			convertConstraints(model.getConstraints());
			deriveProductsFromSamples(model);
			return asq;
		} catch (final NotSupportedConstraintType e) {
			throw new NotSupportedVariabilityTypeException(e);
		}
	}

	private void convertFeature(final Feature feature) throws NotSupportedVariabilityTypeException {
		final Product product = new Product();
		product.setId(feature.getFeatureName());
		product.setName(restoreNameFromProperties(feature, product));
		product.setAbstract(TraVarTUtils.isAbstract(feature));
		restoreAttributesFromProperties(feature, product);
		addPartialProductAttribute(product);
		PprDslUtils.addProduct(asq, product);

		for (final Feature child : TraVarTUtils.getChildren(feature)) {
			convertFeature(child);
		}
	}

	private void addPartialProductAttribute(final Product product) {
		if (!PprDslUtils.isPartialProduct(product)) {
			NamedObject attribute = asq.getDefinedAttributes().get(PARTIAL_PRODUCT_ATTRIBUTE);
			if (attribute == null) {
				attribute = asq.getProductAttributes().get(PARTIAL_PRODUCT_ATTRIBUTE);
			}
			if (attribute == null) {
				attribute = new NamedObject();
				attribute.setName(PARTIAL_PRODUCT_ATTRIBUTE);
				attribute.setType(PARTIAL_PRODUCT_TYPE);
				attribute.setDescription(PARTIAL_PRODUCT_DESCRIPTION);
				attribute.setDefaultValue(PARTIAL_PRODUCT_DEFAULT_VALUE);
				asq.getDefinedAttributes().put(PARTIAL_PRODUCT_ATTRIBUTE, attribute);
				asq.getProductAttributes().put(PARTIAL_PRODUCT_ATTRIBUTE, attribute);
			}
			attribute.setValue("true");
			product.getAttributes().put(PARTIAL_PRODUCT_ATTRIBUTE, attribute);
		}
	}

	private void restoreAttributes(final Feature feature) {
		final Product product = PprDslUtils.getProduct(asq, feature.getFeatureName());
		assert product != null;
		restoreChildrenListOfProducts(feature, product);
		restoreImplementsListOfProducts(feature, product);
		for (final Feature child : TraVarTUtils.getChildren(feature)) {
			restoreAttributes(child);
		}
	}

	private String restoreNameFromProperties(final Feature feature, final Product product) {
		final Attribute nameAttribute = feature.getAttributes().get(NAME_ATTRIBUTE_KEY);
		if (nameAttribute == null) {
			return product.getId();
		}

		return nameAttribute.getValue().toString();
	}

	private void restoreAttributesFromProperties(final Feature feature, final Product product) {
		final List<String> attributeNames = feature.getAttributes().keySet().stream()
				.filter(entry -> entry.startsWith(ATTRIBUTE_ID_KEY_PRAEFIX))
				.map(entry -> entry.substring(ATTRIBUTE_ID_KEY_PRAEFIX.length())).collect(Collectors.toList());

		for (final String attributeName : attributeNames) {
			final NamedObject attribute = new NamedObject();
			attribute.setName(attributeName);

			attribute.setEntityType(DslConstants.ATTRIBUTE_ENTITY);

			final Object descriptionObj = TraVarTUtils.getAttributeValue(feature,
					ATTRIBUTE_DESCRIPTION_KEY_PRAEFIX + attributeName);
			if (descriptionObj != null) {
				attribute.setDescription(descriptionObj.toString());
			}

			final Object unitObj = TraVarTUtils.getAttributeValue(feature, ATTRIBUTE_UNIT_KEY_PRAEFIX + attributeName);
			if (unitObj != null) {
				attribute.setDescription(descriptionObj.toString());
			}

			final Object typeObj = TraVarTUtils.getAttributeValue(feature, ATTRIBUTE_TYPE_KEY_PRAEFIX + attributeName);
			if (typeObj != null) {
				attribute.setType(typeObj.toString());

				switch (typeObj.toString().toLowerCase()) {
				case "number":
					final Double defaultValue = Double.parseDouble(
							TraVarTUtils.getAttributeValue(feature, ATTRIBUTE_DEFAULT_VALUE_KEY_PRAEFIX + attributeName)
									.toString());
					attribute.setDefaultValue(defaultValue);
					final Object valueObj = TraVarTUtils.getAttributeValue(feature,
							ATTRIBUTE_VALUE_KEY_PRAEFIX + attribute.getName());
					if (valueObj != null) {
						final Double value = Double.parseDouble(valueObj.toString());
						attribute.setValue(value);
					}
					break;
				case "string":
					attribute.setDefaultValue(
							TraVarTUtils.getAttributeValue(feature, ATTRIBUTE_DEFAULT_VALUE_KEY_PRAEFIX + attributeName)
									.toString());
					attribute.setValue(
							TraVarTUtils.getAttributeValue(feature, ATTRIBUTE_VALUE_KEY_PRAEFIX + attribute.getName()));
					break;
				}
			}
			product.getAttributes().put(attributeName, attribute);
			asq.getDefinedAttributes().put(attributeName, attribute);
			asq.getProductAttributes().put(attributeName, attribute);
		}
	}

	private void restoreAttributesFromFeatureTree(final Feature feature) {
		// if parent feature is a product, it is an implements relation
		if (feature.getParentFeature() != null && TraVarTUtils.isAbstract(feature.getParentFeature())) {
			final Product parentProduct = PprDslUtils.getProduct(asq, feature.getParentFeature().getFeatureName());
			final Product childProduct = PprDslUtils.getProduct(asq, feature.getFeatureName());
			if (!childProduct.getImplementedProducts().contains(parentProduct)) {
				childProduct.getImplementedProducts().add(parentProduct);
			}
		}
		final Set<Feature> children = TraVarTUtils.getChildren(feature);
		// if it is an alternative group the excludes constraints have to be derived
		if (TraVarTUtils.checkGroupType(feature, Group.GroupType.ALTERNATIVE)) {
			for (final Feature childFeature : children) {
				final Set<Feature> remChildren = new HashSet<>(children);
				remChildren.remove(childFeature);
				final Product childProduct = PprDslUtils.getProduct(asq, childFeature.getFeatureName());
				for (final Feature other : remChildren) {
					final Product otherProduct = PprDslUtils.getProduct(asq, other.getFeatureName());
					childProduct.getExcludes().add(otherProduct);
				}
			}
		}

		for (final Feature child : children) {
			restoreAttributesFromFeatureTree(child);
		}
	}

	private void restoreImplementsListOfProducts(final Feature feature, final Product product) {
		final Object sizeObj = TraVarTUtils.getAttributeValue(feature, IMPLEMENTED_PRODUCTS_LIST_SIZE);
		if (sizeObj != null) {
			final int size = Integer.parseInt(sizeObj.toString());
			for (int i = 0; i < size; i++) {
				final String productName = TraVarTUtils
						.getAttributeValue(feature, IMPLEMENTED_PRODUCTS_LIST_NAME_NR_ + i).toString();
				final Product implementedProduct = PprDslUtils.getProduct(asq, productName);
				assert implementedProduct != null;
				product.getImplementedProducts().add(implementedProduct);
			}

		}
	}

	private void restoreChildrenListOfProducts(final Feature feature, final Product product) {
		final Object sizeObj = TraVarTUtils.getAttributeValue(feature, CHILDREN_PRODUCTS_LIST_SIZE);
		if (sizeObj != null) {
			final int size = Integer.parseInt(sizeObj.toString());
			for (int i = 0; i < size; i++) {
				final String productName = TraVarTUtils.getAttributeValue(feature, CHILDREN_PRODUCTS_LIST_NAME_NR_ + i)
						.toString();
				final Product childrenProduct = PprDslUtils.getProduct(asq, productName);
				assert childrenProduct != null;
				product.getChildren().add(childrenProduct);
			}

		}
	}

	private void convertConstraints(final List<de.vill.model.constraint.Constraint> constraints)
			throws NotSupportedConstraintType {
		long constrNumber = 0;
		for (final de.vill.model.constraint.Constraint constraint : constraints) {
			final de.vill.model.constraint.Constraint cnf = TraVarTUtils.buildConstraintFromFormula(
					TraVarTUtils.buildFormulaFromConstraint(constraint, new FormulaFactory()).cnf());
			if (TraVarTUtils.isRequires(cnf) || TraVarTUtils.isExcludes(cnf)) {
				final Product left = PprDslUtils.getProduct(asq,
						((LiteralConstraint) TraVarTUtils.getLeftConstraint(cnf)).getLiteral());
				final Product right = PprDslUtils.getProduct(asq,
						((LiteralConstraint) TraVarTUtils.getRightConstraint(cnf)).getLiteral());
				if (left != null && right != null && !left.getRequires().contains(right)) {
					if (TraVarTUtils.isRequires(cnf)) {
						left.getRequires().add(right);
					} else {
						left.getExcludes().add(right);
					}
				}
			} else {
				// TODO: check
				final List<Product> products = constraint.getConstraintSubParts().stream()
						.map(f -> PprDslUtils.getProduct(asq, f.toString())).collect(Collectors.toList());
				final String defintion = toConstraintDefintion(products, constraint);
				constrNumber++;
				final Constraint pprConstraint = new Constraint(String.format("Constraint%s", constrNumber), defintion);
				asq.getGlobalConstraints().add(pprConstraint);
			}
		}
	}

	private String toConstraintDefintion(final List<Product> products,
			final de.vill.model.constraint.Constraint constraint) throws NotSupportedConstraintType {
		final StringBuffer buffer = new StringBuffer();
		for (final Product product : products) {
			buffer.append(product.getId());
			buffer.append(ConstraintDefinitionParser.DELIMITER);
		}
		buffer.deleteCharAt(buffer.lastIndexOf(ConstraintDefinitionParser.DELIMITER));
		buffer.append(" ");
		buffer.append(ConstraintDefinitionParser.DEFINITION_ARROW);
		buffer.append(" ");
		toNodeString(buffer, constraint);
		return buffer.toString();
	}

	private void toNodeString(final StringBuffer buffer, final de.vill.model.constraint.Constraint constraint)
			throws NotSupportedConstraintType {
		// todo: check max depth function
		if (TraVarTUtils.getMaxDepth(constraint) == 1) {
			buffer.append(constraint);
		} else if (constraint instanceof ImplicationConstraint || constraint instanceof AndConstraint
				|| constraint instanceof OrConstraint) {
			toNodeString(buffer, constraint.getConstraintSubParts().get(0));
			if (constraint instanceof ImplicationConstraint) {
				buffer.append(" ");
				buffer.append(ConstraintDefinitionParser.IMPLIES);
			} else if (constraint instanceof AndConstraint) {
				buffer.append(" ");
				buffer.append(ConstraintDefinitionParser.AND);
			} else { // OrConstraint
				buffer.append(" ");
				buffer.append(ConstraintDefinitionParser.OR);
			}
			buffer.append(" ");
			// TODO:
			toNodeString(buffer, TraVarTUtils.getRightConstraint(constraint));
		} else if (constraint instanceof NotConstraint) {
			buffer.append(" ");
			buffer.append(ConstraintDefinitionParser.NOT);
			buffer.append(" ");
			toNodeString(buffer, constraint.getConstraintSubParts().get(0));
		} else {
			throw new NotSupportedConstraintType(constraint.getClass().toString());
		}
	}

	private void deriveProductsFromSamples(final FeatureModel fm) throws NotSupportedVariabilityTypeException {
		if (samples == null) {
			// TODO: this is supposed to be FeatureIDE Feature Model
//            this.samples = sampler.sampleValidConfigurations(fm);
		}
		if (!samples.isEmpty()) {
			// create abstract base product for samples
			final String baseProductNameId = fm.getRootFeature().getFeatureName().concat("_products");
			final Product baseProduct = new Product(baseProductNameId, baseProductNameId);
			baseProduct.setAbstract(true);
			// union set of keys is the super type requires field
			final Set<String> commonNames = TraVarTUtils.getCommonConfigurationNameSet(samples);
			baseProduct.getRequires().addAll(PprDslUtils.getProducts(asq, commonNames));
			PprDslUtils.addProduct(asq, baseProduct);
			// Create the set of names
			// for each configuration create a sub product ("implements") and set requires
			// with remaining product names
			// collect all created products for later
			final Set<Product> implProducts = new HashSet<>();
			final Set<Product> excludeProducts = new HashSet<>();
			final Set<Set<String>> fmConfigurations = TraVarTUtils.createConfigurationNameSet(samples);
			int productNumber = 1;
			for (final Set<String> config : fmConfigurations) {
				config.removeAll(commonNames);
				final String productName = baseProductNameId + String.format("_%s", productNumber);
				productNumber++;
				final Product product = new Product(productName, productName);
				product.getImplementedProducts().add(baseProduct);
				product.setAbstract(false);
				product.getRequires().addAll(PprDslUtils.getProducts(asq, config));
				PprDslUtils.addProduct(asq, product);
				implProducts.add(product);
				excludeProducts.add(product);
			}
			// the created products exclude each other, so we have to iterate again and
			// add the excludes constraints
			for (final Product implP : implProducts) {
				for (final Product excProduct : excludeProducts) {
					if (!implP.equals(excProduct)) {
						excProduct.getExcludes().add(implP);
					}
				}
			}
		}
	}

}
