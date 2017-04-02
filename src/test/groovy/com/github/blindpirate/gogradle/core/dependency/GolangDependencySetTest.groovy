package com.github.blindpirate.gogradle.core.dependency

import com.github.blindpirate.gogradle.GogradleRunner
import com.github.blindpirate.gogradle.support.WithResource
import com.github.blindpirate.gogradle.util.IOUtils
import com.github.blindpirate.gogradle.util.ReflectionUtils
import com.github.blindpirate.gogradle.vcs.git.GitNotationDependency
import org.gradle.api.Action
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.specs.Spec
import org.junit.Test
import org.junit.runner.RunWith

import static com.github.blindpirate.gogradle.util.DependencyUtils.asGolangDependencySet
import static com.github.blindpirate.gogradle.util.DependencyUtils.mockResolvedDependency
import static org.mockito.Mockito.*

@RunWith(GogradleRunner)
class GolangDependencySetTest {

    File resource

    @Test
    void 'flattening should succeed'() {
        // given
        ResolvedDependency d1 = mockResolvedDependency('d1')
        ResolvedDependency d2 = mockResolvedDependency('d2')
        ResolvedDependency d3 = mockResolvedDependency('d3')
        GolangDependencySet set1 = asGolangDependencySet(d1)
        GolangDependencySet set2 = asGolangDependencySet(d2)
        GolangDependencySet set3 = asGolangDependencySet(d3)
        // when
        when(d1.getDependencies()).thenReturn(set2)
        when(d2.getDependencies()).thenReturn(set3)
        when(d3.getDependencies()).thenReturn(GolangDependencySet.empty())
        // then
        assert set1.flatten() as Set == [d1, d2, d3] as Set
    }

    @Test(expected = IllegalStateException)
    void 'flattening should fail if max recursion depth is reached'() {
        // given
        ResolvedDependency resolvedDependency = mockResolvedDependency('resolvedDependency')
        GolangDependencySet set = asGolangDependencySet(resolvedDependency)
        when(resolvedDependency.getDependencies()).thenReturn(set)
        // when
        set.flatten()
    }

    void assertUnsupport(Closure closure) {
        try {
            closure.call()
        }
        catch (UnsupportedOperationException e) {
            return
        }
        assert false
    }

    @Test
    void 'all methods of facade should be delegated to itself'() {
        GolangDependencySet set = new GolangDependencySet()
        DependencySet facade = set.toDependencySet()
        GolangDependencySet mockDependencySet = mock(GolangDependencySet)
        ReflectionUtils.setField(facade, 'outerInstance', mockDependencySet)

        assertUnsupport { facade.withType(String) }

        Action action = mock(Action)
        assertUnsupport { facade.withType(String, action) }

        Closure closure = mock(Closure)
        assertUnsupport { facade.withType(String, closure) }

        Spec spec = mock(Spec)
        assertUnsupport { facade.matching(spec) }

        assertUnsupport { facade.matching(closure) }

        assertUnsupport { facade.whenObjectAdded(action) }

        assertUnsupport { facade.whenObjectAdded(closure) }

        assertUnsupport { facade.whenObjectRemoved(action) }

        assertUnsupport { facade.whenObjectRemoved(closure) }

        assertUnsupport { facade.all(action) }

        assertUnsupport { facade.all(closure) }

        assertUnsupport { facade.findAll(closure) }

        assertUnsupport { facade.getBuildDependencies() }

        facade.isEmpty()
        verify(mockDependencySet).isEmpty()

        facade.contains('')
        verify(mockDependencySet).contains('')

        facade.toArray()
        verify(mockDependencySet).toArray()

        Object[] array = [] as Object[]
        facade.toArray(array)
        verify(mockDependencySet).toArray(array)

        facade.remove('')
        verify(mockDependencySet).remove('')

        facade.containsAll([])
        verify(mockDependencySet).containsAll([])

        facade.removeAll([])
        verify(mockDependencySet).removeAll([])

        facade.retainAll([])
        verify(mockDependencySet).retainAll([])

        facade.addAll([])
        verify(mockDependencySet).addAll([])

        facade.clear()
        verify(mockDependencySet).clear()
    }

    @Test(expected = UnsupportedOperationException)
    void 'exception should be thrown when invoking some methods'() {
        new GolangDependencySet().toDependencySet().getBuildDependencies()
    }

    @Test
    @WithResource('')
    void 'serialization and deserialization should succeed'() {
        // given
        File output = new File(resource, 'output.bin')
        IOUtils.touch(output)

        GolangDependencySet set = new GolangDependencySet()
        set.add(newGitDependency())
        set.add(LocalDirectoryDependency.fromLocal('resource', resource))
        set.add(newVendorDependency())

        // when
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(output))
        oos.writeObject(set)

        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(output))
        def result = ois.readObject()

        // then
        assert result instanceof GolangDependencySet

        def gitDependency = result.find { it instanceof GitNotationDependency }
        assert gitDependency.name == 'gitDependency'
        assert gitDependency.commit == 'commit'
        assert gitDependency.urls == ['url']
        assert gitDependency.firstLevel

        def vendorDependency = result.find { it instanceof VendorNotationDependency }
        assert vendorDependency.name == 'vendorDependency'
        assert vendorDependency.hostNotationDependency.commit == 'commit'
        assert vendorDependency.hostNotationDependency.urls == ['url']
        assert vendorDependency.hostNotationDependency.firstLevel
        assert vendorDependency.vendorPath == 'vendor/path'

        def localDependency = result.find { it instanceof LocalDirectoryDependency }
        assert localDependency.name == 'resource'
        assert localDependency.rootDir == resource
    }

    VendorNotationDependency newVendorDependency() {
        VendorNotationDependency ret = new VendorNotationDependency()
        ret.name = 'vendorDependency'
        ret.hostNotationDependency = newGitDependency()
        ret.vendorPath = 'vendor/path'
        return ret
    }

    GitNotationDependency newGitDependency() {
        GitNotationDependency ret = new GitNotationDependency()
        ret.name = 'gitDependency'
        ret.commit = 'commit'
        ret.url = 'url'
        ret.firstLevel = true
        return ret
    }
}
