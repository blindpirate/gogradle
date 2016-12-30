package com.github.blindpirate.gogradle.core.dependency.external.glock

import com.github.blindpirate.gogradle.GogradleRunner
import com.github.blindpirate.gogradle.core.dependency.external.ExternalDependencyFactoryTest
import com.github.blindpirate.gogradle.util.IOUtils
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks

import static org.mockito.ArgumentMatchers.anyMap
import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify

@RunWith(GogradleRunner)
class GlockDependencyFactoryTest extends ExternalDependencyFactoryTest {
    @InjectMocks
    GlockDependencyFactory factory

    @Test
    void 'package without GLOCKFILE should be rejected'() {
        assert !factory.produce(module).isPresent()
    }

    String GLOCKFILE = '''
bitbucket.org/tebeka/selenium 02df1758050f
code.google.com/p/cascadia 4f03c71bc42b
code.google.com/p/go-uuid 7dda39b2e7d5
'''

    @Test
    void 'parsing GLOCKFILE should success'() {
        // given
        prepareGlockfile(GLOCKFILE)
        // when
        factory.produce(module)
        // then
        verifyMapParsed([name: 'bitbucket.org/tebeka/selenium', revision: '02df1758050f'])
        verifyMapParsed([name: 'code.google.com/p/cascadia', revision: '4f03c71bc42b'])
        verifyMapParsed([name: 'code.google.com/p/go-uuid', revision: '7dda39b2e7d5'])
    }

    String glockfileWithCmds = '''
cmd code.google.com/p/go.tools/cmd/godoc
cmd code.google.com/p/go.tools/cmd/goimports
cmd code.google.com/p/go.tools/cmd/vet
bitbucket.org/tebeka/selenium 02df1758050f
code.google.com/p/cascadia 4f03c71bc42b
code.google.com/p/go-uuid 7dda39b2e7d5
'''

    @Test
    void 'cmd lines should be ignored'() {
        // given
        prepareGlockfile(glockfileWithCmds)
        // when
        factory.produce(module)
        // then
        verify(mapNotationParser, times(3)).parse(anyMap())
    }

    String glockfileWithCorruptedLine = '''
This is a corrupted line
'''

    @Test(expected = RuntimeException)
    void 'unrecognized line should cause an exception'() {
        // given
        prepareGlockfile(glockfileWithCorruptedLine)
        // then
        factory.produce(module)
    }

    void prepareGlockfile(String s) {
        IOUtils.write(resource, "GLOCKFILE", s)
    }
}
