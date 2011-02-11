package net.thucydides.core.reports.xml;

import java.io.File;
import java.io.IOException;

import net.thucydides.core.model.AcceptanceTestRun;
import net.thucydides.core.reports.AcceptanceTestReporter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.modeshape.common.text.Inflector;

import com.google.common.base.Preconditions;
import com.thoughtworks.xstream.XStream;

public class XMLAcceptanceTestReporter implements AcceptanceTestReporter {

    private File outputDirectory;
    private final Inflector inflector = Inflector.getInstance();
    
    public File generateReportFor(AcceptanceTestRun testRun) throws IOException {

        Preconditions.checkNotNull(outputDirectory);
        
        XStream xstream = new XStream();
        xstream.alias("acceptance-test-run", AcceptanceTestRun.class);
        xstream.registerConverter(new AcceptanceTestRunConverter());
        String xmlContents = xstream.toXML(testRun);
        
        String reportFilename = getNormalizedTestNameFor(testRun);
        File report = new File(getOutputDirectory(), reportFilename);
        FileUtils.writeStringToFile(report, xmlContents);
        
        return report;
    }

    /**
     * Return a filesystem-friendly version of the test case name.
     * The filesytem version should have no spaces and have the XML file suffix.
     */
    public String getNormalizedTestNameFor(AcceptanceTestRun testRun) {
        String testCaseNameWithUnderscores = inflector.underscore(testRun.getTitle());
        String lowerCaseTestCaseName = testCaseNameWithUnderscores.toLowerCase();
        String lowerCaseTestCaseNameWithUnderscores = lowerCaseTestCaseName.replaceAll("\\s", "_");
        return lowerCaseTestCaseNameWithUnderscores + ".xml";
    }
    
    public File getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

}
