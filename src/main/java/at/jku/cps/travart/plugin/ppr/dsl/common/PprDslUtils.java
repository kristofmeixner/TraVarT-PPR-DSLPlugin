package at.jku.cps.travart.plugin.ppr.dsl.common;

import at.jku.cps.travart.core.common.FeatureMetaData;
import at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties;
import at.sqi.ppr.model.AssemblySequence;
import at.sqi.ppr.model.product.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PprDslUtils {

    private PprDslUtils() {
    }

    public static Product getProduct(final AssemblySequence asq, final String id) {
        return asq.getProducts().get(id);
    }

    public static Set<Product> getProducts(final AssemblySequence asq, final Set<String> commonNames) {
        final Set<Product> products = new HashSet<>();
        commonNames.forEach(name -> {
            final Product product = getProduct(asq, name);
            if (product != null) {
                products.add(product);
            }
        });
        return products;
    }

    public static void addProduct(final AssemblySequence asq, final Product product) {
        asq.getProducts().put(product.getId(), product);
    }

    public static void logModelStatistics(final Logger logger, final AssemblySequence seq) {
        logger.log(Level.INFO,
                String.format("#Products: %s", seq.getProducts() != null ? seq.getProducts().size() : 0));
        logger.log(Level.INFO, String.format("#Variant Products: %s",
                seq.getProducts() != null ? countVariantProducts(seq.getProducts()) : 0));
        logger.log(Level.INFO, String.format("#Partial Products: %s",
                seq.getProducts() != null ? countPartialProducts(seq.getProducts()) : 0));
        logger.log(Level.INFO, String.format("#Abstract Products: %s",
                seq.getProducts() != null ? countAbstractProducts(seq.getProducts()) : 0));
        logger.log(Level.INFO, String.format("#Implementing Products: %s",
                seq.getProducts() != null ? countImplementingProducts(seq.getProducts()) : 0));
        logger.log(Level.INFO, String.format("#Constrainted Products: %s",
                seq.getProducts() != null ? countConstraintedProducts(seq.getProducts()) : 0));
        logger.log(Level.INFO, String.format("#Constrainted requries Products: %s",
                seq.getProducts() != null ? countConstraintedRequieresProducts(seq.getProducts()) : 0));
        logger.log(Level.INFO, String.format("#Constrainted excludes Products: %s",
                seq.getProducts() != null ? countConstraintedExcludesProducts(seq.getProducts()) : 0));
        logger.log(Level.INFO,
                String.format("#Processes: %s", seq.getProcesses() != null ? seq.getProcesses().size() : 0));
        logger.log(Level.INFO,
                String.format("#Constraints: %s", seq.getConstraints() != null ? seq.getConstraints().size() : 0));
        logger.log(Level.INFO, String.format("#Global Constraints: %s",
                seq.getGlobalConstraints() != null ? seq.getGlobalConstraints().size() : 0));
    }

    public static long getNumberOfProducts(final AssemblySequence seq) {
        return seq.getProducts() != null ? seq.getProducts().size() : 0;
    }

    public static long getNumberOfProcesses(final AssemblySequence seq) {
        return seq.getProcesses() != null ? seq.getProcesses().size() : 0;
    }

    public static long getNumberOfConstraints(final AssemblySequence seq) {
        return seq.getConstraints() != null ? seq.getConstraints().size() : 0;
    }

    public static long getNumberOfGlobalConstraints(final AssemblySequence seq) {
        return seq.getGlobalConstraints() != null ? seq.getGlobalConstraints().size() : 0;
    }

    public static long countConstraintedExcludesProducts(final HashMap<String, Product> products) {
        return products.values().stream().filter(p -> !p.getExcludes().isEmpty()).count();
    }

    public static long countConstraintedRequieresProducts(final HashMap<String, Product> products) {
        return products.values().stream().filter(p -> !p.getRequires().isEmpty()).count();
    }

    public static long countConstraintedProducts(final HashMap<String, Product> products) {
        return products.values().stream().filter(p -> !p.getRequires().isEmpty() || !p.getExcludes().isEmpty()).count();
    }

    public static long countImplementingProducts(final HashMap<String, Product> products) {
        return products.values().stream().filter(p -> !p.getImplementedProducts().isEmpty()).count();
    }

    public static long countAbstractProducts(final HashMap<String, Product> products) {
        return products.values().stream().filter(Product::isAbstract).count();
    }

    public static long countPartialProducts(final HashMap<String, Product> products) {
        return products.values().stream()
                .filter(p -> p.getAttributes().containsKey(DefaultPprDslTransformationProperties.PARTIAL_PRODUCT_ATTRIBUTE) && Boolean
                        .parseBoolean(p.getAttributes().get(DefaultPprDslTransformationProperties.PARTIAL_PRODUCT_ATTRIBUTE).getValue().getValue().toString()))
                .count();
    }

    public static long countVariantProducts(final HashMap<String, Product> products) {
        return products.size() - countPartialProducts(products);
    }

    public static boolean isPartialProduct(final Product product) {
        return product.getAttributes().containsKey(DefaultPprDslTransformationProperties.PARTIAL_PRODUCT_ATTRIBUTE) && Boolean
                .parseBoolean(product.getAttributes().get(DefaultPprDslTransformationProperties.PARTIAL_PRODUCT_ATTRIBUTE).getValue().getValue().toString());
    }

    public static Map<String, List<String>> createParentChildRelationship(final Map<String, FeatureMetaData> featureMetaDataMap) {
        final Map<String, List<String>> parentChildRelationshipMap = new HashMap<>();

        featureMetaDataMap.keySet()
                .forEach(
                        key -> {
                            if (featureMetaDataMap.get(key).getHasParent()) {
                                final List<String> children = parentChildRelationshipMap.getOrDefault(
                                        featureMetaDataMap.get(key).getParentName(),
                                        new ArrayList<>()
                                );
                                children.add(key);
                                parentChildRelationshipMap.put(
                                        featureMetaDataMap.get(key).getParentName(),
                                        children
                                );
                            }
                        }
                );


        return parentChildRelationshipMap;
    }
}
