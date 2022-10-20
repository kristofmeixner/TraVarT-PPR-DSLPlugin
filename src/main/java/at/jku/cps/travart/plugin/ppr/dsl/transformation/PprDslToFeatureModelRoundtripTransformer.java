package at.jku.cps.travart.plugin.ppr.dsl.transformation;

import at.jku.cps.travart.core.common.IModelTransformer;
import at.jku.cps.travart.core.common.Prop4JUtils;
import at.jku.cps.travart.core.common.TraVarTUtils;
import at.jku.cps.travart.core.exception.NotSupportedVariabilityTypeException;
import at.sqi.ppr.model.AssemblySequence;
import at.sqi.ppr.model.NamedObject;
import at.sqi.ppr.model.product.Product;
import de.ovgu.featureide.fm.core.ExtensionManager.NoSuchExtensionException;
import de.ovgu.featureide.fm.core.base.FeatureUtils;
import de.ovgu.featureide.fm.core.base.IConstraint;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.IFeatureModelFactory;
import de.ovgu.featureide.fm.core.base.impl.DefaultFeatureModelFactory;
import de.ovgu.featureide.fm.core.base.impl.FMFactoryManager;
import de.ovgu.featureide.fm.core.init.FMCoreLibrary;
import de.ovgu.featureide.fm.core.init.LibraryManager;

import java.util.List;

import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.ATTRIBUTE_DEFAULT_VALUE_KEY_PRAEFIX;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.ATTRIBUTE_DEFAULT_VALUE_TYPE;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.ATTRIBUTE_DESCRIPTION_KEY_PRAEFIX;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.ATTRIBUTE_DESCRIPTION_TYPE;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.ATTRIBUTE_ID_KEY_PRAEFIX;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.ATTRIBUTE_ID_TYPE;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.ATTRIBUTE_TYPE_KEY_PRAEFIX;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.ATTRIBUTE_TYPE_TYPE;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.ATTRIBUTE_UNIT_KEY_PRAEFIX;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.ATTRIBUTE_UNIT_TYPE;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.ATTRIBUTE_VALUE_KEY_PRAEFIX;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.ATTRIBUTE_VALUE_TYPE;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.CHILDREN_PRODUCTS_LIST_NAME_NR_;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.CHILDREN_PRODUCTS_LIST_NAME_NR_TYPE;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.CHILDREN_PRODUCTS_LIST_SIZE;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.CHILDREN_PRODUCTS_LIST_SIZE_TYPE;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.IMPLEMENTED_PRODUCTS_LIST_NAME_NR_;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.IMPLEMENTED_PRODUCTS_LIST_NAME_NR_TYPE;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.IMPLEMENTED_PRODUCTS_LIST_SIZE;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.IMPLEMENTED_PRODUCTS_LIST_SIZE_TYPE;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.NAME_ATTRIBUTE_KEY;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.NAME_ATTRIBUTE_TYPE;

public class PprDslToFeatureModelRoundtripTransformer implements IModelTransformer<AssemblySequence, IFeatureModel> {

    static {
        LibraryManager.registerLibrary(FMCoreLibrary.getInstance());
    }

    private IFeatureModelFactory factory;
    private IFeatureModel fm;

    @Override
    public IFeatureModel transform(final AssemblySequence asq, final String modelName)
            throws NotSupportedVariabilityTypeException {
        try {
            this.factory = FMFactoryManager.getInstance().getFactory(DefaultFeatureModelFactory.ID);
            this.fm = this.factory.create();
            this.transformProducts(asq);
            this.deriveFeatureTree(asq);
            this.transformConstraints(asq);
            TraVarTUtils.deriveFeatureModelRoot(this.factory, this.fm, modelName, true);
            return this.fm;
        } catch (final NoSuchExtensionException e) {
            throw new NotSupportedVariabilityTypeException(e);
        }
    }

    private void transformProducts(final AssemblySequence asq) {
        for (final Product product : asq.getProducts().values()) {
            final IFeature feature = this.factory.createFeature(this.fm, product.getId());
            FeatureUtils.setAbstract(feature, product.isAbstract());
            this.storeNameAttributeAsProperty(feature, product.getName());
            this.storeChildrenAttributeAsProperty(feature, product.getChildren());
            for (final NamedObject attribute : product.getAttributes().values()) {
                this.storeCustomAttributesAsProperty(feature, attribute);
            }
            FeatureUtils.addFeature(this.fm, feature);
        }
    }

