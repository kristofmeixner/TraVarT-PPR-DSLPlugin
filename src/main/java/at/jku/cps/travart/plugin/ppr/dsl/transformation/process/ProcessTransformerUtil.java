package at.jku.cps.travart.plugin.ppr.dsl.transformation.process;

import java.util.Objects;

import at.jku.cps.travart.core.exception.NotSupportedVariabilityTypeException;
import at.jku.cps.travart.core.factory.impl.CoreModelFactory;
import at.jku.cps.travart.core.helpers.TraVarTUtils;
import at.sqi.ppr.model.AssemblySequence;
import at.sqi.ppr.model.process.Process;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.Group;

public class ProcessTransformerUtil {

	private static final String PROCESS_ROOT = "process";

	private ProcessTransformerUtil() {

	}

	private static CoreModelFactory factory;

	public static void createProcessFeatureModel(final CoreModelFactory coreFactory, final FeatureModel fm,
			final AssemblySequence asq) throws NotSupportedVariabilityTypeException {
		factory = Objects.requireNonNull(coreFactory);
		Objects.requireNonNull(fm);
		Objects.requireNonNull(asq);
		transformProcesses(fm, asq);
	}

	private static void transformProcesses(final FeatureModel fm, final AssemblySequence asq) {
		final Feature parent = factory.createFeature(PROCESS_ROOT);
		TraVarTUtils.addFeature(fm, parent);
		for (Process process : asq.getProcesses().values()) {
			final Feature feature = factory.createFeature(process.getId());
			if (process.isAbstract()) {
				TraVarTUtils.setAbstract(feature, true);
			}
			deriveVisibilityConditions(feature, process);
			TraVarTUtils.addFeature(fm, feature);
			TraVarTUtils.setGroup(fm, feature, parent, Group.GroupType.OR);
		}
	}

	private static void deriveVisibilityConditions(final Feature feature, final Process process) {

	}
}
