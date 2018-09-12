import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipFile
import org.apache.log4j.Logger
import org.codehaus.groovy.runtime.StackTraceUtils

class LoadPackages {

    private final String SCRIPT_NAME = "LoadPackages.groovy"

    private final String LOAD_PACKAGE_SCRIPT_NAME = "LoadPackage.groovy"


    private final String SCHEDULER_HOME
    private final String EXAMPLES_ZIP_PATH
    private final String EXAMPLES_DIR_PATH
    private final String TOOLS_DIR
    private final GroovyObject package_loader

    private logger = Logger.getLogger("org.ow2.proactive.scheduler")


    LoadPackages(binding) {
        println "LoadPackages.groovy Constructor Line 0"
        // Bindings
        this.SCHEDULER_HOME = binding.variables.get("pa.scheduler.home")
        println "LoadPackages.groovy Constructor Line 1"

        // User variables
        this.EXAMPLES_ZIP_PATH = new File(this.SCHEDULER_HOME, "samples/proactive-examples.zip").absolutePath
        println "LoadPackages.groovy Constructor Line 2"
        this.TOOLS_DIR = new File(this.SCHEDULER_HOME, "tools")
        println "LoadPackages.groovy Constructor Line 3"

        // Deduced variables
        this.EXAMPLES_DIR_PATH = this.EXAMPLES_ZIP_PATH.substring(0, this.EXAMPLES_ZIP_PATH.lastIndexOf("."))
        println "LoadPackages.groovy Constructor Line 4"

        // Create a new instance of the package loader
        File load_package_script = new File(this.TOOLS_DIR, this.LOAD_PACKAGE_SCRIPT_NAME)
        println "LoadPackages.groovy Constructor Line 5"
        if (load_package_script.exists()) {
            println "LoadPackages.groovy Constructor Line 6"
            GroovyClassLoader gcl = new GroovyClassLoader()
            println "LoadPackages.groovy Constructor Line 7"
            Class loadPackageClass = gcl.parseClass(load_package_script)
            println "LoadPackages.groovy Constructor Line 8"
            this.package_loader = (GroovyObject) loadPackageClass.newInstance(binding)
            println "LoadPackages.groovy Constructor Line 9"
        } else {
            return
        }
    }


    void unzipFile(src, dest) {
        def zipFile = new ZipFile(src)
        zipFile.entries().each { it ->
            def path = Paths.get(dest + "/" + it.name)
            if (it.directory) {
                Files.createDirectories(path)
            } else {
                def parentDir = path.getParent()
                if (!Files.exists(parentDir)) {
                    Files.createDirectories(parentDir)
                }
                Files.copy(zipFile.getInputStream(it), path)
            }
        }
    }

    public void writeToOutput(output) {
        logger.info("[" + this.SCRIPT_NAME + "] " + output)
    }

    public void writeError(output, exception) {
        logger.error("[" + this.SCRIPT_NAME + "] " + output, exception)
    }


    def run() {
        println "Executing " + this.SCRIPT_NAME + " ..."
        writeToOutput(" Automatic deployment of proactive packages ...")

        writeToOutput(" Variables : ")
        writeToOutput(" EXAMPLES_ZIP_PATH " + this.EXAMPLES_ZIP_PATH)
        writeToOutput(" EXAMPLES_DIR_PATH " + this.EXAMPLES_DIR_PATH)

        writeToOutput(" Actions : ")

        // If the unzipped dir already exists, stop the script execution
        def examples_dir = new File(this.EXAMPLES_DIR_PATH)
        if (examples_dir.exists()) {
            writeToOutput(this.EXAMPLES_DIR_PATH + " already exists, delete it to redeploy packages.")
            writeToOutput("Terminated.")
            return
        }

        // Unzip the examples
        def examples_zip = new File(this.EXAMPLES_ZIP_PATH)
        if (!examples_zip.exists()) {
            writeToOutput(this.EXAMPLES_ZIP_PATH + " not found!")
            return

        }
        unzipFile(examples_zip, this.EXAMPLES_DIR_PATH)
        writeToOutput(this.EXAMPLES_ZIP_PATH + " extracted!")

        // Connect to the scheduler
        package_loader.loginAdminUserCredToSchedulerAndGetSessionId()

        // Create buckets following the ordered bucket list
        new File(examples_dir, "ordered_bucket_list").text.split(",").each { bucket ->
            package_loader.createBucketIfNotExist(bucket)
        }

        // Load all packages without loading each package dependencies
        examples_dir.eachDir { package_dir ->
            package_loader.run(package_dir, false)
        }

        writeToOutput(" ... proactive packages deployed!")
        writeToOutput(" Terminated.")
        println "... " + this.SCRIPT_NAME + " done"
    }
}
instance = null;
try {
    instance = new LoadPackages(this.binding)
    instance.run()
} catch (Exception e) {
    StackTraceUtils.deepSanitize(e)
    //instance.writeError("Failed to load examples into the catalog", e)
    throw new Exception("Failed to load examples into the catalog " + e.getMessage())
}
