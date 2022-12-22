package at.jku.cps.travart.plugin.ppr.dsl.transformation;

public final class DefaultPprDslTransformationProperties {

	public static final String NAME_ATTRIBUTE_KEY = "attribute_name";
	public static final String NAME_ATTRIBUTE_TYPE = "String";

	public static final String ATTRIBUTE_ID_KEY_PRAEFIX = "attribute_id_";
	public static final String ATTRIBUTE_ID_TYPE = "String";

	public static final String ATTRIBUTE_DESCRIPTION_KEY_PRAEFIX = "attribute_description_";
	public static final String ATTRIBUTE_DESCRIPTION_TYPE = "String";

	public static final String ATTRIBUTE_DEFAULT_VALUE_KEY_PRAEFIX = "attribute_default_value_";
	public static final String ATTRIBUTE_DEFAULT_VALUE_TYPE = "String";

	public static final String ATTRIBUTE_VALUE_KEY_PRAEFIX = "attribute_value_";
	public static final String ATTRIBUTE_VALUE_TYPE = "String";

	public static final String ATTRIBUTE_UNIT_KEY_PRAEFIX = "attribute_unit_";
	public static final String ATTRIBUTE_UNIT_TYPE = "String";

	public static final String ATTRIBUTE_TYPE_KEY_PRAEFIX = "attribute_type_";
	public static final String ATTRIBUTE_TYPE_TYPE = "String";

	public static final String IMPLEMENTED_PRODUCTS_LIST_SIZE = "implemented_products_list_size";
	public static final String IMPLEMENTED_PRODUCTS_LIST_SIZE_TYPE = "int";

	public static final String CHILDREN_PRODUCTS_LIST_SIZE = "children_products_list_size";
	public static final String CHILDREN_PRODUCTS_LIST_SIZE_TYPE = "int";

	public static final String IMPLEMENTED_PRODUCTS_LIST_NAME_NR_ = "implemented_products_list_nr_";
	public static final String IMPLEMENTED_PRODUCTS_LIST_NAME_NR_TYPE = "String";

	public static final String CHILDREN_PRODUCTS_LIST_NAME_NR_ = "children_products_list_nr_";
	public static final String CHILDREN_PRODUCTS_LIST_NAME_NR_TYPE = "String";

	public static final String PARTIAL_PRODUCT_ATTRIBUTE = "partialProduct";
	public static final String PARTIAL_PRODUCT_TYPE = "String";
	public static final String PARTIAL_PRODUCT_DESCRIPTION = "Specifies if the given product is a partial one";
	public static final String PARTIAL_PRODUCT_DEFAULT_VALUE = "false";

	public static final String DELTA_FILE = "deltaFile";
	public static final String DELTA_FILE_TYPE = "Collection";
	public static final String DELTA_FILE_DESCRIPTION = "Specifies delta file how the resource is build";
	public static final String DELTA_FILE_DEFAULT_VALUE = "";

	private DefaultPprDslTransformationProperties() {
	}
}
