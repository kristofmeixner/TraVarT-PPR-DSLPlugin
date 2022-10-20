package at.jku.cps.travart.plugin.ppr.dsl.io;

import at.jku.cps.travart.core.common.IReader;
import at.jku.cps.travart.core.exception.NotSupportedVariabilityTypeException;
import at.sqi.ppr.dsl.reader.DslReader;
import at.sqi.ppr.dsl.reader.exceptions.DslParsingException;
import at.sqi.ppr.model.AssemblySequence;

import java.io.IOException;
import java.nio.file.Path;

public class PprDslReader implements IReader<AssemblySequence> {

    @Override
    public AssemblySequence read(final Path path) throws IOException, NotSupportedVariabilityTypeException {
        final DslReader dslReader = new DslReader();
        try {
            return dslReader.readDsl(path.toString());
        } catch (final DslParsingException e) {
            throw new NotSupportedVariabilityTypeException(e);
        }
    }
}
