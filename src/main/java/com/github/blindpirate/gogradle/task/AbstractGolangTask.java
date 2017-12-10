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

import com.github.blindpirate.gogradle.GogradleGlobal;
import org.gradle.api.DefaultTask;
import org.gradle.api.Task;

import javax.inject.Inject;

public class AbstractGolangTask extends DefaultTask {
    @Inject
    private GolangTaskContainer golangTaskContainer;

    public AbstractGolangTask() {
        setGroup("Gogradle");
    }

    protected <T extends Task> T getTask(Class<T> clazz) {
        return golangTaskContainer.get(clazz);
    }

    protected void setGogradleGlobalContext() {
        // AbstractGolangTask should set project into thread local before it executes
        // otherwise, subtle issues occur in multiproject
        GogradleGlobal.INSTANCE.setCurrentProject(getProject());
    }
}
