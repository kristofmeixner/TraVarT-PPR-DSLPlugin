package at.jku.cps.travart.plugin.ppr.dsl.transformation;

import at.jku.cps.travart.core.exception.NotSupportedVariabilityTypeException;
import at.jku.cps.travart.core.helpers.TraVarTUtils;
import at.sqi.ppr.dsl.reader.constants.DslConstants;
import at.sqi.ppr.model.AssemblySequence;
import at.sqi.ppr.model.NamedObject;
import at.sqi.ppr.model.product.Product;
import de.vill.model.Attribute;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.constraint.Constraint;
import de.vill.model.constraint.LiteralConstraint;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

public class FeatureModelToPprDslRoundtripTransformer {

    private AssemblySequence asq;

    public AssemblySequence transform(final FeatureModel model)
            throws NotSupportedVariabilityTypeException {
        this.asq = new AssemblySequence();
        this.convertFeature(model.getRootFeature());
        this.restoreAttributes(model.getRootFeature());
        this.convertConstraints(model.getConstraints());
        return this.asq;
    }

    private void convertFeature(final Feature feature) throws NotSupportedVariabilityTypeException {
        final Product product = new Product();
        product.setId(feature.getFeatureName());
        product.setName(this.restoreNameFromProperties(feature, product));
        product.setAbstract(TraVarTUtils.isAbstract(feature));
        this.restoreAttributesFromProperties(feature, product);
        this.asq.getProducts().put(product.getId(), product);

        for (final Feature child : TraVarTUtils.getChildren(feature)) {
            if (!this.isEnumSubFeature(child)) {
                this.convertFeature(child);
            }
        }
    }

