package de.fhg.iais.roberta.worker;

import java.lang.ProcessBuilder.Redirect;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.iais.roberta.bean.CompilerSetupBean;
import de.fhg.iais.roberta.components.Project;
import de.fhg.iais.roberta.util.Key;

public class MicrobitCompilerWorker implements IWorker {

    private static final Logger LOG = LoggerFactory.getLogger(MicrobitCompilerWorker.class);

    @Override
    public void execute(Project project) {
        runBuild(project);
    }

    /**
     * run the build and create the complied hex file
     */
    Key runBuild(Project project) {
        CompilerSetupBean compilerWorkflowBean = (CompilerSetupBean) project.getWorkerResult("CompilerSetup");
        final String compilerBinDir = compilerWorkflowBean.getCompilerBinDir();
        final String compilerResourcesDir = compilerWorkflowBean.getCompilerResourcesDir();
        final String tempDir = compilerWorkflowBean.getTempDir();
        String sourceCode = project.getSourceCode().toString();

        final StringBuilder sb = new StringBuilder();

        String scriptName = compilerResourcesDir + "/compile.py";

        String[] executableWithParameters =
            new String[] {
                compilerBinDir + "python",
                scriptName,
                sourceCode
            };
        project.setCompiledHex(getBinaryFromCrossCompiler(executableWithParameters));
        if ( project.getCompiledHex() != null ) {
            return Key.COMPILERWORKFLOW_SUCCESS;
        } else {
            return Key.COMPILERWORKFLOW_ERROR_PROGRAM_COMPILE_FAILED;
        }
    }

    protected final String getBinaryFromCrossCompiler(String[] executableWithParameters) {
        try {
            ProcessBuilder procBuilder = new ProcessBuilder(executableWithParameters);
            procBuilder.redirectErrorStream(true);
            procBuilder.redirectInput(Redirect.INHERIT);
            procBuilder.redirectOutput(Redirect.PIPE);
            Process p = procBuilder.start();
            String compiledHex = IOUtils.toString(p.getInputStream(), "US-ASCII");
            p.waitFor();
            p.destroy();
            return compiledHex;
        } catch ( Exception e ) {
            LOG.error("exception when calling the cross compiler", e);
            return null;
        }
    }
}
