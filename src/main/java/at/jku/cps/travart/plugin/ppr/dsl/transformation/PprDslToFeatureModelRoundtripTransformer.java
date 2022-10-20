package at.jku.cps.travart.plugin.ppr.dsl.transformation;

import at.jku.cps.travart.core.common.Prop4JUtils;
import at.jku.cps.travart.core.common.TraVarTUtils;
import at.jku.cps.travart.core.exception.NotSupportedVariabilityTypeException;
import at.sqi.ppr.model.AssemblySequence;
import at.sqi.ppr.model.NamedObject;
import at.sqi.ppr.model.product.Product;
import de.ovgu.featureide.fm.core.ExtensionManager.NoSuchExtensionException;
import de.ovgu.featureide.fm.core.base.FeatureUtils;
import de.ovgu.featureide.fm.core.base.IConstraint;
import de.vill.model.Attribute;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;

import java.util.List;

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

public class PprDslToFeatureModelRoundtripTransformer {

    //    private IFeatureModelFactory factory;
    private FeatureModel model;

    public FeatureModel transform(final AssemblySequence asq, final String modelName)
            throws NotSupportedVariabilityTypeException {
        try {
            this.model = new FeatureModel();
            this.transformProducts(asq);
            this.deriveFeatureTree(asq);
            this.transformConstraints(asq);
            TraVarTUtils.deriveFeatureModelRoot(this.model, modelName, true);
            return this.model;
        } catch (final NoSuchExtensionException e) {
            throw new NotSupportedVariabilityTypeException(e);
        }
    }

    private void transformProducts(final AssemblySequence asq) {
        for (final Product product : asq.getProducts().values()) {
            final Feature feature = new Feature(product.getId());
            if (product.isAbstract()) {
                feature.getAttributes().put(
                        "abstract",
                        new Attribute("abstract", true)
                );
            }
            this.storeNameAttributeAsProperty(feature, product.getName());
            this.storeChildrenAttributeAsProperty(feature, product.getChildren());
            for (final NamedObject attribute : product.getAttributes().values()) {
                this.storeCustomAttributesAsProperty(feature, attribute);
            }
            FeatureUtils.addFeature(this.model, feature);
        }
    }

    private void deriveFeatureTree(final AssemblySequence asq) {
        for (final Product product : asq.getProducts().values()) {
            final Feature feature = this.model.getFeatureMap().get(product.getId());
            if (!product.getImplementedProducts().isEmpty() && product.getImplementedProducts().size() == 1) {
                final Product parentProduct = product.getImplementedProducts().get(0);
                final Feature parentFeature = this.model.getFeatureMap().get(parentProduct.getId());
                FeatureUtils.addChild(parentFeature, feature);
            } else {
                // no tree can be derived, but constraints for each of the implemented products
                // (Product requires implemented products)
                for (final Product implemented : product.getImplementedProducts()) {
                    final Feature impFeature = this.model.getFeatureMap().get(implemented.getId());
                    assert impFeature != null;
                    if (!TraVarTUtils.isParentFeatureOf(feature, impFeature)) {
                        final IConstraint constraint = this.factory.createConstraint(this.model, Prop4JUtils.createImplies(
                                Prop4JUtils.createLiteral(feature), Prop4JUtils.createLiteral(impFeature)));
                        FeatureUtils.addConstraint(this.model, constraint);
                    }
                }
            }
            // store the implemented products also as attributes to restore them in the
            // roundtrip
            this.storeImplementedProductsAsProperties(feature, product.getImplementedProducts());
        }
    }

