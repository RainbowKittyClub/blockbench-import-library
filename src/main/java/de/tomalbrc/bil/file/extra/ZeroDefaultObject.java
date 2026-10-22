package de.tomalbrc.bil.file.extra;

import gg.moonflower.molangcompiler.api.MolangEnvironment;
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.exception.MolangRuntimeException;
import gg.moonflower.molangcompiler.api.object.MolangObject;

import java.util.Collection;

/**
 * A Molang namespace that reads a name nothing has set as 0, as Blockbench does, where
 * molang-compiler would throw and fail the whole import.
 */
public record ZeroDefaultObject(MolangObject inner) implements MolangObject {
    public static void install(MolangEnvironment environment) throws MolangRuntimeException {
        environment.loadLibrary("query", new ZeroDefaultObject(environment.get("query")), "q");
        environment.loadLibrary("variable", new ZeroDefaultObject(environment.get("variable")), "v");
        environment.loadLibrary("context", new ZeroDefaultObject(environment.get("context")), "c");
    }

    @Override
    public MolangExpression get(String name) throws MolangRuntimeException {
        return this.inner.has(name) ? this.inner.get(name) : MolangExpression.ZERO;
    }

    @Override
    public void set(String name, MolangExpression value) throws MolangRuntimeException {
        this.inner.set(name, value);
    }

    @Override
    public void remove(String name) throws MolangRuntimeException {
        this.inner.remove(name);
    }

    @Override
    public boolean has(String name) {
        return this.inner.has(name);
    }

    @Override
    public Collection<String> getKeys() {
        return this.inner.getKeys();
    }

    @Override
    public MolangObject getCopy() {
        return new ZeroDefaultObject(this.inner.getCopy());
    }

    @Override
    public boolean isMutable() {
        return this.inner.isMutable();
    }
}
