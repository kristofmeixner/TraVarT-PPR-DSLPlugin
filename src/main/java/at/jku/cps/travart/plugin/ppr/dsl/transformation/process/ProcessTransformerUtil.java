package at.jku.cps.travart.plugin.ppr.dsl.transformation.process;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import at.jku.cps.travart.core.exception.NotSupportedVariabilityTypeException;
import at.jku.cps.travart.core.factory.impl.CoreModelFactory;
import at.jku.cps.travart.core.helpers.TraVarTUtils;
import at.jku.cps.travart.core.optimize.DefaultCoreModelOptimizer;
import at.jku.cps.travart.plugin.ppr.dsl.common.PprDslUtils;
import at.sqi.ppr.model.AssemblySequence;
import at.sqi.ppr.model.process.Process;
import at.sqi.ppr.model.relations.InputProductProcessRelation;
import at.sqi.ppr.model.relations.ResourceProcessRelation;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.Group;

public class ProcessTransformerUtil {

	private static final String PROCESS_ROOT = "process";
	private static final String VISIABLITY_CONDITION = "visibility";
	private static final String IS_SELECTED_CALL = "isSelected(";
	private static final String CLOSING_BRACKET = ")";
	private static final String AND = "&&";

	private ProcessTransformerUtil() {

	}

	private static CoreModelFactory factory;

	public static void createProcessFeatureModel(final CoreModelFactory coreFactory, final FeatureModel fm,
			final AssemblySequence asq) throws NotSupportedVariabilityTypeException {
		factory = Objects.requireNonNull(coreFactory);
		Objects.requireNonNull(fm);
		Objects.requireNonNull(asq);
		transformProcesses(fm, asq);
		deriveFeatureTree(fm, asq);
		transformConstraints(fm, asq);
		DefaultCoreModelOptimizer.getInstance().optimize(fm);
	}

	private static void transformProcesses(final FeatureModel fm, final AssemblySequence asq) {
		final Feature subroot = factory.createFeature(PROCESS_ROOT);
		TraVarTUtils.addFeature(fm, subroot);
		TraVarTUtils.setGroup(fm, subroot, TraVarTUtils.getRoot(fm), Group.GroupType.MANDATORY);
		for (Process process : asq.getProcesses().values()) {
			final Feature feature = factory.createFeature(process.getId());
			if (process.isAbstract()) {
				TraVarTUtils.setAbstract(feature, true);
			}
			deriveVisibilityConditions(feature, process, asq);
			TraVarTUtils.addFeature(fm, feature);
			TraVarTUtils.setGroup(fm, feature, subroot, Group.GroupType.OPTIONAL);
		}
	}

	private static void deriveVisibilityConditions(final Feature feature, final Process process,
			final AssemblySequence asq) {
		if (process.isAbstract()) {
			TraVarTUtils.addAttribute(feature, VISIABLITY_CONDITION, false);
			return;
		}
		List<InputProductProcessRelation> inputRelations = asq.getInputRelationsOfProcess(process);
		String condition = String.valueOf(false);
		if (!inputRelations.isEmpty()) {
			InputProductProcessRelation relation = inputRelations.remove(0);
			condition = String.join("", IS_SELECTED_CALL, relation.getProduct().getId(), CLOSING_BRACKET);
			for (InputProductProcessRelation visIppr : inputRelations) {
				condition = String.join(" ", condition, AND, " ");
				condition = String.join("", condition, IS_SELECTED_CALL, visIppr.getProduct().getId(), CLOSING_BRACKET);
			}
			// add removed one back in
			inputRelations.add(relation);
		}
		List<Process> visibilityProcesses = process.getRequires();
		if (!visibilityProcesses.isEmpty()) {
			for (Process visP : visibilityProcesses) {
				condition = String.join(" ", condition, AND, " ");
				condition = String.join("", condition, IS_SELECTED_CALL, visP.getId(), CLOSING_BRACKET);
			}
		}
		TraVarTUtils.addAttribute(feature, VISIABLITY_CONDITION, condition);
	}

	private static void deriveFeatureTree(final FeatureModel fm, final AssemblySequence asq) {
		for (Process process : asq.getProcesses().values()) {
			if (PprDslUtils.implementsSingleProcess(process)) {
				final Feature childFeature = TraVarTUtils.getFeature(fm, process.getId());
				final Process parentProcess = PprDslUtils.getFirstImplementedProcess(process);
				final Feature parentFeature = TraVarTUtils.getFeature(fm, parentProcess.getId());
				TraVarTUtils.setGroup(fm, childFeature, parentFeature, Group.GroupType.OPTIONAL);
			}
		}
	}

	private static void transformConstraints(final FeatureModel fm, final AssemblySequence asq) {
		for (Process process : asq.getProcesses().values()) {
			// requires constraints
			createRequiresConstraints(fm, process);
			// excludes constraints
			createExcludeConstraints(fm, process);
		}
	}

	private static void createRequiresConstraints(final FeatureModel fm, final Process process) {
		final Feature conditionalFeature = TraVarTUtils.getFeature(fm, process.getId());
		// required rules from requires filed
		for (final Process required : process.getRequires()) {
			final Feature requiredFeature = TraVarTUtils.getFeature(fm, required.getId());
			if (requiredFeature != null && !TraVarTUtils.isParentOf(conditionalFeature, requiredFeature)) {
				TraVarTUtils.addOwnConstraint(fm,
						factory.createImplicationConstraint(
								factory.createLiteralConstraint(TraVarTUtils.getFeatureName(conditionalFeature)),
								factory.createLiteralConstraint(TraVarTUtils.getFeatureName(requiredFeature))));
			}
		}
		// required rules from resources
		Collection<ResourceProcessRelation> resoruceRelations = process.getResourceProcessRelations().values();
		for (final ResourceProcessRelation required : resoruceRelations) {
			final Feature requiredFeature = TraVarTUtils.getFeature(fm, required.getResource().getId());
			if (requiredFeature != null && !TraVarTUtils.isParentOf(conditionalFeature, requiredFeature)) {
				TraVarTUtils.addOwnConstraint(fm,
						factory.createImplicationConstraint(
								factory.createLiteralConstraint(TraVarTUtils.getFeatureName(conditionalFeature)),
								factory.createLiteralConstraint(TraVarTUtils.getFeatureName(requiredFeature))));
			}
		}
	}

	private static void createExcludeConstraints(final FeatureModel fm, final Process process) {
		final Feature conditionalFeature = TraVarTUtils.getFeature(fm, process.getId());
		for (final Process excluded : process.getExcludes()) {
			final Feature excludedFeature = TraVarTUtils.getFeature(fm, excluded.getId());
			if (excludedFeature != null && !TraVarTUtils.isParentOf(conditionalFeature, excludedFeature)) {
				TraVarTUtils.addOwnConstraint(fm, factory.createImplicationConstraint(
						factory.createLiteralConstraint(TraVarTUtils.getFeatureName(conditionalFeature)),
						factory.createNotConstraint(
								factory.createLiteralConstraint(TraVarTUtils.getFeatureName(excludedFeature)))));
			}
		}

	}
}
