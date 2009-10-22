#!/usr/bin/env groovy 

def ant = new AntBuilder()

// lets just call one task
ant.echo("Build durbinlib, a collection of handy Java classes.")


def srcDir          = "src"
def confDir         = "src/conf"
def testDir         = "test"

def libDir          = "lib"

def targetDir       = "target"
def classesDir      = "target/classes"
def classesConfDir  = "target/classes/conf"
def testClassesDir  = "target/test-classes"
def testReportsDir  = "target/test-reports"

ant.sequential {
    echo "Creating the output directories"
    delete(dir: targetDir)
    mkdir(dir: new File(classesDir))
    mkdir(dir: new File(classesConfDir))
    mkdir(dir: new File(testClassesDir))
    mkdir(dir: new File(testReportsDir))

    echo "Defining the classpath"
    path(id: "path") {
        fileset(dir: libDir) {
            include(name: "**/*.jar")
        }
        pathelement(location: classesDir)
        pathelement(location: testClassesDir)
    }

    echo "Copying the configuration resources"
    copy(todir: classesConfDir) {
        fileset(dir: confDir) {
            include(name: "**/*.conf")
        }
    }

    echo "Defining groovyc task"
    taskdef(name: "groovyc", classname: "org.codehaus.groovy.ant.Groovyc", classpathref: "path")

    echo "Compiling main classes"
    groovyc(srcdir: srcDir, destdir: classesDir, classpathref: "path")

    echo "Compiling test classes"
    groovyc(srcdir: testDir, destdir: testClassesDir, classpathref: "path")

    echo "Running tests"
    junit(printsummary:"yes", haltonfailure: "yes") {
        classpath(refid: "path")
        formatter(type: "plain")
        batchtest(fork: "yes", todir: testReportsDir) {
            fileset(dir: testClassesDir) {
                include(name: "**/*Test.*")
            }
        }
    }
   
}