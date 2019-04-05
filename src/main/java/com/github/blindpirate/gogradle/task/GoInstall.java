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

package com.github.blindpirate.gogradle.task;

import com.github.blindpirate.gogradle.GolangPluginSetting;
import com.github.blindpirate.gogradle.core.cache.VendorSnapshoter;
import com.github.blindpirate.gogradle.core.dependency.GolangDependency;
import com.github.blindpirate.gogradle.core.dependency.GolangDependencySet;
import com.github.blindpirate.gogradle.core.dependency.ResolvedDependency;
import com.github.blindpirate.gogradle.util.IOUtils;
import com.github.blindpirate.gogradle.util.StringUtils;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.util.Optional;

import static com.github.blindpirate.gogradle.task.GolangTaskContainer.RESOLVE_BUILD_DEPENDENCIES_TASK_NAME;
import static com.github.blindpirate.gogradle.task.GolangTaskContainer.RESOLVE_TEST_DEPENDENCIES_TASK_NAME;

public class GoInstall extends AbstractGolangTask {
    private static final Logger LOGGER = Logging.getLogger(GoInstall.class);

    @Inject
    private GolangPluginSetting setting;
    @Inject
    private VendorSnapshoter vendorSnapshoter;
    private File vendorDir;


    public GoInstall() {
        setGogradleGlobalContext();
        setGroup(null);
        mustRunAfter(RESOLVE_BUILD_DEPENDENCIES_TASK_NAME, RESOLVE_TEST_DEPENDENCIES_TASK_NAME);
    }

    @TaskAction
    public void installDependenciesToVendor() {
        setGogradleGlobalContext();
        defineVendorDir();

        IOUtils.forceMkdir(vendorDir);
        vendorSnapshoter.loadPersistenceCache();

        GolangDependencySet buildSet = getTask(ResolveBuildDependencies.class).getFlatDependencies();
        GolangDependencySet testSet = getTask(ResolveTestDependencies.class).getFlatDependencies();

        GolangDependencySet result = GolangDependencySet.merge(buildSet, testSet);

        IOUtils.markAndDeleteUnmarked(vendorDir, dir -> vendorIsUpToDate(dir, vendorDir, result));
        result.forEach(this::installIfNecessary);

        removeFilesUnderVendor();
        vendorSnapshoter.savePersistenceCache();
    }

    private void removeFilesUnderVendor() {
        IOUtils.safeListFiles(vendorDir).stream().filter(File::isFile).forEach(IOUtils::deleteQuitely);
    }

    private void defineVendorDir() {
        if (setting.getVendorTargetDir() != null) {
            vendorDir = new File(getProjectDir(), setting.getVendorTargetDir() + "/src");
        } else {
            vendorDir = new File(getProjectDir(), "/vendor/src");
        }
    }

    private void installIfNecessary(GolangDependency dependency) {
        File targetDir = new File(vendorDir + "/" + dependency.getName());
        IOUtils.forceMkdir(targetDir);
        if (IOUtils.dirIsEmpty(targetDir)) {
            ResolvedDependency resolvedDependency = (ResolvedDependency) dependency;
            resolvedDependency.installTo(targetDir);
            vendorSnapshoter.updateCache(resolvedDependency, targetDir);
        } else {
            IOUtils.deleteQuitely(new File(targetDir, "vendor"));
            LOGGER.info("{} is up-to-date, skip installing {}", targetDir, dependency);
        }
    }


    private boolean vendorIsUpToDate(File currentDir,
                                     File vendorDir,
                                     GolangDependencySet dependencySet) {
        String packagePath = StringUtils.toUnixString(vendorDir.toPath().relativize(currentDir.toPath()));
        Optional<GolangDependency> existed = dependencySet.findByName(packagePath);
        if (!existed.isPresent()) {
            return false;
        }

        ResolvedDependency resolvedDependency = (ResolvedDependency) (existed.get());

        return vendorSnapshoter.isUpToDate(resolvedDependency, currentDir);
    }

}
