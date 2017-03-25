package com.github.blindpirate.gogradle.vcs

import com.github.blindpirate.gogradle.GogradleRunner
import com.github.blindpirate.gogradle.core.GolangConfiguration
import com.github.blindpirate.gogradle.core.GolangPackage
import com.github.blindpirate.gogradle.core.VcsGolangPackage
import com.github.blindpirate.gogradle.core.cache.GlobalCacheManager
import com.github.blindpirate.gogradle.core.dependency.*
import com.github.blindpirate.gogradle.core.dependency.produce.DependencyVisitor
import com.github.blindpirate.gogradle.core.dependency.produce.strategy.DependencyProduceStrategy
import com.github.blindpirate.gogradle.core.exceptions.DependencyInstallationException
import com.github.blindpirate.gogradle.core.exceptions.DependencyResolutionException
import com.github.blindpirate.gogradle.support.MockOffline
import com.github.blindpirate.gogradle.support.WithMockInjector
import com.github.blindpirate.gogradle.support.WithResource
import com.github.blindpirate.gogradle.util.DependencyUtils
import com.github.blindpirate.gogradle.util.IOUtils
import com.github.blindpirate.gogradle.util.ReflectionUtils
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock

import java.nio.file.Paths
import java.util.concurrent.Callable

import static com.github.blindpirate.gogradle.core.dependency.resolve.AbstractVcsDependencyManagerTest.callCallableAnswer
import static com.github.blindpirate.gogradle.util.DependencyUtils.mockWithName
import static java.util.Optional.empty
import static java.util.Optional.of
import static org.mockito.Matchers.any
import static org.mockito.Matchers.anyString
import static org.mockito.Mockito.*

@RunWith(GogradleRunner)
@WithResource('')
@WithMockInjector
class GitMercurialDependencyManagerTest {

    GitMercurialNotationDependency notationDependency = mockWithName(GitMercurialNotationDependency, 'github.com/a/b')
    GitMercurialResolvedDependency resolvedDependency = mockWithName(GitMercurialResolvedDependency, 'github.com/a/b')

    String DEFAULT_BRANCH = 'DEFAULT_BRANCH'

    @Mock
    GlobalCacheManager cacheManager
    @Mock
    GitMercurialAccessor accessor
    @Mock
    GolangDependencySet dependencySet
    @Mock
    DependencyProduceStrategy strategy
    @Mock
    Set exclusionSpecs
    @Mock
    GolangConfiguration configuration
    @Mock
    DependencyRegistry dependencyRegistry
    @Mock
    GitMercurialCommit commit

    GitMercurialDependencyManager manager

    String commitId = '1' * 40
    String repoUrl = 'https://github.com/a/b.git'
    GolangPackage thePackage = VcsGolangPackage.builder()
            .withPath('github.com/a/b')
            .withRootPath('github.com/a/b')
            .withVcsType(VcsType.GIT)
            .withUrl(repoUrl)
            .build()

    File resource

    @Before
    void setUp() {
        // prevent ensureGlobalCacheEmptyOrMatch from returning directly
        IOUtils.write(resource, '.git', '')

        manager = new TestGitMercurialDependencyManager(cacheManager, mock(DependencyVisitor), accessor)

        when(configuration.getDependencyRegistry()).thenReturn(dependencyRegistry)

        when(cacheManager.runWithGlobalCacheLock(any(GolangDependency), any(Callable))).thenAnswer(callCallableAnswer)
        when(cacheManager.getGlobalPackageCachePath(anyString())).thenReturn(resource.toPath())
        when(accessor.findCommit(resource, commitId)).thenReturn(of(commit))
        when(accessor.headCommitOfBranch(resource, 'MockDefault')).thenReturn(commit)
        when(commit.getId()).thenReturn(commitId)
        when(commit.getCommitTime()).thenReturn(123000L)

        when(accessor.getRemoteUrl(resource)).thenReturn(repoUrl)
        when(notationDependency.getStrategy()).thenReturn(strategy)
        when(strategy.produce(any(ResolvedDependency), any(File), any(DependencyVisitor), anyString())).thenReturn(dependencySet)

        when(notationDependency.getTransitiveDepExclusions()).thenReturn(exclusionSpecs)

        when(notationDependency.getUrls()).thenReturn([repoUrl])
        when(notationDependency.getCommit()).thenReturn(commitId)
        when(notationDependency.getPackage()).thenReturn(thePackage)
    }

