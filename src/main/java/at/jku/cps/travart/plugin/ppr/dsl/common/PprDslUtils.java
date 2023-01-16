package at.jku.cps.travart.plugin.ppr.dsl.common;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties;
import at.sqi.ppr.model.AssemblySequence;
import at.sqi.ppr.model.process.Process;
import at.sqi.ppr.model.product.Product;
import at.sqi.ppr.model.resource.Resource;

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
		logger.log(Level.INFO, "#Products: {0}", seq.getProducts() != null ? seq.getProducts().size() : 0);
		logger.log(Level.INFO, "#Variant Products: {0}",
				seq.getProducts() != null ? countVariantProducts(seq.getProducts()) : 0);
		logger.log(Level.INFO, "#Partial Products: {0}",
				seq.getProducts() != null ? countPartialProducts(seq.getProducts()) : 0);
		logger.log(Level.INFO, "#Abstract Products: {0}",
				seq.getProducts() != null ? countAbstractProducts(seq.getProducts()) : 0);
		logger.log(Level.INFO, "#Implementing Products: {0}",
				seq.getProducts() != null ? countImplementingProducts(seq.getProducts()) : 0);
		logger.log(Level.INFO, "#Constrainted Products: {0}",
				seq.getProducts() != null ? countConstraintedProducts(seq.getProducts()) : 0);
		logger.log(Level.INFO, "#Constrainted requries Products: {0}",
				seq.getProducts() != null ? countConstraintedRequieresProducts(seq.getProducts()) : 0);
		logger.log(Level.INFO, "#Constrainted excludes Products: {0}",
				seq.getProducts() != null ? countConstraintedExcludesProducts(seq.getProducts()) : 0);
		logger.log(Level.INFO, "#Processes: {0}", seq.getProcesses() != null ? seq.getProcesses().size() : 0);
		logger.log(Level.INFO, "#Constraints: {0}", seq.getConstraints() != null ? seq.getConstraints().size() : 0);
		logger.log(Level.INFO, "#Global Constraints: {0}",
				seq.getGlobalConstraints() != null ? seq.getGlobalConstraints().size() : 0);
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

	public static long countConstraintedExcludesProducts(final Map<String, Product> products) {
		return products.values().stream().filter(p -> !p.getExcludes().isEmpty()).count();
	}

	public static long countConstraintedRequieresProducts(final Map<String, Product> products) {
		return products.values().stream().filter(p -> !p.getRequires().isEmpty()).count();
	}

	public static long countConstraintedProducts(final Map<String, Product> products) {
		return products.values().stream().filter(p -> !p.getRequires().isEmpty() || !p.getExcludes().isEmpty()).count();
	}

	public static long countImplementingProducts(final Map<String, Product> products) {
		return products.values().stream().filter(p -> !p.getImplementedProducts().isEmpty()).count();
	}

	public static long countAbstractProducts(final Map<String, Product> products) {
		return products.values().stream().filter(Product::isAbstract).count();
	}

	public static long countPartialProducts(final Map<String, Product> products) {
		return products.values().stream().filter(
				p -> p.getAttributes().containsKey(DefaultPprDslTransformationProperties.PARTIAL_PRODUCT_ATTRIBUTE)
						&& Boolean.parseBoolean(
								p.getAttributes().get(DefaultPprDslTransformationProperties.PARTIAL_PRODUCT_ATTRIBUTE)
										.getValue().getValue().toString()))
				.count();
	}

	public static long countVariantProducts(final Map<String, Product> products) {
		return products.size() - countPartialProducts(products);
	}

	public static boolean isPartialProduct(final Product product) {
		return Objects.requireNonNull(product).getAttributes()
				.containsKey(DefaultPprDslTransformationProperties.PARTIAL_PRODUCT_ATTRIBUTE)
				&& Boolean.parseBoolean(
						product.getAttributes().get(DefaultPprDslTransformationProperties.PARTIAL_PRODUCT_ATTRIBUTE)
								.getValue().getValue().toString());
	}

	public static boolean hasAttributeSpecified(final Product product, final String attributeKey) {
		return Objects.requireNonNull(product).getAttributes().containsKey(attributeKey);
	}

	public static boolean hasAttributeSpecified(final Process process, final String attributeKey) {
		return Objects.requireNonNull(process).getAttributes().containsKey(attributeKey);
	}

	public static boolean hasAttributeSpecified(final Resource resource, final String attributeKey) {
		return Objects.requireNonNull(resource).getAttributes().containsKey(attributeKey);
	}

	public static Object getAttributeValue(final Product product, final String key) {
		return Objects.requireNonNull(product).getAttributes().get(key).getValue().getValue();
	}

	public static Object getAttributeValue(final Process process, final String key) {
		return Objects.requireNonNull(process).getAttributes().get(key).getValue().getValue();
	}

	public static Object getAttributeValue(final Resource resource, final String key) {
		return Objects.requireNonNull(resource).getAttributes().get(key).getValue().getValue();
	}

	public static boolean hasChildren(final Product product) {
		return !product.getChildProducts().isEmpty();
	}

	// TODO: similar implementation for all three areas - generalize! Talk to
	// Kristof regarding common interface
	public static boolean implementsSingleProduct(final Product product) {
		return !Objects.requireNonNull(product).getImplementedProducts().isEmpty()
				&& product.getImplementedProducts().size() == 1;
	}

	public static boolean implementsSingleResource(final Resource resource) {
		return !Objects.requireNonNull(resource).getImplementedResources().isEmpty()
				&& resource.getImplementedResources().size() == 1;
	}

	public static boolean implementsSingleProcess(final Process process) {
		return !Objects.requireNonNull(process).getImplementedProcesses().isEmpty()
				&& process.getImplementedProcesses().size() == 1;
	}

	public static Product getFirstImplementedProduct(final Product product) {
		return Objects.requireNonNull(product).getImplementedProducts().get(0);
	}

	public static Resource getFirstImplementedResource(final Resource resource) {
		return Objects.requireNonNull(resource).getImplementedResources().get(0);
	}

	public static Process getFirstImplementedProcess(final Process process) {
		return Objects.requireNonNull(process).getImplementedProcesses().get(0);
	}

}
