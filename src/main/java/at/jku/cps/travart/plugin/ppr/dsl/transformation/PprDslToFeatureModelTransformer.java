package at.jku.cps.travart.plugin.ppr.dsl.transformation;

import at.jku.cps.travart.core.common.Prop4JUtils;
import at.jku.cps.travart.core.common.TraVarTUtils;
import at.jku.cps.travart.core.common.UVLUtils;
import at.jku.cps.travart.core.exception.NotSupportedVariabilityTypeException;
import at.jku.cps.travart.plugin.ppr.dsl.common.PprDslUtils;
import at.jku.cps.travart.plugin.ppr.dsl.parser.ConstraintDefinitionParser;
import at.sqi.ppr.model.AssemblySequence;
import at.sqi.ppr.model.NamedObject;
import at.sqi.ppr.model.constraint.Constraint;
import at.sqi.ppr.model.product.Product;
import at.sqi.ppr.model.vdi.product.IProduct;
import de.ovgu.featureide.fm.core.ExtensionManager.NoSuchExtensionException;
import de.ovgu.featureide.fm.core.base.FeatureUtils;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.functional.Functional;
import de.vill.model.Attribute;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.constraint.ImplicationConstraint;
import de.vill.model.constraint.LiteralConstraint;
import de.vill.model.constraint.NotConstraint;
import org.prop4j.Node;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

public class PprDslToFeatureModelTransformer {

    //private IFeatureModelFactory factory;
    private FeatureModel model;

    public FeatureModel transform(final AssemblySequence asq, final String modelName)
            throws NotSupportedVariabilityTypeException {
        try {
            //this.factory = FMFactoryManager.getInstance().getFactory(DefaultFeatureModelFactory.ID);
            this.model = new FeatureModel();
            this.transformProducts(asq);
            this.deriveFeatureTree(asq);
            this.transformConstraints(asq);
            // todo: fix this somehow
            TraVarTUtils.deriveFeatureModelRoot(this.model, modelName, false);
            this.optimizeFeatureModel();
            return this.model;
        } catch (final NoSuchExtensionException e) {
            throw new NotSupportedVariabilityTypeException(e);
        }
    }

