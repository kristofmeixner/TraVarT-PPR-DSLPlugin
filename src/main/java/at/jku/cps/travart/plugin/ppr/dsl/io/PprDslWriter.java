package at.jku.cps.travart.plugin.ppr.dsl.io;

import at.jku.cps.travart.core.common.IWriter;
import at.jku.cps.travart.core.exception.NotSupportedVariabilityTypeException;
import at.sqi.ppr.dsl.serializer.AssemblySequenceDslSerializer;
import at.sqi.ppr.dsl.serializer.AttributeDslSerializer;
import at.sqi.ppr.dsl.serializer.ConstraintDslSerializer;
import at.sqi.ppr.dsl.serializer.DslSerializerException;
import at.sqi.ppr.dsl.serializer.ProcessDslSerializer;
import at.sqi.ppr.dsl.serializer.ProductDslSerializer;
import at.sqi.ppr.dsl.serializer.ResourceDslSerializer;
import at.sqi.ppr.model.AssemblySequence;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class PprDslWriter implements IWriter<AssemblySequence> {

    public static final String PPR_DSL_EXTENSION = ".dsl";

    @Override
    public void write(final AssemblySequence asq, final Path path) throws IOException, NotSupportedVariabilityTypeException {
        final AttributeDslSerializer attributeWriter = new AttributeDslSerializer();
        final ProductDslSerializer productWriter = new ProductDslSerializer();
        final ResourceDslSerializer resourceWriter = new ResourceDslSerializer();
        final ProcessDslSerializer processWriter = new ProcessDslSerializer();
        final ConstraintDslSerializer constraintWriter = new ConstraintDslSerializer();
        final AssemblySequenceDslSerializer asqWriter = new AssemblySequenceDslSerializer(attributeWriter, productWriter,
                resourceWriter, processWriter, constraintWriter);
        try (final FileWriter writer = new FileWriter(path.toFile(), StandardCharsets.UTF_8)) {
            final String asqString = asqWriter.serializeAssemblySequence(asq);
            writer.append(asqString);
            writer.flush();
        } catch (final DslSerializerException e) {
            throw new IOException(e);
        }
    }
}