    private void deriveFeatureTree(final AssemblySequence asq) {
        for (final Product product : asq.getProducts().values()) {
            final IFeature feature = this.fm.getFeature(product.getId());
            if (!product.getImplementedProducts().isEmpty() && product.getImplementedProducts().size() == 1) {
                final Product parentProduct = product.getImplementedProducts().get(0);
                final IFeature parentFeature = this.fm.getFeature(parentProduct.getId());
                FeatureUtils.addChild(parentFeature, feature);
            } else {
                // no tree can be derived, but constraints for each of the implemented products
                // (Product requires implemented products)
                for (final Product implemented : product.getImplementedProducts()) {
                    final IFeature impFeature = this.fm.getFeature(implemented.getId());
                    assert impFeature != null;
                    if (!TraVarTUtils.isParentFeatureOf(feature, impFeature)) {
                        final IConstraint constraint = this.factory.createConstraint(this.fm, Prop4JUtils.createImplies(
                                Prop4JUtils.createLiteral(feature), Prop4JUtils.createLiteral(impFeature)));
                        FeatureUtils.addConstraint(this.fm, constraint);
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
            final IFeature child = this.fm.getFeature(product.getId());
            assert child != null;
            // requires constraints
            for (final Product required : product.getRequires()) {
                final IFeature parent = this.fm.getFeature(required.getId());
                assert parent != null;
                if (!TraVarTUtils.isParentFeatureOf(child, parent)) {
                    final IConstraint constraint = this.factory.createConstraint(this.fm, Prop4JUtils
                            .createImplies(Prop4JUtils.createLiteral(child), Prop4JUtils.createLiteral(parent)));
                    FeatureUtils.addConstraint(this.fm, constraint);
                }
            }
            // excludes constraints
            for (final Product excluded : product.getExcludes()) {
                final IFeature parent = this.fm.getFeature(excluded.getId());
                assert parent != null;
                if (!TraVarTUtils.isParentFeatureOf(child, parent)) {
                    final IConstraint constraint = this.factory.createConstraint(this.fm,
                            Prop4JUtils.createImplies(Prop4JUtils.createLiteral(child),
                                    Prop4JUtils.createNot(Prop4JUtils.createLiteral(parent))));
                    FeatureUtils.addConstraint(this.fm, constraint);
                }
            }
        }
    }

    private void storeNameAttributeAsProperty(final IFeature feature, final String name) {
        feature.getCustomProperties().set(NAME_ATTRIBUTE_KEY, NAME_ATTRIBUTE_TYPE, name);
    }

    private void storeCustomAttributesAsProperty(final IFeature feature, final NamedObject attribute) {
        feature.getCustomProperties().set(ATTRIBUTE_ID_KEY_PRAEFIX + attribute.getName(), ATTRIBUTE_ID_TYPE,
                attribute.getName());
        feature.getCustomProperties().set(ATTRIBUTE_DESCRIPTION_KEY_PRAEFIX + attribute.getName(),
                ATTRIBUTE_DESCRIPTION_TYPE, attribute.getDescription());
        feature.getCustomProperties().set(ATTRIBUTE_UNIT_KEY_PRAEFIX + attribute.getName(), ATTRIBUTE_UNIT_TYPE,
                attribute.getUnit());
        feature.getCustomProperties().set(ATTRIBUTE_TYPE_KEY_PRAEFIX + attribute.getName(), ATTRIBUTE_TYPE_TYPE,
                attribute.getType());
        feature.getCustomProperties().set(ATTRIBUTE_DEFAULT_VALUE_KEY_PRAEFIX + attribute.getName(),
                ATTRIBUTE_DEFAULT_VALUE_TYPE, attribute.getDefaultValueObject().toString());
        feature.getCustomProperties().set(ATTRIBUTE_VALUE_KEY_PRAEFIX + attribute.getName(), ATTRIBUTE_VALUE_TYPE,
                attribute.getValueObject().toString());
    }

    private void storeImplementedProductsAsProperties(final IFeature feature, final List<Product> implementedProducts) {
        // store the size of implemented products
        feature.getCustomProperties().set(IMPLEMENTED_PRODUCTS_LIST_SIZE, IMPLEMENTED_PRODUCTS_LIST_SIZE_TYPE,
                Integer.toString(implementedProducts.size()));
        // store the names of the implemented products
        for (int i = 0; i < implementedProducts.size(); i++) {
            feature.getCustomProperties().set(IMPLEMENTED_PRODUCTS_LIST_NAME_NR_ + i,
                    IMPLEMENTED_PRODUCTS_LIST_NAME_NR_TYPE, implementedProducts.get(i).getId());
        }
    }

    private void storeChildrenAttributeAsProperty(final IFeature feature, final List<Product> childProducts) {
        // store the size of child products
        feature.getCustomProperties().set(CHILDREN_PRODUCTS_LIST_SIZE, CHILDREN_PRODUCTS_LIST_SIZE_TYPE,
                Integer.toString(childProducts.size()));
        // store the names of the implemented products
        for (int i = 0; i < childProducts.size(); i++) {
            feature.getCustomProperties().set(CHILDREN_PRODUCTS_LIST_NAME_NR_ + i, CHILDREN_PRODUCTS_LIST_NAME_NR_TYPE,
                    childProducts.get(i).getId());
        }
    }
}