    private void transformConstraints(final AssemblySequence asq) {
        for (final Product product : asq.getProducts().values()) {
            final Feature child = this.model.getFeatureMap().get(product.getId());
            assert child != null;
            // requires constraints
            for (final Product required : product.getRequires()) {
                final Feature parent = this.model.getFeatureMap().get(required.getId());
                assert parent != null;
                if (!TraVarTUtils.isParentFeatureOf(child, parent)) {
                    final IConstraint constraint = this.factory.createConstraint(this.model, Prop4JUtils
                            .createImplies(Prop4JUtils.createLiteral(child), Prop4JUtils.createLiteral(parent)));
                    FeatureUtils.addConstraint(this.model, constraint);
                }
            }
            // excludes constraints
            for (final Product excluded : product.getExcludes()) {
                final Feature parent = this.model.getFeatureMap().get(excluded.getId());
                assert parent != null;
                if (!TraVarTUtils.isParentFeatureOf(child, parent)) {
                    final IConstraint constraint = this.factory.createConstraint(this.model,
                            Prop4JUtils.createImplies(Prop4JUtils.createLiteral(child),
                                    Prop4JUtils.createNot(Prop4JUtils.createLiteral(parent))));
                    FeatureUtils.addConstraint(this.model, constraint);
                }
            }
        }
    }

    private void storeNameAttributeAsProperty(final Feature feature, final String name) {
        feature.getAttributes().put(
                NAME_ATTRIBUTE_KEY,
                new Attribute(NAME_ATTRIBUTE_KEY, name)
        );
    }

    private void storeCustomAttributesAsProperty(final Feature feature, final NamedObject attribute) {
        feature.getAttributes().put(
                ATTRIBUTE_ID_KEY_PRAEFIX + attribute.getName(),
                new Attribute(ATTRIBUTE_ID_KEY_PRAEFIX + attribute.getName(), attribute.getName())
        );
        feature.getAttributes().put(
                ATTRIBUTE_DESCRIPTION_KEY_PRAEFIX + attribute.getName(),
                new Attribute(ATTRIBUTE_DESCRIPTION_KEY_PRAEFIX + attribute.getName(), attribute.getDescription())
        );
        feature.getAttributes().put(
                ATTRIBUTE_UNIT_KEY_PRAEFIX + attribute.getName(),
                new Attribute(ATTRIBUTE_UNIT_KEY_PRAEFIX + attribute.getName(), attribute.getUnit())
        );
        feature.getAttributes().put(
                ATTRIBUTE_TYPE_KEY_PRAEFIX + attribute.getName(),
                new Attribute(ATTRIBUTE_TYPE_KEY_PRAEFIX + attribute.getName(), attribute.getType())
        );
        feature.getAttributes().put(
                ATTRIBUTE_DEFAULT_VALUE_KEY_PRAEFIX + attribute.getName(),
                new Attribute(ATTRIBUTE_DEFAULT_VALUE_KEY_PRAEFIX + attribute.getName(), attribute.getDefaultValueObject())
        );
        feature.getAttributes().put(
                ATTRIBUTE_VALUE_KEY_PRAEFIX + attribute.getName(),
                new Attribute(ATTRIBUTE_VALUE_KEY_PRAEFIX + attribute.getName(), attribute.getValueObject())
        );
    }

    private void storeImplementedProductsAsProperties(final Feature feature, final List<Product> implementedProducts) {
        // store the size of implemented products
        feature.getAttributes().put(
                IMPLEMENTED_PRODUCTS_LIST_SIZE,
                new Attribute(IMPLEMENTED_PRODUCTS_LIST_SIZE, implementedProducts.size())
        );
        // store the names of the implemented products
        for (int i = 0; i < implementedProducts.size(); i++) {
            feature.getAttributes().put(
                    IMPLEMENTED_PRODUCTS_LIST_NAME_NR_ + i,
                    new Attribute(IMPLEMENTED_PRODUCTS_LIST_NAME_NR_ + i, implementedProducts.get(i).getId())
            );
        }
    }

    private void storeChildrenAttributeAsProperty(final Feature feature, final List<Product> childProducts) {
        // store the size of child products
        feature.getAttributes().put(
                CHILDREN_PRODUCTS_LIST_SIZE,
                new Attribute(CHILDREN_PRODUCTS_LIST_SIZE, childProducts.size())
        );
        // store the names of the implemented products
        for (int i = 0; i < childProducts.size(); i++) {
            feature.getAttributes().put(
                    CHILDREN_PRODUCTS_LIST_NAME_NR_ + i,
                    new Attribute(CHILDREN_PRODUCTS_LIST_NAME_NR_ + i, childProducts.get(i).getId())
            );
        }
    }
}
