package com.github.blindpirate.gogradle.util;


import com.google.common.collect.Lists;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Singleton
public class ProcessUtils {
    private static final Logger LOGGER = Logging.getLogger(ProcessUtils.class);

    public static class ProcessResult {
        private int code;
        private String stdout;
        private String stderr;

        public int getCode() {
            return code;
        }

        public String getStdout() {
            return stdout;
        }

        public String getStderr() {
            return stderr;
        }

        public ProcessResult(Process process) throws InterruptedException {
            code = process.waitFor();
            stdout = IOUtils.toString(process.getInputStream());
            stderr = IOUtils.toString(process.getErrorStream());
        }
    }

    public String getStdout(Process process) {
        String ret = getResult(process).getStdout();
        LOGGER.debug("Process stdout: {}", ret);
        return ret;
    }

    public String runAndGetStdout(String... args) {
        return getStdout(run(args));
    }

    public Process run(String... args) {
        return run(Arrays.asList(args), null, null);
    }

    public ProcessResult getResult(Process process) {
        try {
            return new ProcessResult(process);
        } catch (InterruptedException e) {
            throw ExceptionHandler.uncheckException(e);
        }
    }

    public Process run(List<String> args, Map<String, String> envs, File workingDirectory) {
        LOGGER.debug("Forking process: args {}, envs {}, workingDir {}", args, envs, workingDirectory);
        try {
            ProcessBuilder pb = new ProcessBuilder().command(args);
            if (envs != null) {
                pb.environment().putAll(envs);
            }
            if (workingDirectory != null) {
                pb.directory(workingDirectory);
            }
            return pb.start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // this should be moved to test source set since it's only used in test
    public ProcessResult runProcessWithCurrentClasspath(Class mainClass,
                                                 List<String> args,
                                                 Map<String, String> envs) {
        String currentClasspath = System.getProperty("java.class.path");

        List<String> cmds = Lists.newArrayList("java", "-cp", currentClasspath, mainClass.getName());
        cmds.addAll(args);
        return getResult(run(cmds, envs, null));
    }
}