    @Test
    void 'notation dependency should be created successfully'() {
        // given
        when(notationDependency.getTag()).thenReturn('tag')
        when(notationDependency.isFirstLevel()).thenReturn(true)
        // when
        ResolvedDependency result = manager.createResolvedDependency(notationDependency, resource, commit)
        // then
        assertResolvedDependency(result)
    }

    void assertResolvedDependency(ResolvedDependency result) {
        assert result.name == 'github.com/a/b'
        assert result.dependencies.isEmpty()
        assert ReflectionUtils.getField(result, 'repoUrl') == repoUrl
        assert ReflectionUtils.getField(result, 'tag') == 'tag'
        assert result.version == commitId
        assert result.updateTime == 123000L
        assert result.firstLevel
        assert ReflectionUtils.getField(result, 'transitiveDepExclusions').is(exclusionSpecs)
    }

    @Test
    void 'git notation dependency with non-root name should be created successfully'() {
        // given
        when(notationDependency.getName()).thenReturn('github.com/a/b/c')
        when(notationDependency.getPackage()).thenReturn(thePackage.resolve(Paths.get('github.com/a/b/c')).get())
        when(notationDependency.getTag()).thenReturn('tag')
        when(notationDependency.isFirstLevel()).thenReturn(true)
        // when
        ResolvedDependency result = manager.createResolvedDependency(notationDependency, resource, commit)
        // then
        assertResolvedDependency(result)
    }

    @Test
    void 'update time of vendor dependency should be set to last commit time of that directory'() {
        // given
        VendorResolvedDependency vendorResolvedDependency = mockWithName(VendorResolvedDependency, 'vendorResolvedDependency')
        GolangDependencySet dependencies = DependencyUtils.asGolangDependencySet(vendorResolvedDependency)
        when(strategy.produce(any(ResolvedDependency), any(File), any(DependencyVisitor), anyString())).thenReturn(dependencies)
        when(vendorResolvedDependency.getHostDependency()).thenReturn(resolvedDependency)
        when(vendorResolvedDependency.getRelativePathToHost()).thenReturn(Paths.get('vendor/path/to/vendor'))
        when(vendorResolvedDependency.getDependencies()).thenReturn(GolangDependencySet.empty())
        when(accessor.lastCommitTimeOfPath(resource, 'vendor/path/to/vendor')).thenReturn(456L)
        // when
        manager.resolve(configuration, notationDependency)
        // then
        verify(vendorResolvedDependency).setUpdateTime(456L)
    }

    @Test
    void 'existed repository should be updated'() {
        // given:
        when(cacheManager.currentDependencyIsOutOfDate(notationDependency)).thenReturn(true)
        when(accessor.getRemoteUrl(resource)).thenReturn(repoUrl)
        // when:
        manager.resolve(configuration, notationDependency)
        // then:
        verify(accessor).pull(resource)
    }

    @Test
    void 'empty repository should be cloned'() {
        // given
        IOUtils.clearDirectory(resource)
        // when
        manager.resolve(configuration, notationDependency)
        // then
        verify(accessor).clone(repoUrl, resource)
    }

    @Test
    @MockOffline
    void 'pull should not be executed if offline'() {
        // when:
        manager.resolve(configuration, notationDependency)
        // then:
        verify(accessor, times(0)).pull(resource)
    }

    @Test
    void 'dependency with tag should be resolved successfully'() {
        // given
        when(notationDependency.getTag()).thenReturn('tag')
        when(accessor.findCommitByTag(resource, 'tag')).thenReturn(of(commit))
        // when
        manager.resolve(configuration, notationDependency)
        // then
        verify(accessor).checkout(resource, commitId)
    }

    @Test
    void 'tag should be interpreted as sem version if commit not found'() {
        // given
        when(notationDependency.getTag()).thenReturn('~1.0.0')
        when(accessor.findCommitByTag(resource, '~1.0.0')).thenReturn(empty())
        GitMercurialCommit satisfiedCommit = GitMercurialCommit.of('commitId', '1.0.1', 321L)
        when(accessor.getAllTags(resource)).thenReturn([satisfiedCommit])
        // then
        assert manager.determineVersion(resource, notationDependency).is(satisfiedCommit)
    }

    @Test
    void 'commit will be searched if tag cannot be recognized'() {
        // given
        when(notationDependency.getCommit()).thenReturn(null)
        // when
        manager.determineVersion(resource, notationDependency)
        // then
        verify(accessor).headCommitOfBranch(resource, DEFAULT_BRANCH)
    }

