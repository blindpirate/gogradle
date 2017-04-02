package com.github.blindpirate.gogradle.core.dependency;

import com.github.blindpirate.gogradle.core.GolangPackage;
import com.github.blindpirate.gogradle.core.UnrecognizedGolangPackage;

import java.util.Set;
import java.util.function.Predicate;

public class UnrecognizedPackageNotationDependency extends AbstractGolangDependency implements NotationDependency {

    private UnrecognizedGolangPackage pkg;

    public static UnrecognizedPackageNotationDependency of(UnrecognizedGolangPackage pkg) {
        UnrecognizedPackageNotationDependency ret = new UnrecognizedPackageNotationDependency();
        ret.pkg = pkg;
        ret.setName(pkg.getPathString());
        return ret;
    }

    private UnrecognizedPackageNotationDependency() {
    }

    @Override
    public boolean isFirstLevel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public GolangPackage getPackage() {
        return pkg;
    }

    @Override
    public Set<Predicate<GolangDependency>> getTransitiveDepExclusions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResolvedDependency resolve(ResolveContext context) {
        throw new UnsupportedOperationException("Cannot resolve package: " + getName());
    }
}
