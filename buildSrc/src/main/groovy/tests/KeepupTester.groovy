package tests

import org.gradle.api.Project

import java.util.concurrent.TimeoutException

import static org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS
import static org.apache.tools.ant.taskdefs.condition.Os.isFamily

class KeepupTester {
    final Project project
    final File appLogFile
    final File keepupLogFile
    final String launcher
    final File packagedImage
    final File testAppUpdateFile

    Closure cleanup
    String[] envVars = []

    private Process process

    KeepupTester(Project project, String appName, File packagedImage, File testAppUpdateFile) {
        this.project = project
        appLogFile = project.file("build/app.log")
        keepupLogFile = project.file('build/keepup.log')
        this.packagedImage = packagedImage
        this.testAppUpdateFile = testAppUpdateFile
        launcher = "${project.buildDir}/image/bin/${appName}${isFamily(FAMILY_WINDOWS) ? '.bat' : ''}"
    }

    void reset() {
        cleanup?.call()
        cleanup = null
        envVars = []
        process = null
        appLogFile.delete()
        keepupLogFile.delete()
        testAppUpdateFile.delete()
    }

    void setEnvVars(Map envVars) {
        this.envVars = inheritEnv(envVars)
    }

    void makeUpdateAvailable() {
        testAppUpdateFile << packagedImage.bytes
    }

    Process exec() {
        process = launcher.execute(envVars, project.projectDir)
    }

    void verifyProcessExitCode(int code, long waitFor = 2000L) {
        def timeout = System.currentTimeMillis() + waitFor
        while (System.currentTimeMillis() < timeout) {
            if (!process.isAlive()) {
                assert process.exitValue() == code
                return
            }
            sleep 250
        }
        process.destroyForcibly()
        throw new TimeoutException("Process did not die in time\n" +
                "Keepup Log:\n${keepupLog()}")
    }

    void verifyOutputIs(String out, long waitFor = 2000L) {
        def timeout = System.currentTimeMillis() + waitFor
        while (System.currentTimeMillis() < timeout) {
            if (!process.isAlive()) {
                assert process.inputStream.text == out
                return
            }
            sleep 250
        }
        process.destroyForcibly()
        throw new TimeoutException("Process did not die in time\n" +
                "Keepup Log:\n${keepupLog()}")
    }

    void verifyErrorIs(String err, long waitFor = 2000L) {
        def timeout = System.currentTimeMillis() + waitFor
        while (System.currentTimeMillis() < timeout) {
            if (!process.isAlive()) {
                assert process.errorStream.text == err
                return
            }
            sleep 250
        }
        process.destroyForcibly()
        throw new TimeoutException("Process did not die in time\n" +
                "Keepup Log:\n${keepupLog()}")
    }

    void verifyAppLogIs(String out, long waitFor = 2000L) {
        def timeout = System.currentTimeMillis() + waitFor
        while (System.currentTimeMillis() < timeout) {
            if (appLogFile.isFile() && appLogFile.text == out) {
                return // success
            }
            sleep 250
        }
        assert appLogFile.isFile() && appLogFile.text == out
    }

    private static String[] inheritEnv(Map envVars) {
        (System.getenv() + envVars).inject([]) { all, e -> all + "${e.key}=${e.value}" }
    }

    private String keepupLog() {
        if (keepupLogFile.isFile()) return keepupLogFile.text
        else return ''
    }
}