    @Test
    void 'NEWEST_COMMIT should be recognized properly'() {
        // given
        when(notationDependency.getCommit()).thenReturn(GitMercurialNotationDependency.NEWEST_COMMIT)
        // when
        manager.determineVersion(resource, notationDependency)
        // then
        verify(accessor).headCommitOfBranch(resource, DEFAULT_BRANCH)
    }

    @Test(expected = DependencyResolutionException)
    void 'exception should be thrown when every url has been tried'() {
        // given
        when(notationDependency.getUrls()).thenReturn(['url1', 'url2'])
        when(accessor.clone('url1', resource)).thenThrow(new IllegalStateException())
        when(accessor.clone('url2', resource)).thenThrow(new IllegalStateException())
        // when
        manager.initRepository(notationDependency, resource)
    }

    @Test
    void 'every url should be tried until success'() {
        // given
        when(notationDependency.getUrls()).thenReturn(['url1', 'url2'])
        when(accessor.clone('url1', resource)).thenThrow(IOException)
        // when
        manager.resolve(configuration, notationDependency)
        // then
        verify(accessor).clone('url2', resource)
    }

    @Test
    void 'resetting to a commit should succeed'() {
        // when
        manager.resetToSpecificVersion(resource, commit)
        // then
        verify(accessor).checkout(resource, commitId)
    }

    @Test(expected = DependencyResolutionException)
    void 'trying to resolve an inexistent commit should result in an exception'() {
        // given
        when(notationDependency.getCommit()).thenReturn('inexistent')
        // when
        manager.resolve(configuration, notationDependency)
    }

    @Test(expected = DependencyResolutionException)
    void 'exception in locked block should not be swallowed'() {
        // given
        when(cacheManager.runWithGlobalCacheLock(any(GitMercurialNotationDependency), any(Callable)))
                .thenThrow(new IOException())
        // when
        manager.resolve(configuration, notationDependency)
    }

    @Test
    void 'mismatched repository should be cleared'() {
        // given
        when(notationDependency.getUrls()).thenReturn(['anotherUrl'])
        IOUtils.write(resource, 'some file', 'file content')
        // when
        manager.resolve(configuration, notationDependency)
        // then
        assert IOUtils.dirIsEmpty(resource)
    }

    @Test
    void 'installing a resolved dependency should succeed'() {
        // given
        File globalCache = IOUtils.mkdir(resource, 'globalCache')
        File projectGopath = IOUtils.mkdir(resource, 'projectGopath')
        when(cacheManager.getGlobalPackageCachePath(anyString())).thenReturn(globalCache.toPath())
        when(resolvedDependency.getVersion()).thenReturn(commitId)
        // when
        manager.install(resolvedDependency, projectGopath)
        // then
        verify(accessor).checkout(globalCache, commitId)
    }

    @Test(expected = DependencyInstallationException)
    void 'exception in install process should be wrapped'() {
        // given
        when(cacheManager.getGlobalPackageCachePath(anyString())).thenThrow(IllegalStateException)
        // then
        manager.install(resolvedDependency, resource)
    }

    class TestGitMercurialDependencyManager extends GitMercurialDependencyManager {
        GitMercurialAccessor accessor

        TestGitMercurialDependencyManager(GlobalCacheManager cacheManager,
                                          DependencyVisitor dependencyVisitor,
                                          GitMercurialAccessor accessor) {
            super(cacheManager, dependencyVisitor)
            this.accessor = accessor
        }

        @Override
        protected String getDefaultBranchName() {
            return DEFAULT_BRANCH
        }

        @Override
        protected GitMercurialAccessor getAccessor() {
            return accessor
        }
    }

    @Test
    void 'finding commit by sem version expression should succeed'() {
        // when
        def tags = ['3.0.0', '2.1.2', '2.1.1', '2.1.0', '2.0', '1.2.0', '1.0.0', '0.0.3-prerelease', 'v0.0.2', '0.0.1'].collect {
            GitMercurialCommit.of('commit', it, 0L)
        }
        when(accessor.getAllTags(resource)).thenReturn(tags)
        //3.0.0
        assert findMatchedTag('3.x') == '3.0.0'
        // NOT 1.0.0
        assert findMatchedTag('!(1.0.0)') == '3.0.0'

        // 3.0.0
        assert findMatchedTag('2.0-3.0') == '3.0.0'

        // 2.1.2
        assert findMatchedTag('~2.1.0') == '2.1.2'
        // 1.2.0
        assert findMatchedTag('>=1.0.0 & <2.0.0') == '1.2.0'
    }

    def findMatchedTag(String expression) {
        when(notationDependency.getTag()).thenReturn(expression)
        return manager.determineVersion(resource, notationDependency).tag
    }

}