    private void restoreAttributes(final Feature feature) {
        final Product product = this.getProductFromId(feature.getFeatureName());
        assert product != null;
        this.restoreChildrenListOfProducts(feature, product);
        this.restoreImplementsListOfProducts(feature, product);

        for (final Feature child : TraVarTUtils.getChildren(feature)) {
            if (!this.isEnumSubFeature(child)) {
                this.restoreAttributes(child);
            }
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
        final List<String> attributeNames = feature.getAttributes().keySet()
                .stream()
                .filter(entry -> entry.startsWith(ATTRIBUTE_ID_KEY_PRAEFIX))
                .map(entry -> entry.substring(ATTRIBUTE_ID_KEY_PRAEFIX.length()))
                .collect(Collectors.toList());

        for (final String attributeName : attributeNames) {
            final NamedObject attribute = new NamedObject();
            attribute.setName(attributeName);

            attribute.setEntityType(DslConstants.ATTRIBUTE_ENTITY);

            attribute.setDescription(
                    TraVarTUtils.getAttributeValue(feature, ATTRIBUTE_DESCRIPTION_KEY_PRAEFIX + attributeName).toString()
            );
            attribute.setUnit(
                    TraVarTUtils.getAttributeValue(feature, ATTRIBUTE_UNIT_KEY_PRAEFIX + attributeName).toString()
            );

            final String type = TraVarTUtils.getAttributeValue(feature, ATTRIBUTE_TYPE_KEY_PRAEFIX + attributeName).toString();
            attribute.setType(type);

            switch (type.toLowerCase()) {
                case "number":
                    final Double defaultValue = Double.parseDouble(
                            TraVarTUtils.getAttributeValue(feature, ATTRIBUTE_DEFAULT_VALUE_KEY_PRAEFIX + attributeName).toString()
                    );
                    attribute.setDefaultValue(defaultValue);
                    final Object valueStr =
                            TraVarTUtils.getAttributeValue(feature, ATTRIBUTE_VALUE_KEY_PRAEFIX + attribute.getName());
                    if (valueStr != null) {
                        final Double value = Double.parseDouble(valueStr.toString());
                        attribute.setValue(value);
                    }
                    break;
                case "string":
                    attribute.setDefaultValue(
                            TraVarTUtils.getAttributeValue(feature, ATTRIBUTE_DEFAULT_VALUE_KEY_PRAEFIX + attributeName).toString()
                    );
                    attribute.setValue(
                            TraVarTUtils.getAttributeValue(feature, ATTRIBUTE_VALUE_KEY_PRAEFIX + attribute.getName()).toString()
                    );
                    break;
            }

            product.getAttributes().put(attributeName, attribute);
            this.asq.getDefinedAttributes().put(attributeName, attribute);
            this.asq.getProductAttributes().put(attributeName, attribute);
        }
    }

    private void restoreImplementsListOfProducts(final Feature feature, final Product product) {
        final Object sizeObj = TraVarTUtils.getAttributeValue(feature, IMPLEMENTED_PRODUCTS_LIST_SIZE);
        if (sizeObj != null) {
            final int size = Integer.parseInt(sizeObj.toString());
            for (int i = 0; i < size; i++) {
                final String productName = TraVarTUtils.getAttributeValue(feature, IMPLEMENTED_PRODUCTS_LIST_NAME_NR_ + i).toString();
                final Product implementedProduct = this.getProductFromId(productName);
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
                final String productName = TraVarTUtils.getAttributeValue(feature, CHILDREN_PRODUCTS_LIST_NAME_NR_ + i).toString();
                final Product childrenProduct = this.getProductFromId(productName);
                assert childrenProduct != null;
                product.getChildren().add(childrenProduct);
            }
        }
    }

    private void convertConstraints(final List<Constraint> constraints) {
        for (final Constraint constraint : constraints) {
            this.convertConstraintNodeRec(constraint);
        }
    }

    private void convertConstraintNodeRec(final Constraint constraint) {
        // create a CNF from nodes enables separating the concerns how to transform the
        // different groups.
        // A requires B <=> CNF: Not(A) or B
        // A excludes B <=> CNF: Not(A) or Not(B)

        if (TraVarTUtils.isComplexConstraint(constraint)) {
            for (final Constraint child : constraint.getConstraintSubParts()) {
                this.convertConstraintNodeRec(child);
            }
        } else {
            this.convertConstraintNode(constraint);
        }
    }

    private void convertConstraintNode(final Constraint constraint) {
        // TODO: Check coz 110% this is incorrect
        // node is an implies --> requires attribute
        if (TraVarTUtils.isRequires(constraint)) {
            final Constraint sourceLiteral = TraVarTUtils.getFirstNegativeLiteral(constraint);
            final Constraint targetLiteral = TraVarTUtils.getFirstPositiveLiteral(constraint);

            if (sourceLiteral instanceof LiteralConstraint && targetLiteral instanceof LiteralConstraint) {
                // node is an implies --> requires attribute
                final Product sourceProduct = this.getProductFromId(((LiteralConstraint) sourceLiteral).getLiteral());
                final Product targetProduct = this.getProductFromId(((LiteralConstraint) targetLiteral).getLiteral());
                sourceProduct.getRequires().add(targetProduct);
            } else {
                // TODO: create constraint from it
            }
        }
        // node is an excludes --> excludes attribute
        else if (TraVarTUtils.isExcludes(constraint)) {
            final Constraint sourceLiteral = constraint.getConstraintSubParts().get(0);
            final Constraint targetLiteral = constraint.getConstraintSubParts().get(1);
            if ((sourceLiteral instanceof LiteralConstraint) && (targetLiteral instanceof LiteralConstraint)) {
                // node is an excludes --> excludes attribute
                final Product sourceProduct = this.getProductFromId(((LiteralConstraint) sourceLiteral).getLiteral());
                final Product targetProduct = this.getProductFromId(((LiteralConstraint) targetLiteral).getLiteral());
                sourceProduct.getExcludes().add(targetProduct);
            } else {
                // TODO: create constraint from it
            }
        } else {
            // TODO: create constraint from it
        }
    }

    private Product getProductFromId(final String productId) {
        return this.asq.getProducts().get(productId);
    }

    private boolean isEnumSubFeature(final Feature feature) {
        final Optional<Feature> parent = TraVarTUtils.getParent(feature, feature, null);
        return parent.isPresent() && TraVarTUtils.isEnumerationType(parent.get());
    }
}
