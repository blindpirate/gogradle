/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.github.blindpirate.gogradle

import com.github.blindpirate.gogradle.core.mode.BuildMode
import org.junit.Before
import org.junit.Test

import javax.annotation.Nonnull

class GolangPluginSettingTest {
    GolangPluginSetting setting = new GolangPluginSetting()

    @Before
    void setUp() {
        setting.packagePath = 'github.com/a/b'
    }

    @Test(expected = IllegalStateException)
    void 'verification should fail if package name not set'() {
        setting.packagePath = ''
        setting.verify()
    }

    @Test
    void 'verification should succeed if package name is set'() {
        setting.verify()
    }

    @Test
    void 'setting build mode should succeed'() {
        setting.buildMode = 'DEVELOP'
        assert setting.buildMode == BuildMode.DEVELOP

        setting.buildMode = BuildMode.REPRODUCIBLE
        assert setting.buildMode == BuildMode.REPRODUCIBLE
    }

    @Test
    void 'setting build mode via System.property should succeed'() {
        setting.buildMode = 'DEVELOP'
        assert setting.buildMode == BuildMode.DEVELOP

        System.setProperty('gogradle.mode', 'REPRODUCIBLE')
        assert setting.buildMode == BuildMode.REPRODUCIBLE

        System.setProperty('gogradle.mode', '')
    }

    @Test
    void 'setting go executable should succeed'() {
        assert setting.goExecutable == 'go'

        setting.goExecutable = '/path/to/go'
        assert setting.goExecutable == '/path/to/go'
    }

    @Test
    void 'setting build tags should succeed'() {
        setting.buildTags = ['a', 'b']
        assert setting.buildTags == ['a', 'b']
    }

    @Test(expected = IllegalStateException)
    void 'setting build tags should fail when it contains single quote'() {
        setting.buildTags = ["'"]
    }

    @Test(expected = IllegalStateException)
    void 'setting build tags should fail when it contains double quote'() {
        setting.buildTags = ['"']
    }

    @Test
    void 'setting packagePath should succeed'() {
        assert setting.packagePath == 'github.com/a/b'
    }

    @Test
    void 'setting go version should succeed'() {
        setting.goVersion = '1.7.4'
        assert setting.goVersion == '1.7.4'
    }

    @Test
    void 'setting goExecutable should succeed'() {
        setting.goExecutable = '/bin/go'
        assert setting.goExecutable == '/bin/go'
    }

    @Test
    void 'setting fuckGfw should succeed'() {
        setting.fuckGfw = true
        assert setting.goBinaryDownloadTemplate == 'http://golangtc.com/static/go/${version}/go${version}.${os}-${arch}${extension}'
    }

    @Test
    void 'setting fuckGfw changes the binary download uri'() {
        def before = setting.goBinaryDownloadTemplate
        setting.fuckGfw()
        assert before != setting.goBinaryDownloadTemplate
    }

    @Test
    void 'setting go binary download base uri to a String should succeed (backwards compatiblity)'() {
        setting.goBinaryDownloadBaseUri = 'http://example.com/'
        assert setting.goBinaryDownloadTemplate == 'http://example.com/go${version}.${os}-${arch}${extension}'
    }

    @Test
    void 'setting go binary download base uri to a URI should succeed (backwards compatiblity)'() {
        setting.goBinaryDownloadBaseUri = URI.create('http://example.com/')
        assert setting.goBinaryDownloadTemplate == 'http://example.com/go${version}.${os}-${arch}${extension}'
    }

    @Test
    void 'setting go binary download uri to a URI should succeed'() {
        setting.goBinaryDownloadTemplate = URI.create('http://example.com/wherever?youName=it')
        assert setting.goBinaryDownloadTemplate == 'http://example.com/wherever?youName=it'
    }

    @Test
    void 'setting go binary download uri to a String should succeed'() {
        setting.goBinaryDownloadTemplate = 'http://example.com/wherever?youName=it'
        assert setting.goBinaryDownloadTemplate == 'http://example.com/wherever?youName=it'
    }

    @Test
    void 'setting goroot should succeed'() {
        setting.goRoot = 'goroot'
        assert setting.goRoot == 'goroot'
    }

    @Test
    void 'setting global cache time should succeed'() {
        assertCacheTimeEquals(1, 'SECONDS', 1)
        assertCacheTimeEquals(1, 'SECOND', 1)
        assertCacheTimeEquals(1, 'MINUTE', 60)
        assertCacheTimeEquals(1, 'MINUTES', 60)
        assertCacheTimeEquals(2, 'HOUR', 3600 * 2)
        assertCacheTimeEquals(2, 'HOURS', 3600 * 2)
        assertCacheTimeEquals(3, 'DAY', 3600 * 24 * 3)
        assertCacheTimeEquals(3, 'DAYS', 3600 * 24 * 3)

        assertCacheTimeEquals(1, 'seconds', 1)
        assertCacheTimeEquals(1, 'second', 1)
        assertCacheTimeEquals(1, 'minute', 60)
        assertCacheTimeEquals(1, 'minutes', 60)
        assertCacheTimeEquals(2, 'hour', 3600 * 2)
        assertCacheTimeEquals(2, 'hours', 3600 * 2)
        assertCacheTimeEquals(3, 'day', 3600 * 24 * 3)
        assertCacheTimeEquals(3, 'days', 3600 * 24 * 3)
    }

    @Test(expected = IllegalArgumentException)
    void 'setting an unsupported time unit should fail'() {
        setting.globalCacheFor(1, 'year')
    }

    private void assertCacheTimeEquals(int count, @Nonnull String unit, long expectedResult) {
        setting.globalCacheFor(count, unit)
        assert setting.getGlobalCacheSecond() == expectedResult
    }
}
