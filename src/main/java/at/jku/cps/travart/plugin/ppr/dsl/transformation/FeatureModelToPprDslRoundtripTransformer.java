package at.jku.cps.travart.plugin.ppr.dsl.transformation;

import at.jku.cps.travart.core.common.Prop4JUtils;
import at.jku.cps.travart.core.common.TraVarTUtils;
import at.jku.cps.travart.core.common.UVLUtils;
import at.jku.cps.travart.core.exception.NotSupportedVariabilityTypeException;
import at.sqi.ppr.dsl.reader.constants.DslConstants;
import at.sqi.ppr.model.AssemblySequence;
import at.sqi.ppr.model.NamedObject;
import at.sqi.ppr.model.product.Product;
import de.ovgu.featureide.fm.core.base.FeatureUtils;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.vill.model.Attribute;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.constraint.Constraint;
import org.prop4j.Literal;
import org.prop4j.Node;

import java.util.List;
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
        product.setAbstract(UVLUtils.isAbstract(feature));
        this.restoreAttributesFromProperties(feature, product);
        this.asq.getProducts().put(product.getId(), product);

        for (final Feature child : feature.getChildren()) {
            if (TraVarTUtils.isVirtualRootFeature(feature) || !this.isEnumSubFeature(child)) {
                this.convertFeature(child);
            }
        }
    }

    private void restoreAttributes(final Feature feature) {
        final Product product = this.getProductFromId(feature.getFeatureName());
        assert product != null;
        this.restoreChildrenListOfProducts(feature, product);
        this.restoreImplementsListOfProducts(feature, product);

        for (final Feature child : feature.getChildren()) {
            if (TraVarTUtils.isVirtualRootFeature(feature) || !this.isEnumSubFeature(child)) {
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
                    UVLUtils.getAttributeValue(feature, ATTRIBUTE_DESCRIPTION_KEY_PRAEFIX + attributeName).toString()
            );
            attribute.setUnit(
                    UVLUtils.getAttributeValue(feature, ATTRIBUTE_UNIT_KEY_PRAEFIX + attributeName).toString()
            );

            final String type = UVLUtils.getAttributeValue(feature, ATTRIBUTE_TYPE_KEY_PRAEFIX + attributeName).toString();
            attribute.setType(type);

            switch (type.toLowerCase()) {
                case "number":
                    final Double defaultValue = Double.parseDouble(
                            UVLUtils.getAttributeValue(feature, ATTRIBUTE_DEFAULT_VALUE_KEY_PRAEFIX + attributeName).toString()
                    );
                    attribute.setDefaultValue(defaultValue);
                    final Object valueStr =
                            UVLUtils.getAttributeValue(feature, ATTRIBUTE_VALUE_KEY_PRAEFIX + attribute.getName());
                    if (valueStr != null) {
                        final Double value = Double.parseDouble(valueStr.toString());
                        attribute.setValue(value);
                    }
                    break;
                case "string":
                    attribute.setDefaultValue(
                            UVLUtils.getAttributeValue(feature, ATTRIBUTE_DEFAULT_VALUE_KEY_PRAEFIX + attributeName).toString()
                    );
                    attribute.setValue(
                            UVLUtils.getAttributeValue(feature, ATTRIBUTE_VALUE_KEY_PRAEFIX + attribute.getName()).toString()
                    );
                    break;
            }

            product.getAttributes().put(attributeName, attribute);
            this.asq.getDefinedAttributes().put(attributeName, attribute);
            this.asq.getProductAttributes().put(attributeName, attribute);
        }
    }

    private void restoreImplementsListOfProducts(final Feature feature, final Product product) {
        final Object sizeObj = UVLUtils.getAttributeValue(feature, IMPLEMENTED_PRODUCTS_LIST_SIZE);
        if (sizeObj != null) {
            final int size = Integer.parseInt(sizeObj.toString());
            for (int i = 0; i < size; i++) {
                final String productName = UVLUtils.getAttributeValue(feature, IMPLEMENTED_PRODUCTS_LIST_NAME_NR_ + i).toString();
                final Product implementedProduct = this.getProductFromId(productName);
                assert implementedProduct != null;
                product.getImplementedProducts().add(implementedProduct);
            }
        }
    }

    private void restoreChildrenListOfProducts(final Feature feature, final Product product) {
        final Object sizeObj = UVLUtils.getAttributeValue(feature, CHILDREN_PRODUCTS_LIST_SIZE);
        if (sizeObj != null) {
            final int size = Integer.parseInt(sizeObj.toString());
            for (int i = 0; i < size; i++) {
                final String productName = UVLUtils.getAttributeValue(feature, CHILDREN_PRODUCTS_LIST_NAME_NR_ + i).toString();
                final Product childrenProduct = this.getProductFromId(productName);
                assert childrenProduct != null;
                product.getChildren().add(childrenProduct);
            }
        }
    }

    private void convertConstraints(final List<Constraint> constraints) {
        for (final Constraint constraint : constraints) {
            this.convertConstraintNodeRec(constraint.getNode());
        }
    }

    private void convertConstraintNodeRec(final Node node) {
        // create a CNF from nodes enables separating the concerns how to transform the
        // different groups.
        // A requires B <=> CNF: Not(A) or B
        // A excludes B <=> CNF: Not(A) or Not(B)
        final Node cnfNode = node.toCNF();
        if (Prop4JUtils.isComplexNode(cnfNode)) {
            for (final Node child : cnfNode.getChildren()) {
                this.convertConstraintNodeRec(child);
            }
        } else {
            this.convertConstraintNode(cnfNode);
        }
    }

    private void convertConstraintNode(final Node cnfNode) {
        // node is an implies --> requires attribute
        if (Prop4JUtils.isRequires(cnfNode)) {
            final Node sourceLiteral = Prop4JUtils.getFirstNegativeLiteral(cnfNode);
            final Node targetLiteral = Prop4JUtils.getFirstPositiveLiteral(cnfNode);
            if (Prop4JUtils.isLiteral(sourceLiteral) && Prop4JUtils.isLiteral(targetLiteral)) {
                // node is an implies --> requires attribute
                final Product sourceProduct = this.getProductFromId(Prop4JUtils.getLiteralName((Literal) sourceLiteral));
                final Product targetProduct = this.getProductFromId(Prop4JUtils.getLiteralName((Literal) targetLiteral));
                sourceProduct.getRequires().add(targetProduct);
            } else {
                // TODO: create constraint from it
            }
        }
        // node is an excludes --> excludes attribute
        else if (Prop4JUtils.isExcludes(cnfNode)) {
            final Node sourceLiteral = Prop4JUtils.getLeftNode(cnfNode);
            final Node targetLiteral = Prop4JUtils.getRightNode(cnfNode);
            if (Prop4JUtils.isLiteral(sourceLiteral) && Prop4JUtils.isLiteral(targetLiteral)) {
                // node is an excludes --> excludes attribute
                final Product sourceProduct = this.getProductFromId(Prop4JUtils.getLiteralName((Literal) sourceLiteral));
                final Product targetProduct = this.getProductFromId(Prop4JUtils.getLiteralName((Literal) targetLiteral));
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

    private boolean isEnumSubFeature(final IFeature feature) {
        // works as each tree in FeatureIDE has only one cardinality across all
        // sub-features.
        final IFeature parent = FeatureUtils.getParent(feature);
        return parent != null && TraVarTUtils.isEnumerationType(parent);
    }
}
