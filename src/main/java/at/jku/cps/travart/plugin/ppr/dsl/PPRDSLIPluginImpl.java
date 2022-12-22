package at.jku.cps.travart.plugin.ppr.dsl;

import java.util.List;

import org.pf4j.Extension;

import at.jku.cps.travart.core.common.IModelTransformer;
import at.jku.cps.travart.core.common.IPlugin;
import at.jku.cps.travart.core.common.IReader;
import at.jku.cps.travart.core.common.IWriter;
import at.jku.cps.travart.plugin.ppr.dsl.io.PprDslReader;
import at.jku.cps.travart.plugin.ppr.dsl.io.PprDslWriter;
import at.jku.cps.travart.plugin.ppr.dsl.transformation.PprModelTransformerImpl;

@Extension
@SuppressWarnings("rawtypes")
public class PPRDSLIPluginImpl implements IPlugin {

	public static final String ID = "ppr-dsl-plugin";

	@Override
	public IModelTransformer getTransformer() {
		return new PprModelTransformerImpl();
	}

	@Override
	public IReader getReader() {
		return new PprDslReader();
	}

	@Override
	public IWriter getWriter() {
		return new PprDslWriter();
	}

	@Override
	public String getName() {
		return "PPR-DSL";
	}

	@Override
	public String getVersion() {
		return "0.0.1";
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public List<String> getSupportedFileExtensions() {
		return List.of(".dsl");
	}
}