    private void transformProducts(final AssemblySequence asq) {
        for (final Product product : asq.getProducts().values()) {
            if (PprDslUtils.isPartialProduct(product)) {
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
    }

    private void deriveFeatureTree(final AssemblySequence asq) {
        for (final Product product : asq.getProducts().values()) {
            if (PprDslUtils.isPartialProduct(product)) {
                final Feature feature = this.model.getFeatureMap().get(product.getId());
                if (!product.getImplementedProducts().isEmpty() && product.getImplementedProducts().size() == 1) {
                    final Product parentProduct = product.getImplementedProducts().get(0);
                    final Feature parentFeature = this.model.getFeatureMap().get(parentProduct.getId());
                    FeatureUtils.addChild(parentFeature, feature);
                } else {
                    // no tree can be derived from implemented products, but constraints for each of
                    // the implemented products
                    // (Product requires implemented products)
                    for (final Product implemented : product.getImplementedProducts()) {
                        final Feature impFeature = this.model.getFeatureMap().get(implemented.getId());
                        assert impFeature != null;
                        if (!TraVarTUtils.isParentFeatureOf(feature, impFeature)) {
                            this.model.getConstraints().add(
                                    new ImplicationConstraint(
                                            new LiteralConstraint(feature.getFeatureName()),
                                            new LiteralConstraint(impFeature.getFeatureName())
                                    )
                            );
                        }
                    }
                }
                // store the implemented products also as attributes to restore them in the
                // roundtrip
                this.storeImplementedProductsAsProperties(feature, product.getImplementedProducts());
                // derive tree from children attribute
                if (!product.getChildProducts().isEmpty()) {
                    for (final IProduct child : product.getChildProducts()) {
                        final Product childproduct = (Product) child;
                        final Feature childFeature = this.model.getFeatureMap().get(childproduct.getId());
                        FeatureUtils.addChild(feature, childFeature);
                    }
                }
            }
            if (!PprDslUtils.isPartialProduct(product) && product.isAbstract()) {
                // if the dsl product is abstract the feature model e.g., mandatory features
                for (final Product required : product.getRequires()) {
                    final Feature mandatoryFeature = this.model.getFeatureMap().get(required.getId());
                    assert mandatoryFeature != null;
                    FeatureUtils.setMandatory(mandatoryFeature, true);
                }
            }
        }
    }

    private void transformConstraints(final AssemblySequence asq) {
        for (final Product product : asq.getProducts().values()) {
            if (PprDslUtils.isPartialProduct(product)) {
                final Feature child = this.model.getFeatureMap().get(product.getId());
                assert child != null;
                // requires constraints
                for (final Product required : product.getRequires()) {
                    final Feature parent = this.model.getFeatureMap().get(required.getId());
                    if (parent != null) {
                        if (!TraVarTUtils.isParentFeatureOf(child, parent)) {
                            this.model.getConstraints().add(
                                    new ImplicationConstraint(
                                            new LiteralConstraint((child.getFeatureName())),
                                            new LiteralConstraint(parent.getFeatureName())
                                    )
                            );
                        }
                    }
                }
                // excludes constraints
                for (final Product excluded : product.getExcludes()) {
                    final Feature parent = this.model.getFeatureMap().get(excluded.getId());
                    if (parent != null) {
                        if (!TraVarTUtils.isParentFeatureOf(child, parent)) {
                            this.model.getConstraints().add(
                                    new ImplicationConstraint(
                                            new LiteralConstraint((child.getFeatureName())),
                                            new NotConstraint(new LiteralConstraint(parent.getFeatureName()))
                                    )
                            );
                        }
                    }
                }
            }
        }
        for (final Constraint constr : asq.getGlobalConstraints()) {
            final ConstraintDefinitionParser parser = new ConstraintDefinitionParser(this.model);
            final de.vill.model.constraint.Constraint fmConstraint = parser.parse(constr.getDefinition());
            this.model.getConstraints().add(fmConstraint);
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

    private void optimizeFeatureModel() { // final AssemblySequence asq
        // find mandatory features within feature groups
        this.fixFalseOptionalFeaturesByFeatureGroupConstraints(FeatureUtils.getRoot(this.model));
        // find mandatory features within abstract feature groups
        this.fixFalseOptionalFeaturesByAbstractFeatureGroup(FeatureUtils.getRoot(this.model));
        // find mandatory features within requires constraints
        this.fixFalseOptionalFeaturesByConstraints();
        // find alternative groups
        this.transformConstraintsToAlternativeGroup(FeatureUtils.getRoot(this.model));
    }

    private void fixFalseOptionalFeaturesByConstraints() {
        final Set<de.vill.model.constraint.Constraint> toDelete = new HashSet<>();
        for (final de.vill.model.constraint.Constraint constr : this.model.getConstraints()) {
            final Node cnf = constr.getNode().toCNF();
            if (Prop4JUtils.isRequires(cnf)) {
                final IFeature left = this.model.getFeatureMap().get(Prop4JUtils.getLiteralName(Prop4JUtils.getLeftLiteral(cnf)));
                final IFeature right = this.model.getFeatureMap().get(Prop4JUtils.getLiteralName(Prop4JUtils.getRightLiteral(cnf)));
                if (left != null && right != null && FeatureUtils.isMandatorySet(left)
                        && !FeatureUtils.isMandatorySet(right) && FeatureUtils.isRoot(FeatureUtils.getParent(left))) {
                    FeatureUtils.setMandatory(right, true);
                    toDelete.add(constr);
                }
            }
        }
        toDelete.forEach(constr -> this.model.removeConstraint(constr));
    }

    private void fixFalseOptionalFeaturesByAbstractFeatureGroup(final Feature root) {
        for (final Feature child : root.getChildren()) {
            this.fixFalseOptionalFeaturesByAbstractFeatureGroup(child);
        }
        if (FeatureUtils.getChildrenCount(root) > 0 && UVLUtils.isAbstract(root)
                && (FeatureUtils.isOr(root) || FeatureUtils.isAlternative(root))) {
            // abstract features where all child features are mandatory are also mandatory
            boolean childMandatory = true;
            for (final IFeature childFeature : FeatureUtils.getChildren(root)) {
                childMandatory = childMandatory && FeatureUtils.isMandatorySet(childFeature);
            }
            if (childMandatory) {
                FeatureUtils.setMandatory(root, true);
            }
        }
    }

    private void fixFalseOptionalFeaturesByFeatureGroupConstraints(final Feature root) {
        for (final Feature child : root.getChildren()) {
            this.fixFalseOptionalFeaturesByFeatureGroupConstraints(child);
        }
        // if there is a requires constraint in the feature model between parent and
        // child, we can remove the constraint and make the child mandatory
        for (final Feature childFeature : FeatureUtils.getChildren(root)) {
            final de.vill.model.constraint.Constraint requiredConstraint = new ImplicationConstraint(
                    new LiteralConstraint(root.getFeatureName()),
                    new LiteralConstraint(childFeature.getFeatureName())
            );
            final List<de.vill.model.constraint.Constraint> relevant = this.model.getConstraints().stream()
                    .filter(constr -> constr.getNode().toCNF().equals(requiredConstraint.toCNF()))
                    .collect(Collectors.toList());
            if (relevant.size() == 1) {
                FeatureUtils.setMandatory(childFeature, true);
                this.model.removeConstraint(relevant.get(0));
            }
        }
    }

    private void transformConstraintsToAlternativeGroup(final Feature root) {
        final int childCount = FeatureUtils.getChildrenCount(root);
        if (childCount > 0) {
            final List<de.vill.model.constraint.Constraint> constraints = this.model.getConstraints();
            final Set<de.vill.model.constraint.Constraint> relevantExcludesConstraints = new HashSet<>();
            for (final Feature childFeature : UVLUtils.getChildren(root)) {
                this.transformConstraintsToAlternativeGroup(childFeature);
                final Set<Feature> otherChildren = Functional.toSet(FeatureUtils.getChildren(root));
                otherChildren.remove(childFeature);
                for (final de.vill.model.constraint.Constraint constr : constraints) {
                    if (Prop4JUtils.isExcludes(constr.getNode().toCNF())
                            && constr.getContainedFeatures().contains(childFeature)
                            && constr.getContainedFeatures().stream().anyMatch(f -> otherChildren.contains(f))) {
                        for (final Feature other : otherChildren) {
                            final de.vill.model.constraint.Constraint constraint = new ImplicationConstraint(
                                    new LiteralConstraint(childFeature.getFeatureName()),
                                    new LiteralConstraint(other.getFeatureName())
                            );
                            if (constr.getNode().equals(constraint)) {
                                relevantExcludesConstraints.add(constr);
                            }
                        }

                    }
                }
            }
            if (!relevantExcludesConstraints.isEmpty()
                    // is <= really correct? we do not want to remove to much constraints
                    && childCount * (childCount - 1) <= relevantExcludesConstraints.size()) {
                FeatureUtils.setAlternative(root);
                relevantExcludesConstraints.forEach(constr -> this.model.removeConstraint(constr));
            }
        }
    }
}
